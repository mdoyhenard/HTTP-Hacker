package httpraider.model.network;

import java.io.Serializable;

public class FirewallRule implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public enum Source {
        METHOD("Method"),
        URL("URL"),
        VERSION("Version"),
        HEADERS("Headers"),
        BODY("Body"),
        FULL_REQUEST("Full Request");
        
        private final String displayName;
        
        Source(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
    }
    
    private Source source;
    private String jsCode;
    private boolean closeConnection;
    private boolean enabled;
    
    public FirewallRule() {
        this.source = Source.FULL_REQUEST;
        this.jsCode = "";  // Will be populated by UI with source-specific template
        this.closeConnection = false;
        this.enabled = true;
    }
    
    public FirewallRule(Source source, String jsCode, boolean closeConnection) {
        this.source = source;
        this.jsCode = jsCode;
        this.closeConnection = closeConnection;
        this.enabled = true;
    }
    
    public Source getSource() {
        return source;
    }
    
    public void setSource(Source source) {
        this.source = source;
    }
    
    public String getJsCode() {
        return jsCode;
    }
    
    public void setJsCode(String jsCode) {
        this.jsCode = jsCode;
    }
    
    public boolean isCloseConnection() {
        return closeConnection;
    }
    
    public void setCloseConnection(boolean closeConnection) {
        this.closeConnection = closeConnection;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}