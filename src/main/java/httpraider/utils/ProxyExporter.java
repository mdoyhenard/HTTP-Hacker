package httpraider.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import httpraider.model.network.ProxyModel;
import httpraider.model.network.HttpParserModel;
import httpraider.model.network.LoadBalancingRule;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ProxyExporter {
    
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    
    /**
     * Exports a ProxyModel to a JSON file
     * @param proxy The proxy to export
     * @param file The file to write to
     * @throws IOException If there's an error writing the file
     */
    public static void exportProxy(ProxyModel proxy, File file) throws IOException {
        // Create a data transfer object that excludes the ID
        ProxyExportData exportData = new ProxyExportData(proxy);
        
        String json = gson.toJson(exportData);
        Files.writeString(file.toPath(), json, StandardCharsets.UTF_8);
    }
    
    /**
     * Imports a ProxyModel from a JSON file
     * @param file The file to read from
     * @return The imported ProxyModel with a new ID
     * @throws IOException If there's an error reading the file
     * @throws JsonSyntaxException If the JSON is invalid
     */
    public static ProxyModel importProxy(File file) throws IOException, JsonSyntaxException {
        String json = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        ProxyExportData exportData = gson.fromJson(json, ProxyExportData.class);
        
        return exportData.toProxyModel();
    }
    
    /**
     * Data transfer object for exporting/importing proxies
     * Excludes the ID field since it should be regenerated on import
     */
    private static class ProxyExportData {
        private String domainName;
        private String description;
        private boolean isClient;
        private boolean showParser;
        private HttpParserModel parserSettings;
        
        // Constructor for export
        public ProxyExportData(ProxyModel proxy) {
            this.domainName = proxy.getDomainName();
            this.description = proxy.getDescription();
            this.isClient = proxy.isClient();
            this.showParser = proxy.isShowParser();
            
            // Copy parser settings but exclude forwarding rules
            this.parserSettings = copyParserSettingsWithoutForwardingRules(proxy.getParserSettings());
        }
        
        // Convert back to ProxyModel with new ID
        public ProxyModel toProxyModel() {
            ProxyModel proxy = new ProxyModel(domainName);
            proxy.setDescription(description);
            proxy.setClient(isClient);
            proxy.setShowParser(showParser);
            proxy.setParserSettings(parserSettings);
            return proxy;
        }
        
        private HttpParserModel copyParserSettingsWithoutForwardingRules(HttpParserModel original) {
            HttpParserModel copy = new HttpParserModel();
            
            // Copy all settings except loadBalancingRules
            copy.setHeaderLineEndings(original.getHeaderLineEndings());
            copy.setAllowHeaderFolding(original.isAllowHeaderFolding());
            copy.setDeleteHeaderRules(original.getDeleteHeaderRules());
            copy.setAddHeaderRules(original.getAddHeaderRules());
            copy.setBodyLenHeaderRules(original.getBodyLenHeaderRules());
            copy.setChunkedLineEndings(original.getChunkedLineEndings());
            copy.setRequestLineDelimiters(original.getRequestLineDelimiters());
            copy.setRewriteMethodEnabled(original.isRewriteMethodEnabled());
            copy.setFromMethod(original.getFromMethod());
            copy.setToMethod(original.getToMethod());
            copy.setDecodeUrlBeforeForwarding(original.isDecodeUrlBeforeForwarding());
            copy.setUrlDecodeFrom(original.getUrlDecodeFrom());
            copy.setUrlDecodeTo(original.getUrlDecodeTo());
            copy.setForcedHttpVersion(original.getForcedHttpVersion());
            copy.setCustomHttpVersion(original.getCustomHttpVersion());
            copy.setUseHeaderLinesJs(original.isUseHeaderLinesJs());
            copy.setHeaderLinesScript(original.getHeaderLinesScript());
            copy.setUseRequestLineJs(original.isUseRequestLineJs());
            copy.setRequestLineScript(original.getRequestLineScript());
            copy.setUseMessageLengthJs(original.isUseMessageLengthJs());
            copy.setMessageLengthScript(original.getMessageLengthScript());
            copy.setOutputBodyEncoding(original.getOutputBodyEncoding());
            
            // Explicitly set empty forwarding rules
            copy.setLoadBalancingRules(new java.util.ArrayList<>());
            
            return copy;
        }
    }
}