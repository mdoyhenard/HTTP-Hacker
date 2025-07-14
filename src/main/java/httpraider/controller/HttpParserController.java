package httpraider.controller;

import httpraider.model.network.HttpParserModel;
import httpraider.model.network.ProxyModel;
import httpraider.view.panels.parser.HeaderLinesParserPanel;
import httpraider.view.panels.parser.LoadBalancingParserPanel;
import httpraider.view.panels.HttpParserPanel;
import httpraider.view.panels.parser.MessageLengthParserPanel;
import httpraider.view.panels.parser.RequestLineParserPanel;

import javax.swing.*;
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
    }

    private void stopTableEditing(JTable table) {
        if (table != null && table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
    }

    // --- Utility: Find eligible proxies for load balancing ---
    // Eligible: directly connected proxies (distance 1) that have a path to client passing through currentProxy
    private Map<String, String> getEligibleProxies(ProxyModel currentProxy, NetworkController networkController) {
        Map<String, String> proxies = new LinkedHashMap<>();
        List<ProxyModel> directConnections = networkController.getDirectConnections(currentProxy.getId());
        for (ProxyModel p : directConnections) {
            if (networkController.hasPathThroughProxyToClient(p.getId(), currentProxy.getId())) {
                proxies.put(p.getId(), p.getDomainName() != null && !p.getDomainName().isEmpty() ? p.getDomainName() : p.getId());
            }
        }
        return proxies;
    }
}
