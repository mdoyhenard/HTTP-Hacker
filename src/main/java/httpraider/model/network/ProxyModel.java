package httpraider.model.network;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

public class ProxyModel implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final String CLIENT_ID = "client";

    private final String id;
    private String domainName;
    private String description;
    private boolean isClient;
    private boolean showParser;

    private HttpParserModel parserSettings;

    public ProxyModel(String id,
                      String domainName,
                      String description) {
        this.id = id == null ? UUID.randomUUID().toString() : id;
        this.domainName = domainName;
        this.description = description;
        this.isClient = false;
        this.parserSettings = new HttpParserModel();
        this.showParser = false;
    }

    public ProxyModel(String domainName) {
        this(null, domainName, "");
    }

    public String getId() {
        return id;
    }

    public boolean isShowParser() { return showParser; }

    public void setShowParser(boolean showParser) { this.showParser = showParser; }

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

    public HttpParserModel getParserSettings() {
        return parserSettings;
    }

    public void setParserSettings(HttpParserModel parserSettings) {
        this.parserSettings = parserSettings;
    }
}
