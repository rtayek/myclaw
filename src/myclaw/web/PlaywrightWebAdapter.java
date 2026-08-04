package myclaw.web;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

import java.util.Objects;
import java.util.Optional;

public class PlaywrightWebAdapter implements AutoCloseable {
    public static final String DEFAULT_CDP_URL = "http://localhost:9222";

    private final String cdpUrl;
    private Playwright playwright;
    private Browser browser;

    public PlaywrightWebAdapter() {
        this(DEFAULT_CDP_URL);
    }

    public PlaywrightWebAdapter(String cdpUrl) {
        this.cdpUrl = Objects.requireNonNull(cdpUrl, "cdpUrl");
    }

    public String cdpUrl() {
        return cdpUrl;
    }

    public synchronized void connect() {
        if (playwright == null) {
            playwright = Playwright.create();
        }
        if (browser == null || !browser.isConnected()) {
            browser = playwright.chromium().connectOverCDP(cdpUrl);
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
    }
}
