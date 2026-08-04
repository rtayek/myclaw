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

    public Page findOrOpenPage(String chatUrl) {
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

