package httpraider.model.network;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

public class ProxyModel implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final String CLIENT_ID = "client";

    private final String id;
    private String domainName;
    private String description;
    private String basePath;
    private String parsingCode;
    private String interpretationCode;
    private String forwardingCode;
    private Map<String, String> forwardingRules;
    private boolean isClient;

    public ProxyModel(String id, String domainName, String description, String basePath, String parsingCode, String interpretationCode, String forwardingCode, Map<String, String> forwardingRules) {
        this.id = id == null ? UUID.randomUUID().toString() : id;
        this.domainName = domainName;
        this.description = description;
        this.basePath = basePath;
        this.parsingCode = parsingCode;
        this.interpretationCode = interpretationCode;
        this.forwardingCode = forwardingCode;
        this.forwardingRules = forwardingRules;
        this.isClient = false;
    }

    public ProxyModel(String domainName) {
        this(null, domainName, "", "", "", "", "", Map.of());
    }

    public String getId() {
        return id;
    }

    public boolean isClient() {
        return isClient;
    }

    public void setClient(boolean client) {
        isClient = client;
    }

    public String getDomainName() {
        return domainName;
    }

    public String getDescription() {
        return description;
    }

    public String getBasePath() {
        return basePath;
    }

    public String getParsingCode() {
        return parsingCode;
    }

    public String getInterpretationCode() {
        return interpretationCode;
    }

    public String getForwardingCode() {
        return forwardingCode;
    }

    public Map<String, String> getForwardingRules() {
        return forwardingRules;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public void setParsingCode(String parsingCode) {
        this.parsingCode = parsingCode;
    }

    public void setInterpretationCode(String interpretationCode) {
        this.interpretationCode = interpretationCode;
    }

    public void setForwardingCode(String forwardingCode) {
        this.forwardingCode = forwardingCode;
    }

    public void setForwardingRules(Map<String, String> forwardingRules) {
        this.forwardingRules = forwardingRules;
    }
}
