package proxyFinder.engine;

import java.util.Collections;
import java.util.Set;

public class HeaderWhitelistConfig {
    private static final Set<String> WHITELIST = Set.of(
            "server",
            "via",
            "x-powered-by",
            "proxy-agent",
            "location",
            "x-cache",
            "x-squid-error",
            "x-cdn",
            "forwarded",
            "cf-cache-status",
            "x-bluecoat-via"
    );

    public static Set<String> getWhitelist() {
        return Collections.unmodifiableSet(WHITELIST);
    }
}
