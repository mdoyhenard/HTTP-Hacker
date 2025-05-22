package RawRepeater;

import Network.ProxyConfig;
import Utils.HttpParser;

import java.util.ArrayList;
import java.util.List;

public class RawRequest {

    private String headers;
    private String body;
    private boolean isTE;

    public boolean isTE() {
        return isTE;
    }

    public int getLength(){
        return (headers==null? 0 :headers.length())+(body==null ? 0 : body.length());
    }

    public void setTE(boolean TE) {
        isTE = TE;
    }

    public RawRequest(){
        this.headers = "";
        this.body = "";
    }

    public String getHeaders() {
        return headers;
    }

    public void setHeaders(String headers) {
        this.headers = headers;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public static List<RawRequest> parseRequestArea(String request, ProxyConfig proxyConfig){
        ArrayList<RawRequest> rawRequests = new ArrayList<>();
        RawRequest rr;
        while (request.contains(proxyConfig.getEscapedHeadersEnd())){
            rr = new RawRequest();
            int currentIndx = request.indexOf(proxyConfig.getEscapedHeadersEnd())+proxyConfig.getEscapedHeadersEnd().length();
            rr.setHeaders(request.substring(0, currentIndx));
            request = request.substring(currentIndx);
            for (int i = 0; i<proxyConfig.getMessageLengths().size(); i++){
                if (rr.getHeaders().contains(proxyConfig.getMessageLengths().get(i).getEscapedPreffixPattern())){
                    if (proxyConfig.getMessageLengths().get(i).isTE()){
                        rr.setTE(true);
                        currentIndx = HttpParser.parseTEbody(request);
                        if (currentIndx == -1){
                            rr.setBody(request);
                            rawRequests.add(rr);
                            return rawRequests;
                        }
                    }
                    else {
                        currentIndx = HttpParser.parseCLheader(rr.getHeaders(), proxyConfig.getMessageLengths().get(i));
                    }
                    if (currentIndx >= 0) {
                        rr.setBody(request.substring(0, currentIndx));
                        request = request.substring(currentIndx);
                    }
                    else {
                        rr.setBody("");
                    }
                    break;
                }
            }
            rawRequests.add(rr);
        }
        if (!request.isEmpty()){
            rr = new RawRequest();
            rr.setHeaders(request);
            rr.setBody("");
            rawRequests.add(rr);
        }
        return rawRequests;
    }

}
