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

    public HttpParserModel() {
        headerLineEndings = new ArrayList<>();
        headerLineEndings.add("\\r\\n");

        allowHeaderFolding = false;

        deleteHeaderRules = new ArrayList<>();
        addHeaderRules = new ArrayList<>();

        bodyLenHeaderRules = new ArrayList<>();
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
        headerLinesScript = "";

        useRequestLineJs = false;
        requestLineScript = "";

        useMessageLengthJs = false;
        messageLengthScript = "";

        outputBodyEncoding = MessageLenBodyEncoding.DONT_MODIFY;

        loadBalancingRules = new ArrayList<>();
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

}
