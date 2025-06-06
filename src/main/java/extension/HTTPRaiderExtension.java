package extension;

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
        new httpraider.controller.ApplicationController();
    }

    @Override
    public Set<EnhancedCapability> enhancedCapabilities() {
        return Set.of(EnhancedCapability.AI_FEATURES);
    }
}
