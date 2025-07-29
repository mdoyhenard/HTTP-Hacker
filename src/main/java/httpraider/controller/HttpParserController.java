package httpraider.controller;

import httpraider.model.network.HttpParserModel;
import httpraider.model.network.LoadBalancingRule;
import httpraider.model.network.ProxyModel;
import httpraider.model.network.FirewallRule;
import httpraider.view.panels.parser.HeaderLinesParserPanel;
import httpraider.view.panels.parser.LoadBalancingParserPanel;
import httpraider.view.panels.HttpParserPanel;
import httpraider.view.panels.parser.MessageLengthParserPanel;
import httpraider.view.panels.parser.RequestLineParserPanel;
import httpraider.view.panels.parser.FirewallRulesParserPanel;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class HttpParserController {
    private final HttpParserModel settings;
    private final HttpParserPanel parserPanel;
    private final ProxyModel currentProxy;
    private final NetworkController networkController;

    public HttpParserController(ProxyModel proxyModel, HttpParserPanel parserPanel, NetworkController networkController) {
        this.settings = proxyModel.getParserSettings();
        this.parserPanel = parserPanel;
        this.currentProxy = proxyModel;
        this.networkController = networkController;
        installListeners();
        loadFromModel();
    }

    private void installListeners() {
        HeaderLinesParserPanel headerLinesPanel = parserPanel.getHeaderLinesParserPanel();
        MessageLengthParserPanel messageLengthPanel = parserPanel.getMessageLengthParserPanel();

        parserPanel.getSaveButton().addActionListener(e -> saveToModel());

        parserPanel.getTestButton().addActionListener(e -> {
            saveToModel(); // always parse using latest UI config
            byte[] input = parserPanel.getInputEditorPanel().getBytes();
            List<List<byte[]>> groups = httpraider.parser.ParserChainRunner.parseOnlyCurrentProxyForPanel(
                    currentProxy, input
            );
            parserPanel.getResultEditorPanel().addAll(groups);
        });
        
        // Add window closing listener
        parserPanel.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        parserPanel.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (hasUnsavedChanges()) {
                    int result = JOptionPane.showConfirmDialog(
                        parserPanel,
                        "You have unsaved changes. Do you want to save them?",
                        "Unsaved Changes",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.WARNING_MESSAGE
                    );
                    
                    if (result == JOptionPane.YES_OPTION) {
                        saveToModel();
                        parserPanel.dispose();
                    } else if (result == JOptionPane.NO_OPTION) {
                        parserPanel.dispose();
                    }
                    // If CANCEL, do nothing (don't close)
                } else {
                    parserPanel.dispose();
                }
            }
        });
    }

    private void loadFromModel() {
        HeaderLinesParserPanel headerLinesPanel = parserPanel.getHeaderLinesParserPanel();
        RequestLineParserPanel requestLinePanel = parserPanel.getRequestLineParserPanel();
        MessageLengthParserPanel messageLengthPanel = parserPanel.getMessageLengthParserPanel();
        LoadBalancingParserPanel loadBalancingPanel = parserPanel.getLoadBalancingParserPanel();

        // Header Lines tab (NO chunked endings here)
        headerLinesPanel.getHeaderLineEndingsPanel().setLineEndings(settings.getHeaderLineEndings());
        headerLinesPanel.getHeaderFoldingPanel().setHeaderFoldingEnabled(settings.isAllowHeaderFolding());
        headerLinesPanel.getHeaderDeletePanel().setRules(settings.getDeleteHeaderRules());
        headerLinesPanel.getHeaderAddPanel().setHeaders(settings.getAddHeaderRules());
        headerLinesPanel.setUseJsParser(settings.isUseHeaderLinesJs());
        headerLinesPanel.setJsScript(settings.getHeaderLinesScript());

        // Request Line tab
        requestLinePanel.setLineDelimiters(settings.getRequestLineDelimiters());
        requestLinePanel.setRewriteMethodEnabled(settings.isRewriteMethodEnabled());
        requestLinePanel.setFromMethod(settings.getFromMethod());
        requestLinePanel.setToMethod(settings.getToMethod());
        requestLinePanel.setDecodeUrlBeforeForwarding(settings.isDecodeUrlBeforeForwarding());
        requestLinePanel.setUrlDecodeFrom(settings.getUrlDecodeFrom());
        requestLinePanel.setUrlDecodeTo(settings.getUrlDecodeTo());
        requestLinePanel.setForcedHttpVersion(settings.getForcedHttpVersion());
        requestLinePanel.setCustomHttpVersion(settings.getCustomHttpVersion());
        requestLinePanel.setUseJsParser(settings.isUseRequestLineJs());
        requestLinePanel.setJsScript(settings.getRequestLineScript());

        // Message Length tab
        messageLengthPanel.setRules(settings.getBodyLenHeaderRules());
        messageLengthPanel.setChunkedEndings(settings.getChunkedLineEndings());
        messageLengthPanel.setUseJsParser(settings.isUseMessageLengthJs());
        messageLengthPanel.setJsScript(settings.getMessageLengthScript());
        messageLengthPanel.setOutputEncoding(settings.getOutputBodyEncoding());

        // Load Balancing tab
        Map<String, String> eligibleProxies = getEligibleProxies(currentProxy, networkController);
        loadBalancingPanel.setEligibleProxies(eligibleProxies);
        loadBalancingPanel.setRules(settings.getLoadBalancingRules());
        
        // Firewall Rules tab
        FirewallRulesParserPanel firewallRulesPanel = parserPanel.getFirewallRulesParserPanel();
        firewallRulesPanel.setRules(settings.getFirewallRules());
    }

    private void saveToModel() {
        HeaderLinesParserPanel headerLinesPanel = parserPanel.getHeaderLinesParserPanel();
        RequestLineParserPanel requestLinePanel = parserPanel.getRequestLineParserPanel();
        MessageLengthParserPanel messageLengthPanel = parserPanel.getMessageLengthParserPanel();
        LoadBalancingParserPanel loadBalancingPanel = parserPanel.getLoadBalancingParserPanel();

        // --- Commit all table edits before reading values ---
        stopTableEditing(headerLinesPanel.getHeaderLineEndingsPanel().getTable());
        stopTableEditing(headerLinesPanel.getHeaderDeletePanel().getTable());
        stopTableEditing(headerLinesPanel.getHeaderAddPanel().getTable());
        stopTableEditing(messageLengthPanel.getMessageLengthHeadersPanel().getTable());
        stopTableEditing(messageLengthPanel.getChunkedEndingsPanel().getTable());
        stopTableEditing(requestLinePanel.getDelimiterTable());
        // No call for load balancing panel (not a table-based UI)

        // Header Lines tab (NO chunked endings here)
        settings.setHeaderLineEndings(headerLinesPanel.getHeaderLineEndingsPanel().getLineEndings());
        settings.setAllowHeaderFolding(headerLinesPanel.getHeaderFoldingPanel().isHeaderFoldingEnabled());
        settings.setDeleteHeaderRules(headerLinesPanel.getHeaderDeletePanel().getRules());
        settings.setAddHeaderRules(headerLinesPanel.getHeaderAddPanel().getHeaders());
        settings.setUseHeaderLinesJs(headerLinesPanel.isUseJsParser());
        settings.setHeaderLinesScript(headerLinesPanel.getJsScript());

        // Request Line tab
        settings.setRequestLineDelimiters(requestLinePanel.getLineDelimiters());
        settings.setRewriteMethodEnabled(requestLinePanel.isRewriteMethodEnabled());
        settings.setFromMethod(requestLinePanel.getFromMethod());
        settings.setToMethod(requestLinePanel.getToMethod());
        settings.setDecodeUrlBeforeForwarding(requestLinePanel.isDecodeUrlBeforeForwarding());
        settings.setUrlDecodeFrom(requestLinePanel.getUrlDecodeFrom());
        settings.setUrlDecodeTo(requestLinePanel.getUrlDecodeTo());
        settings.setForcedHttpVersion(requestLinePanel.getForcedHttpVersion());
        settings.setCustomHttpVersion(requestLinePanel.getCustomHttpVersion());
        settings.setUseRequestLineJs(requestLinePanel.isUseJsParser());
        settings.setRequestLineScript(requestLinePanel.getJsScript());

        // Message Length tab
        settings.setBodyLenHeaderRules(messageLengthPanel.getRules());
        settings.setChunkedLineEndings(messageLengthPanel.getChunkedEndings());
        settings.setUseMessageLengthJs(messageLengthPanel.isUseJsParser());
        settings.setMessageLengthScript(messageLengthPanel.getJsScript());
        settings.setOutputBodyEncoding(messageLengthPanel.getOutputEncoding());

        // Load Balancing tab
        settings.setLoadBalancingRules(loadBalancingPanel.getRules());
        
        // Firewall Rules tab
        FirewallRulesParserPanel firewallRulesPanel = parserPanel.getFirewallRulesParserPanel();
        settings.setFirewallRules(firewallRulesPanel.getRules());
    }

    private void stopTableEditing(JTable table) {
        if (table != null && table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
    }

    // --- Utility: Find eligible proxies for load balancing ---
    // Eligible: directly connected proxies that are downstream (further from client than current proxy)
    private Map<String, String> getEligibleProxies(ProxyModel currentProxy, NetworkController networkController) {
        Map<String, String> proxies = new LinkedHashMap<>();
        List<ProxyModel> directConnections = networkController.getDirectConnections(currentProxy.getId());
        
        // Get distance to client for current proxy
        List<ProxyModel> currentPath = networkController.getConnectionPathToClient(currentProxy.getId());
        int currentDistance = (currentPath == null) ? Integer.MAX_VALUE : currentPath.size();
        
        for (ProxyModel p : directConnections) {
            // Skip the client
            if (p.isClient()) continue;
            
            // Get distance to client for this proxy
            List<ProxyModel> pPath = networkController.getConnectionPathToClient(p.getId());
            int pDistance = (pPath == null) ? Integer.MAX_VALUE : pPath.size();
            
            // Only include proxies that are further from client (downstream)
            if (pDistance > currentDistance) {
                proxies.put(p.getId(), p.getDomainName() != null && !p.getDomainName().isEmpty() ? p.getDomainName() : p.getId());
            }
        }
        return proxies;
    }
    
    private boolean hasUnsavedChanges() {
        HeaderLinesParserPanel headerLinesPanel = parserPanel.getHeaderLinesParserPanel();
        RequestLineParserPanel requestLinePanel = parserPanel.getRequestLineParserPanel();
        MessageLengthParserPanel messageLengthPanel = parserPanel.getMessageLengthParserPanel();
        LoadBalancingParserPanel loadBalancingPanel = parserPanel.getLoadBalancingParserPanel();
        
        // Stop any active table editing
        stopTableEditing(headerLinesPanel.getHeaderLineEndingsPanel().getTable());
        stopTableEditing(headerLinesPanel.getHeaderDeletePanel().getTable());
        stopTableEditing(headerLinesPanel.getHeaderAddPanel().getTable());
        stopTableEditing(messageLengthPanel.getMessageLengthHeadersPanel().getTable());
        stopTableEditing(messageLengthPanel.getChunkedEndingsPanel().getTable());
        stopTableEditing(requestLinePanel.getDelimiterTable());
        
        // Compare Header Lines tab
        if (!listsEqual(settings.getHeaderLineEndings(), headerLinesPanel.getHeaderLineEndingsPanel().getLineEndings())) return true;
        if (settings.isAllowHeaderFolding() != headerLinesPanel.getHeaderFoldingPanel().isHeaderFoldingEnabled()) return true;
        if (!listsEqual(settings.getDeleteHeaderRules(), headerLinesPanel.getHeaderDeletePanel().getRules())) return true;
        if (!listsEqual(settings.getAddHeaderRules(), headerLinesPanel.getHeaderAddPanel().getHeaders())) return true;
        if (settings.isUseHeaderLinesJs() != headerLinesPanel.isUseJsParser()) return true;
        if (!stringsEqual(settings.getHeaderLinesScript(), headerLinesPanel.getJsScript())) return true;
        
        // Compare Request Line tab
        if (!listsEqual(settings.getRequestLineDelimiters(), requestLinePanel.getLineDelimiters())) return true;
        if (settings.isRewriteMethodEnabled() != requestLinePanel.isRewriteMethodEnabled()) return true;
        if (!stringsEqual(settings.getFromMethod(), requestLinePanel.getFromMethod())) return true;
        if (!stringsEqual(settings.getToMethod(), requestLinePanel.getToMethod())) return true;
        if (settings.isDecodeUrlBeforeForwarding() != requestLinePanel.isDecodeUrlBeforeForwarding()) return true;
        if (!stringsEqual(settings.getUrlDecodeFrom(), requestLinePanel.getUrlDecodeFrom())) return true;
        if (!stringsEqual(settings.getUrlDecodeTo(), requestLinePanel.getUrlDecodeTo())) return true;
        if (settings.getForcedHttpVersion() != requestLinePanel.getForcedHttpVersion()) return true;
        if (!stringsEqual(settings.getCustomHttpVersion(), requestLinePanel.getCustomHttpVersion())) return true;
        if (settings.isUseRequestLineJs() != requestLinePanel.isUseJsParser()) return true;
        if (!stringsEqual(settings.getRequestLineScript(), requestLinePanel.getJsScript())) return true;
        
        // Compare Message Length tab
        if (!bodyLenRulesEqual(settings.getBodyLenHeaderRules(), messageLengthPanel.getRules())) return true;
        if (!listsEqual(settings.getChunkedLineEndings(), messageLengthPanel.getChunkedEndings())) return true;
        if (settings.isUseMessageLengthJs() != messageLengthPanel.isUseJsParser()) return true;
        if (!stringsEqual(settings.getMessageLengthScript(), messageLengthPanel.getJsScript())) return true;
        if (settings.getOutputBodyEncoding() != messageLengthPanel.getOutputEncoding()) return true;
        
        // Compare Load Balancing tab
        if (!loadBalancingRulesEqual(settings.getLoadBalancingRules(), loadBalancingPanel.getRules())) return true;
        
        return false;
    }
    
    private boolean stringsEqual(String s1, String s2) {
        if (s1 == null && s2 == null) return true;
        if (s1 == null || s2 == null) return false;
        return s1.equals(s2);
    }
    
    private boolean listsEqual(List<String> list1, List<String> list2) {
        if (list1 == null && list2 == null) return true;
        if (list1 == null || list2 == null) return false;
        if (list1.size() != list2.size()) return false;
        for (int i = 0; i < list1.size(); i++) {
            if (!stringsEqual(list1.get(i), list2.get(i))) return false;
        }
        return true;
    }
    
    private boolean bodyLenRulesEqual(List<HttpParserModel.BodyLenHeaderRule> list1, List<HttpParserModel.BodyLenHeaderRule> list2) {
        if (list1 == null && list2 == null) return true;
        if (list1 == null || list2 == null) return false;
        if (list1.size() != list2.size()) return false;
        for (int i = 0; i < list1.size(); i++) {
            HttpParserModel.BodyLenHeaderRule r1 = list1.get(i);
            HttpParserModel.BodyLenHeaderRule r2 = list2.get(i);
            if (!stringsEqual(r1.getPattern(), r2.getPattern())) return false;
            if (r1.isChunked() != r2.isChunked()) return false;
            if (r1.getDuplicateHandling() != r2.getDuplicateHandling()) return false;
        }
        return true;
    }
    
    private boolean loadBalancingRulesEqual(List<LoadBalancingRule> list1, List<LoadBalancingRule> list2) {
        // Simple comparison - could be enhanced if needed
        if (list1 == null && list2 == null) return true;
        if (list1 == null || list2 == null) return false;
        return list1.size() == list2.size();
    }
}
