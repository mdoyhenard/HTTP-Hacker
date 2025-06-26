// file: httpraider/model/network/ProxyModel.java
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
    private boolean isClient;

    private HTTPParserSettings parserSettings;
    private HTTPParserSettings forwardingSettings;

    public ProxyModel(String id,
                      String domainName,
                      String description,
                      String basePath,
                      String parsingCode,
                      String interpretationCode,
                      String forwardingCode,
                      Map<String, String> forwardingRules) {
        this.id = id == null ? UUID.randomUUID().toString() : id;
        this.domainName = domainName;
        this.description = description;
        this.basePath = basePath;
        this.isClient = false;
        this.parserSettings = new HTTPParserSettings();
        this.forwardingSettings = new HTTPParserSettings();
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

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public HTTPParserSettings getParserSettings() {
        return parserSettings;
    }

    public void setParserSettings(HTTPParserSettings parserSettings) {
        this.parserSettings = parserSettings;
    }

    public HTTPParserSettings getForwardingSettings() {
        return forwardingSettings;
    }
    public void setForwardingSettings(HTTPParserSettings forwardingSettings) {
        this.forwardingSettings = forwardingSettings;
    }

}
