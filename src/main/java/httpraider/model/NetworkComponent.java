package httpraider.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

public class NetworkComponent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    private final String id;

    protected String vendor;
    protected String description;
    protected String basePath;

    public NetworkComponent(){
        id = UUID.randomUUID().toString();
        vendor = "";
        description = "";
        basePath = "";
    }

    public String getId() { return id; }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
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

    public String getType(){
        return "type";
    }

    public void setType(String type){}

}
