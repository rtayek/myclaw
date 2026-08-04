package myclaw.web;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

final class PlaywrightWebAdapterTest {

    @Test
    void defaultCdpUrlIsLocalhost9222() {
        try (PlaywrightWebAdapter adapter = new PlaywrightWebAdapter()) {
            assertEquals("http://localhost:9222", adapter.cdpUrl());
            assertFalse(adapter.isConnected());
        }
    }

    @Test
    void acceptsCustomCdpUrl() {
        try (PlaywrightWebAdapter adapter = new PlaywrightWebAdapter("http://localhost:9333")) {
            assertEquals("http://localhost:9333", adapter.cdpUrl());
            assertFalse(adapter.isConnected());
        }
    }

    @Test
    void closeIdempotentWhenNotConnected() {
        PlaywrightWebAdapter adapter = new PlaywrightWebAdapter();
        adapter.close();
        adapter.close();
        assertFalse(adapter.isConnected());
    }
}
