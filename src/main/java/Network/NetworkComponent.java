package Network;

import ProxyFinder.RequestSamples;
import Utils.HttpParser;

import java.util.ArrayList;
import java.util.List;

public class NetworkComponent {

    protected String vendor;
    protected String description;
    protected String basePath;
    protected HttpParser parser;
    protected List<RequestSamples> samples;

    public NetworkComponent(){
        this.vendor = "";
        this.description = "";
        this. basePath = "";
        this.parser = new HttpParser();
        this.samples = new ArrayList<>();
    }

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

    public HttpParser getParser() {
        return parser;
    }

    public void setParser(HttpParser parser) {
        this.parser = parser;
    }

    public List<RequestSamples> getSamples() {
        return samples;
    }

    public void setSamples(List<RequestSamples> samples) {
        this.samples = samples;
    }

    public String getType(){
        return "type";
    }

    public void setType(String type){}
}
