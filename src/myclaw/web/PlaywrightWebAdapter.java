package myclaw.web;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class PlaywrightWebAdapter implements AutoCloseable {
    public record ChatWebSummary(String title, String url) {}

    public static final String DEFAULT_CDP_URL = "http://127.0.0.1:9222";

    private final String cdpUrl;
    private Playwright playwright;
    private Browser browser;
    private boolean connectedViaCdp = false;

    public PlaywrightWebAdapter() {
        this(DEFAULT_CDP_URL);
    }

    public PlaywrightWebAdapter(String cdpUrl) {
        this.cdpUrl = Objects.requireNonNull(cdpUrl, "cdpUrl");
    }

    public String cdpUrl() {
        return cdpUrl;
    }

    public boolean isConnectedViaCdp() {
        return connectedViaCdp;
    }

    public synchronized void connect() {
        if (playwright == null) {
            playwright = Playwright.create();
        }
        if (browser == null || !browser.isConnected()) {
            try {
                browser = playwright.chromium().connectOverCDP(cdpUrl);
                connectedViaCdp = true;
            } catch (Exception cdpException) {
                connectedViaCdp = false;
                browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
            }
        }
    }

    public synchronized Page findOrOpenPage(String chatUrl) {
        connect();
        BrowserContext context = browser.contexts().isEmpty()
                ? browser.newContext()
                : browser.contexts().get(0);

        Optional<Page> existing = context.pages().stream()
                .filter(page -> page.url().contains(chatUrl) || chatUrl.contains(page.url()))
                .findFirst();

        if (existing.isPresent()) {
            Page page = existing.get();
            page.bringToFront();
            return page;
        }

        Page page = context.newPage();
        page.navigate(chatUrl);
        return page;
    }

    public String submitPrompt(String chatUrl, String prompt) {
        Page page = findOrOpenPage(chatUrl);
        return submitToPage(page, prompt);
    }

    public String submitToTitle(String title, String prompt) {
        return submitToTitle(title, prompt, new ChatGptWebSessionResolver());
    }

    public String submitToTitle(String title, String prompt, ChatGptWebSessionResolver resolver) {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(prompt, "prompt");

        String url = null;
        if (resolver != null) {
            try {
                Map<String, String> sessions = resolver.resolveSessions(cdpUrl);
                url = findMatchingUrl(sessions, title);
            } catch (Exception ignored) {
            }
        }

        if (url == null && isConnected()) {
            List<ChatWebSummary> summaries = listChatGPTChats();
            Map<String, String> map = new java.util.LinkedHashMap<>();
            for (ChatWebSummary s : summaries) {
                map.put(s.title(), s.url());
            }
            url = findMatchingUrl(map, title);
        }

        if (url == null) {
            throw new IllegalArgumentException("No ChatGPT web chat found with title: " + title);
        }

        return submitPrompt(url, prompt);
    }

    private static String findMatchingUrl(Map<String, String> sessions, String title) {
        if (sessions == null || sessions.isEmpty()) {
            return null;
        }
        if (sessions.containsKey(title)) {
            return sessions.get(title);
        }
        for (Map.Entry<String, String> entry : sessions.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(title)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public String submitToPage(Page page, String prompt) {
        Objects.requireNonNull(page, "page");
        Objects.requireNonNull(prompt, "prompt");

        String selector = page.locator("#prompt-textarea").count() > 0
                ? "#prompt-textarea"
                : (page.locator("textarea").count() > 0 ? "textarea" : "div[contenteditable='true']");

        page.focus(selector);
        page.fill(selector, prompt);

        if (page.locator("button[data-testid='send-button']").count() > 0) {
            page.click("button[data-testid='send-button']");
        } else if (page.locator("button[aria-label*='Send']").count() > 0) {
            page.click("button[aria-label*='Send']");
        } else {
            page.keyboard().press("Enter");
        }

        return page.content();
    }

    public List<ChatWebSummary> listChatGPTChats() {
        Page page = findOrOpenPage("https://chatgpt.com");
        return listChatGPTChats(page);
    }

    public List<ChatWebSummary> listChatGPTChats(Page page) {
        Objects.requireNonNull(page, "page");
        try {
            page.waitForSelector("a[href*='/c/'], a[href*='/g/']", new Page.WaitForSelectorOptions().setTimeout(3000));
        } catch (Exception ignored) {
            // Sidebar links may take time or may not exist if logged out
        }

        List<ChatWebSummary> summaries = new ArrayList<>();
        java.util.Set<String> seenUrls = new java.util.LinkedHashSet<>();
        Locator chatLinks = page.locator("a[href*='/c/'], a[href*='/g/']");
        int count = chatLinks.count();
        for (int i = 0; i < count; i++) {
            Locator link = chatLinks.nth(i);
            String href = link.getAttribute("href");
            if (href == null || href.isBlank()) {
                continue;
            }
            String fullUrl = href.startsWith("/") ? "https://chatgpt.com" + href : href;
            String title = link.innerText().strip();
            if (title.isBlank()) {
                title = link.getAttribute("title");
            }
            if (title == null || title.isBlank()) {
                title = link.getAttribute("aria-label");
            }
            if (title == null || title.isBlank()) {
                title = "Untitled Chat";
            }
            if (seenUrls.add(fullUrl)) {
                summaries.add(new ChatWebSummary(title.strip(), fullUrl));
            }
        }
        return summaries;
    }

    // --- claude.ai web transcript reading (new; independent of the ChatGPT path above) ---

    public static final String CLAUDE_BASE_URL = "https://claude.ai";

    /**
     * The most recent claude.ai conversation, read from the already-logged-in
     * browser session and formatted as role-prefixed Markdown, or empty when
     * nothing is reachable (browser not running, not logged in, no chats, or the
     * page layout no longer matches any known selector). Never throws for the
     * "nothing available" case: callers (the HTTP endpoint) treat empty as 204.
     */
    public Optional<String> latestClaudeChatMarkdown() {
        try {
            if (!connectViaCdpOnly()) {
                // No logged-in session to read, and we deliberately do NOT launch
                // a fresh browser here: "browser not running" must yield 204.
                return Optional.empty();
            }
            Optional<ChatWebSummary> latest = latestClaudeChat();
            if (latest.isEmpty()) {
                return Optional.empty();
            }
            Page page = findOrOpenPage(latest.get().url());
            List<ClaudeTurn> turns = readClaudeTranscript(page);
            if (turns.isEmpty()) {
                return Optional.empty();
            }
            String markdown = ClaudeTranscriptFormatter.toRolePrefixedMarkdown(turns);
            return markdown.isBlank() ? Optional.empty() : Optional.of(markdown);
        } catch (Exception unavailable) {
            // Treat any failure to reach/read as "nothing available" so the
            // endpoint returns 204 rather than surfacing a stack trace.
            return Optional.empty();
        }
    }

    /**
     * Attaches to an already-running Chrome over CDP only; unlike {@link #connect()}
     * it never falls back to launching a browser. Returns false (and leaves the
     * adapter unconnected) when no CDP endpoint is reachable.
     */
    private synchronized boolean connectViaCdpOnly() {
        if (playwright == null) {
            playwright = Playwright.create();
        }
        if (browser != null && browser.isConnected() && connectedViaCdp) {
            return true;
        }
        try {
            browser = playwright.chromium().connectOverCDP(cdpUrl);
            connectedViaCdp = true;
            return true;
        } catch (Exception cdpUnreachable) {
            connectedViaCdp = false;
            return false;
        }
    }

    /** The most recently updated claude.ai conversation from the sidebar, or empty. */
    public Optional<ChatWebSummary> latestClaudeChat() {
        List<ChatWebSummary> chats = listClaudeChats();
        return chats.isEmpty() ? Optional.empty() : Optional.of(chats.get(0));
    }

    /**
     * Lists claude.ai conversations from the sidebar, most-recent first.
     *
     * claude.ai conversation links are {@code /chat/<uuid>}; the sidebar renders
     * them newest-first, so element order is recency order. Titles-and-urls only,
     * mirroring {@link #listChatGPTChats()} — message bodies are read separately
     * by {@link #readClaudeTranscript(Page)}.
     */
    public List<ChatWebSummary> listClaudeChats() {
        Page page = findOrOpenPage(CLAUDE_BASE_URL);
        return listClaudeChats(page);
    }

    public List<ChatWebSummary> listClaudeChats(Page page) {
        Objects.requireNonNull(page, "page");
        try {
            page.waitForSelector("a[href*='/chat/']",
                    new Page.WaitForSelectorOptions().setTimeout(5000));
        } catch (Exception ignored) {
            // Sidebar may be slow, or absent when logged out.
        }

        List<ChatWebSummary> summaries = new ArrayList<>();
        java.util.Set<String> seenUrls = new java.util.LinkedHashSet<>();
        Locator chatLinks = page.locator("a[href*='/chat/']");
        int count = chatLinks.count();
        for (int i = 0; i < count; i++) {
            Locator link = chatLinks.nth(i);
            String href = link.getAttribute("href");
            if (href == null || href.isBlank()) {
                continue;
            }
            String fullUrl = href.startsWith("/") ? CLAUDE_BASE_URL + href : href;
            String title = firstNonBlank(
                    safeInnerText(link), link.getAttribute("title"), link.getAttribute("aria-label"));
            if (title == null || title.isBlank()) {
                title = "Untitled Chat";
            }
            if (seenUrls.add(fullUrl)) {
                summaries.add(new ChatWebSummary(title.strip(), fullUrl));
            }
        }
        return summaries;
    }

    /**
     * Reads the full transcript of an open claude.ai conversation page, turn by
     * turn, preserving user/assistant roles.
     *
     * claude.ai's DOM is undocumented and changes, so this tries several
     * selectors in order and falls back, the same defensive pattern as
     * {@link #submitToPage(Page, String)}. The role of each turn is inferred
     * from stable-looking attributes ({@code data-testid}) first, then from the
     * known {@code font-user-message} / {@code font-claude-message} class markers.
     *
     * NOTE: these selectors are best-effort and MUST be verified against a live
     * logged-in page; adjust the candidate lists below if claude.ai has changed.
     */
    public List<ClaudeTurn> readClaudeTranscript(Page page) {
        Objects.requireNonNull(page, "page");

        // Candidate selectors that each match BOTH roles' turn containers, in
        // DOM (chronological) order. First one that finds anything wins.
        String[] turnSelectorCandidates = {
                "div[data-testid='user-message'], div[data-testid='assistant-message']",
                "[data-testid='user-message'], .font-claude-message",
                "div.font-user-message, div.font-claude-message",
        };

        try {
            page.waitForSelector(String.join(", ", turnSelectorCandidates),
                    new Page.WaitForSelectorOptions().setTimeout(5000));
        } catch (Exception ignored) {
            // No messages found in time (e.g. not logged in) -> empty transcript.
        }

        for (String selector : turnSelectorCandidates) {
            Locator turns = page.locator(selector);
            int count = turns.count();
            if (count == 0) {
                continue;
            }
            List<ClaudeTurn> result = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                Locator turn = turns.nth(i);
                String text = safeInnerText(turn);
                if (text == null || text.isBlank()) {
                    continue;
                }
                result.add(new ClaudeTurn(inferRole(turn), text.strip()));
            }
            if (!result.isEmpty()) {
                return result;
            }
        }
        return List.of();
    }

    private static String inferRole(Locator turn) {
        String testId = turn.getAttribute("data-testid");
        if (testId != null) {
            String lowered = testId.toLowerCase(java.util.Locale.ROOT);
            if (lowered.contains("user")) {
                return "user";
            }
            if (lowered.contains("assistant") || lowered.contains("claude")) {
                return "assistant";
            }
        }
        String classAttr = turn.getAttribute("class");
        if (classAttr != null) {
            String lowered = classAttr.toLowerCase(java.util.Locale.ROOT);
            if (lowered.contains("user")) {
                return "user";
            }
            if (lowered.contains("claude") || lowered.contains("assistant")) {
                return "assistant";
            }
        }
        return "unknown";
    }

    private static String safeInnerText(Locator locator) {
        try {
            return locator.innerText();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    public synchronized boolean isConnected() {
        return browser != null && browser.isConnected();
    }

    @Override
    public synchronized void close() {
        if (browser != null) {
            try {
                browser.close();
            } catch (Exception ignored) {
            }
            browser = null;
        }
        if (playwright != null) {
            try {
                playwright.close();
            } catch (Exception ignored) {
            }
            playwright = null;
        }
        connectedViaCdp = false;
    }
}

