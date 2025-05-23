package extension;

import httpraider.view.panels.SessionPanel;
import burp.api.montoya.BurpExtension;
import burp.api.montoya.EnhancedCapability;
import burp.api.montoya.MontoyaApi;

import java.awt.*;
import java.util.Set;

public class HTTPRaiderExtension implements BurpExtension {

    public static MontoyaApi API;
    public static final Color BACK_COLOR = Color.WHITE;

    @Override
    public void initialize(MontoyaApi montoyaApi) {
        API = montoyaApi;
        API.userInterface().registerSuiteTab("HTTPStreamHacker", new SessionPanel());
        int a = 1;
    }

    @Override
    public Set<EnhancedCapability> enhancedCapabilities() {
        return Set.of(EnhancedCapability.AI_FEATURES);
    }
}
