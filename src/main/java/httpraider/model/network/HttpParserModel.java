package httpraider.model.network;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class HttpParserModel implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // --- Header line ending patterns ---
    private List<String> headerLineEndings;

    // --- Header folding ---
    private boolean allowHeaderFolding;

    // --- Header delete and add rules ---
    private List<String> deleteHeaderRules;
    private List<String> addHeaderRules;

    // --- Message-length headers ---
    private List<BodyLenHeaderRule> bodyLenHeaderRules;

    // --- Chunked line ending patterns ---
    private List<String> chunkedLineEndings;

    // --- Request line configuration ---
    private List<String> requestLineDelimiters;
    private boolean rewriteMethodEnabled;
    private String fromMethod;
    private String toMethod;
    private boolean decodeUrlBeforeForwarding;
    private String urlDecodeFrom;
    private String urlDecodeTo;
    private ForcedHttpVersion forcedHttpVersion;
    private String customHttpVersion;

    // --- JS code for each step ---
    private boolean useHeaderLinesJs;
    private String headerLinesScript;

    private boolean useRequestLineJs;
    private String requestLineScript;

    private boolean useMessageLengthJs;
    private String messageLengthScript;

    // --- Message-Length Body Encoding Option ---
    private MessageLenBodyEncoding outputBodyEncoding;

    // --- Load balancing rules ---
    private List<LoadBalancingRule> loadBalancingRules = new ArrayList<>();

    // --- Firewall rules ---
    private List<FirewallRule> firewallRules = new ArrayList<>();

    public HttpParserModel() {
        headerLineEndings = new ArrayList<>();
        headerLineEndings.add("\\r\\n");

        allowHeaderFolding = false;

        deleteHeaderRules = new ArrayList<>();
        addHeaderRules = new ArrayList<>();

        bodyLenHeaderRules = new ArrayList<>();
        // Transfer-Encoding takes precedence over Content-Length per RFC 7230
        bodyLenHeaderRules.add(new BodyLenHeaderRule("Transfer-Encoding: chunked", true, DuplicateHandling.LAST));
        bodyLenHeaderRules.add(new BodyLenHeaderRule("Content-Length: ", false, DuplicateHandling.FIRST));

        chunkedLineEndings = new ArrayList<>();
        chunkedLineEndings.add("\\r\\n");

        requestLineDelimiters = new ArrayList<>();
        requestLineDelimiters.add(" ");

        rewriteMethodEnabled = false;
        fromMethod = "";
        toMethod = "";

        decodeUrlBeforeForwarding = false;
        urlDecodeFrom = "%21";
        urlDecodeTo = "%7f";

        forcedHttpVersion = ForcedHttpVersion.AUTO;
        customHttpVersion = "";

        useHeaderLinesJs = false;
        headerLinesScript = getDefaultHeaderLinesScript();

        useRequestLineJs = false;
        requestLineScript = getDefaultRequestLineScript();

        useMessageLengthJs = false;
        messageLengthScript = getDefaultMessageLengthScript();

        outputBodyEncoding = MessageLenBodyEncoding.DONT_MODIFY;

        loadBalancingRules = new ArrayList<>();
        firewallRules = new ArrayList<>();
    }

    // --- Getters & Setters ---

    public List<String> getHeaderLineEndings() { return headerLineEndings; }
    public void setHeaderLineEndings(List<String> headerLineEndings) { this.headerLineEndings = headerLineEndings; }

    public boolean isAllowHeaderFolding() { return allowHeaderFolding; }
    public void setAllowHeaderFolding(boolean allowHeaderFolding) { this.allowHeaderFolding = allowHeaderFolding; }

    public List<String> getDeleteHeaderRules() { return deleteHeaderRules; }
    public void setDeleteHeaderRules(List<String> deleteHeaderRules) {
        this.deleteHeaderRules = deleteHeaderRules != null ? new ArrayList<>(deleteHeaderRules) : new ArrayList<>();
    }

    public List<String> getAddHeaderRules() { return addHeaderRules; }
    public void setAddHeaderRules(List<String> addHeaderRules) {
        this.addHeaderRules = addHeaderRules != null ? new ArrayList<>(addHeaderRules) : new ArrayList<>();
    }

    public List<BodyLenHeaderRule> getBodyLenHeaderRules() { return bodyLenHeaderRules; }
    public void setBodyLenHeaderRules(List<BodyLenHeaderRule> bodyLenHeaderRules) { this.bodyLenHeaderRules = bodyLenHeaderRules; }

    public List<String> getChunkedLineEndings() { return chunkedLineEndings; }
    public void setChunkedLineEndings(List<String> chunkedLineEndings) { this.chunkedLineEndings = chunkedLineEndings; }

    public List<String> getRequestLineDelimiters() { return requestLineDelimiters; }
    public void setRequestLineDelimiters(List<String> requestLineDelimiters) { this.requestLineDelimiters = requestLineDelimiters; }

    public boolean isRewriteMethodEnabled() { return rewriteMethodEnabled; }
    public void setRewriteMethodEnabled(boolean rewriteMethodEnabled) { this.rewriteMethodEnabled = rewriteMethodEnabled; }

    public String getFromMethod() { return fromMethod; }
    public void setFromMethod(String fromMethod) { this.fromMethod = fromMethod; }

    public String getToMethod() { return toMethod; }
    public void setToMethod(String toMethod) { this.toMethod = toMethod; }

    public boolean isDecodeUrlBeforeForwarding() { return decodeUrlBeforeForwarding; }
    public void setDecodeUrlBeforeForwarding(boolean decodeUrlBeforeForwarding) { this.decodeUrlBeforeForwarding = decodeUrlBeforeForwarding; }

    public String getUrlDecodeFrom() { return urlDecodeFrom; }
    public void setUrlDecodeFrom(String urlDecodeFrom) { this.urlDecodeFrom = urlDecodeFrom; }

    public String getUrlDecodeTo() { return urlDecodeTo; }
    public void setUrlDecodeTo(String urlDecodeTo) { this.urlDecodeTo = urlDecodeTo; }

    public ForcedHttpVersion getForcedHttpVersion() { return forcedHttpVersion; }
    public void setForcedHttpVersion(ForcedHttpVersion forcedHttpVersion) { this.forcedHttpVersion = forcedHttpVersion; }

    public String getCustomHttpVersion() { return customHttpVersion; }
    public void setCustomHttpVersion(String customHttpVersion) { this.customHttpVersion = customHttpVersion; }

    public boolean isUseHeaderLinesJs() { return useHeaderLinesJs; }
    public void setUseHeaderLinesJs(boolean useHeaderLinesJs) { this.useHeaderLinesJs = useHeaderLinesJs; }

    public String getHeaderLinesScript() { return headerLinesScript; }
    public void setHeaderLinesScript(String headerLinesScript) { this.headerLinesScript = headerLinesScript; }

    public boolean isUseRequestLineJs() { return useRequestLineJs; }
    public void setUseRequestLineJs(boolean useRequestLineJs) { this.useRequestLineJs = useRequestLineJs; }

    public String getRequestLineScript() { return requestLineScript; }
    public void setRequestLineScript(String requestLineScript) { this.requestLineScript = requestLineScript; }

    public boolean isUseMessageLengthJs() { return useMessageLengthJs; }
    public void setUseMessageLengthJs(boolean useMessageLengthJs) { this.useMessageLengthJs = useMessageLengthJs; }

    public String getMessageLengthScript() { return messageLengthScript; }
    public void setMessageLengthScript(String messageLengthScript) { this.messageLengthScript = messageLengthScript; }

    public MessageLenBodyEncoding getOutputBodyEncoding() { return outputBodyEncoding; }
    public void setOutputBodyEncoding(MessageLenBodyEncoding outputBodyEncoding) { this.outputBodyEncoding = outputBodyEncoding; }

    public List<LoadBalancingRule> getLoadBalancingRules() { return loadBalancingRules; }
    public void setLoadBalancingRules(List<LoadBalancingRule> rules) {
        this.loadBalancingRules = rules != null ? new ArrayList<>(rules) : new ArrayList<>();
    }

    public List<FirewallRule> getFirewallRules() { return firewallRules; }
    public void setFirewallRules(List<FirewallRule> rules) {
        this.firewallRules = rules != null ? new ArrayList<>(rules) : new ArrayList<>();
    }

    // --- Nested types ---

    public enum DuplicateHandling {
        FIRST, LAST, ERROR
    }

    public enum ForcedHttpVersion {
        AUTO, HTTP_1_0, HTTP_1_1
    }

    public enum MessageLenBodyEncoding {
        FORCE_CHUNKED, FORCE_CL_HEADER, DONT_MODIFY
    }

    public static class BodyLenHeaderRule implements Serializable {
        private String pattern;
        private boolean isChunked;
        private DuplicateHandling duplicateHandling;

        public BodyLenHeaderRule(String pattern, boolean isChunked, DuplicateHandling duplicateHandling) {
            this.pattern = pattern;
            this.isChunked = isChunked;
            this.duplicateHandling = duplicateHandling;
        }

        public BodyLenHeaderRule() {
            this("", false, DuplicateHandling.FIRST);
        }

        public String getPattern() { return pattern; }
        public void setPattern(String pattern) { this.pattern = pattern; }

        public boolean isChunked() { return isChunked; }
        public void setChunked(boolean chunked) { isChunked = chunked; }

        public DuplicateHandling getDuplicateHandling() { return duplicateHandling; }
        public void setDuplicateHandling(DuplicateHandling duplicateHandling) { this.duplicateHandling = duplicateHandling; }
    }

    // --- Default JavaScript Templates ---

    private static String getDefaultHeaderLinesScript() {
        return """
            // Input: headerLines (array), headerBlock (string)
            // Output: outHeaderLines (array)
            
            function findHeader(lines, name) {
                var lowerName = name.toLowerCase();
                for (var i = 0; i < lines.length; i++) {
                    if (lines[i].toLowerCase().indexOf(lowerName + ':') === 0) {
                        return i;
                    }
                }
                return -1;
            }
            
            function getHeaderValue(lines, name) {
                var idx = findHeader(lines, name);
                if (idx >= 0) {
                    var colonPos = lines[idx].indexOf(':');
                    return lines[idx].substring(colonPos + 1).trim();
                }
                return null;
            }
            
            function setHeader(lines, name, value) {
                var idx = findHeader(lines, name);
                var newHeader = name + ': ' + value;
                if (idx >= 0) {
                    lines[idx] = newHeader;
                } else {
                    lines.push(newHeader);
                }
            }
            
            function removeHeader(lines, name) {
                var idx = findHeader(lines, name);
                if (idx >= 0) {
                    lines.splice(idx, 1);
                }
            }
            
            var processedLines = [];
            for (var i = 0; i < headerLines.length; i++) {
                processedLines.push(headerLines[i]);
            }
            
            // setHeader(processedLines, 'X-Custom', 'value');
            
            outHeaderLines = processedLines;
            """;
    }

    private static String getDefaultRequestLineScript() {
        return """
            // Input: input (string)
            // Output: output (string)
            
            function parseRequestLine(line) {
                var parts = line.split(' ');
                if (parts.length >= 3) {
                    return {
                        method: parts[0],
                        path: parts.slice(1, -1).join(' '),
                        version: parts[parts.length - 1]
                    };
                }
                return null;
            }
            
            function urlEncode(str) {
                return encodeURIComponent(str).replace(/%20/g, '+');
            }
            
            function urlDecode(str) {
                return decodeURIComponent(str.replace(/\\+/g, '%20'));
            }
            
            var parsed = parseRequestLine(input);
            
            if (parsed) {
                // parsed.method = 'POST';
                
                output = parsed.method + ' ' + parsed.path + ' ' + parsed.version;
            } else {
                output = input;
            }
            """;
    }

    private static String getDefaultMessageLengthScript() {
        return """
            // Input: headerLines (array), body (string), buffer (string)
            // Output: outBody (string), outBuffer (string)
            
            function parseChunkedBody(data) {
                var chunks = [];
                var remaining = data;
                
                while (remaining.length > 0) {
                    var lineEnd = remaining.indexOf('\\r\\n');
                    if (lineEnd === -1) break;
                    
                    var sizeLine = remaining.substring(0, lineEnd);
                    var chunkSize = parseInt(sizeLine, 16);
                    
                    if (isNaN(chunkSize) || chunkSize < 0) break;
                    
                    if (chunkSize === 0) {
                        remaining = remaining.substring(lineEnd + 2);
                        var endPos = remaining.indexOf('\\r\\n\\r\\n');
                        if (endPos >= 0) {
                            remaining = remaining.substring(endPos + 4);
                        } else {
                            remaining = '';
                        }
                        break;
                    }
                    
                    var chunkStart = lineEnd + 2;
                    var chunkEnd = chunkStart + chunkSize;
                    
                    if (chunkEnd + 2 > remaining.length) break;
                    
                    chunks.push(remaining.substring(chunkStart, chunkEnd));
                    remaining = remaining.substring(chunkEnd + 2);
                }
                
                return {
                    body: chunks.join(''),
                    remaining: remaining
                };
            }
            
            function createChunkedBody(plainBody) {
                var chunked = '';
                var chunkSize = 1024;
                
                for (var i = 0; i < plainBody.length; i += chunkSize) {
                    var chunk = plainBody.substring(i, Math.min(i + chunkSize, plainBody.length));
                    chunked += chunk.length.toString(16) + '\\r\\n' + chunk + '\\r\\n';
                }
                
                chunked += '0\\r\\n\\r\\n';
                return chunked;
            }
            
            outBody = body;
            outBuffer = buffer;
            
            // var decoded = parseChunkedBody(body + buffer);
            """;
    }

}
