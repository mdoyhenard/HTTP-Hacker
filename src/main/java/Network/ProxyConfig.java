package Network;

import RawRepeater.MsgLenHeader;
import Utils.HttpParser;
import org.apache.commons.text.StringEscapeUtils;

import java.util.ArrayList;
import java.util.List;

public class ProxyConfig {

    private String name;
    private String headersEnd;
    private List<MsgLenHeader> messageLengths;
    private ProxyConfig nextProxy;
    private HttpParser parser;
    private List<String> jsCodes;
    private String vendor;
    private String description;

    public ProxyConfig(String name, String headersEnd) {
        this.headersEnd = headersEnd;
        this.messageLengths = new ArrayList<>();
    }

    public ProxyConfig() {
        this.headersEnd = "\r\n\r\n";
        this.messageLengths = new ArrayList<>();
    }

    public ProxyConfig(ProxyConfig proxyConfig){
        this.name = proxyConfig.name;
        this.headersEnd = proxyConfig.headersEnd;
        this.messageLengths = new ArrayList<>();
        for (MsgLenHeader msglen : proxyConfig.messageLengths){
            messageLengths.add(new MsgLenHeader(msglen));
        }
        this.nextProxy = proxyConfig.nextProxy;
        this.parser = new HttpParser(proxyConfig.parser);
        this.jsCodes = new ArrayList<>();
        for (String trans : proxyConfig.jsCodes){
            this.jsCodes.add(new String(trans));
        }
        this.vendor = proxyConfig.vendor;
        this.description = proxyConfig.description;
    }

    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name = name;
    }

    public ProxyConfig(String headersEnd, List<MsgLenHeader> messageLengths) {
        this.headersEnd = headersEnd;
        this.messageLengths = messageLengths;
    }

    public void addMsgLenHeader(MsgLenHeader header){
        this.messageLengths.add(header);
    }

    public String getHeadersEnd() {
        return headersEnd;
    }

    public String getEscapedHeadersEnd() {
        return StringEscapeUtils.unescapeJava(headersEnd);
    }

    public void setHeadersEnd(String headersEnd) {
        this.headersEnd = headersEnd;
    }

    public List<MsgLenHeader> getMessageLengths() {
        return messageLengths;
    }

    public void setMessageLengths(List<MsgLenHeader> messageLengths) {
        this.messageLengths = messageLengths;
    }

    public ProxyConfig getNextProxy() {
        return nextProxy;
    }

    public void setNextProxy(ProxyConfig nextProxy) {
        this.nextProxy = nextProxy;
    }

    public HttpParser getParser() {
        return parser;
    }

    public void setParser(HttpParser parser) {
        this.parser = parser;
    }

    public List<String> getTransformations() {
        return jsCodes;
    }

    public void setTransformations(List<String> jsCodes) {
        this.jsCodes = jsCodes;
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
}

