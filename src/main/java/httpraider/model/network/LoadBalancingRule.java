package httpraider.model.network;

import java.io.Serializable;

public class LoadBalancingRule implements Serializable {
    private String forwardToProxyId;
    private boolean enabled;
    private RuleType ruleType;

    // For Headers
    private HeaderField headerField;
    private MatchMode matchMode;
    private MatchMode matchOption;
    private String pattern;
    private String jsCode;

    // For Host
    private String hostPattern;
    private MatchMode hostMatchMode;

    // For Cookies
    private String cookieNamePattern;
    private MatchMode cookieNameMatchMode;
    private String cookieValuePattern;
    private MatchMode cookieValueMatchMode;

    // For Header Name+Value
    private String headerNamePattern;
    private MatchMode headerNameMatchMode;
    private String headerValuePattern;
    private MatchMode headerValueMatchMode;

    public LoadBalancingRule() {
        this.forwardToProxyId = null;
        this.enabled = false;
        this.ruleType = RuleType.URL;
        this.headerField = HeaderField.NAME;
        this.matchMode = MatchMode.EXACT;
        this.matchOption = MatchMode.EXACT;
        this.pattern = "";
        this.jsCode = "";
        this.hostPattern = "";
        this.hostMatchMode = MatchMode.EXACT;
        this.cookieNamePattern = "";
        this.cookieNameMatchMode = MatchMode.EXACT;
        this.cookieValuePattern = "";
        this.cookieValueMatchMode = MatchMode.EXACT;
        this.headerNamePattern = "";
        this.headerNameMatchMode = MatchMode.EXACT;
        this.headerValuePattern = "";
        this.headerValueMatchMode = MatchMode.EXACT;
    }

    public String getForwardToProxyId() { return forwardToProxyId; }
    public void setForwardToProxyId(String id) { this.forwardToProxyId = id; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public RuleType getRuleType() { return ruleType; }
    public void setRuleType(RuleType ruleType) { this.ruleType = ruleType; }

    public HeaderField getHeaderField() { return headerField; }
    public void setHeaderField(HeaderField headerField) { this.headerField = headerField; }
    public MatchMode getMatchMode() { return matchMode; }
    public void setMatchMode(MatchMode matchMode) { this.matchMode = matchMode; }
    public MatchMode getMatchOption() { return matchOption; }
    public void setMatchOption(MatchMode matchOption) { this.matchOption = matchOption; }
    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }
    public String getJsCode() { return jsCode; }
    public void setJsCode(String jsCode) { this.jsCode = jsCode; }

    // Host
    public String getHostPattern() { return hostPattern; }
    public void setHostPattern(String pattern) { this.hostPattern = pattern; }
    public MatchMode getHostMatchMode() { return hostMatchMode; }
    public void setHostMatchMode(MatchMode mode) { this.hostMatchMode = mode; }

    // Cookies
    public String getCookieNamePattern() { return cookieNamePattern; }
    public void setCookieNamePattern(String pattern) { this.cookieNamePattern = pattern; }
    public MatchMode getCookieNameMatchMode() { return cookieNameMatchMode; }
    public void setCookieNameMatchMode(MatchMode mode) { this.cookieNameMatchMode = mode; }
    public String getCookieValuePattern() { return cookieValuePattern; }
    public void setCookieValuePattern(String pattern) { this.cookieValuePattern = pattern; }
    public MatchMode getCookieValueMatchMode() { return cookieValueMatchMode; }
    public void setCookieValueMatchMode(MatchMode mode) { this.cookieValueMatchMode = mode; }

    // Header Name+Value
    public String getHeaderNamePattern() { return headerNamePattern; }
    public void setHeaderNamePattern(String p) { this.headerNamePattern = p; }
    public MatchMode getHeaderNameMatchMode() { return headerNameMatchMode; }
    public void setHeaderNameMatchMode(MatchMode m) { this.headerNameMatchMode = m; }
    public String getHeaderValuePattern() { return headerValuePattern; }
    public void setHeaderValuePattern(String p) { this.headerValuePattern = p; }
    public MatchMode getHeaderValueMatchMode() { return headerValueMatchMode; }
    public void setHeaderValueMatchMode(MatchMode m) { this.headerValueMatchMode = m; }
}
