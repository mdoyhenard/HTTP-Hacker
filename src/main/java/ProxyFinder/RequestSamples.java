package ProxyFinder;

import Utils.Utils;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RequestSamples{

    private HttpRequestResponse baseReqResp;
    private List<Long> timeSamples;
    private MontoyaApi api;
    private static final int SAMPLE_COUNT = 10;

    public RequestSamples(MontoyaApi apiVal){
        api = apiVal;
    }

    public HttpRequestResponse getBaseReqResp() {
        return baseReqResp;
    }

    public List<Long> getTimeSamples() {
        return timeSamples;
    }

    public boolean sampleRequest(HttpRequest request){
        timeSamples = new ArrayList<>();
        baseReqResp = Utils.sendHTTPRequest(api, request.withPath(request.pathWithoutQuery()+"?cache-buster="+randstr(6)), 1);
        if (baseReqResp != null && baseReqResp.response().statusCode() >= 400 && baseReqResp.response().statusCode() != 403 && baseReqResp.response().statusCode() != 404){
            List<HttpRequestResponse> sampleList = new ArrayList<>();
            int failCount = 0;
            for (int i=0; i<SAMPLE_COUNT; i++){
                HttpRequestResponse sample = Utils.sendHTTPRequest(api, baseReqResp.request(), 1);
                if (sample != null && baseReqResp.response().statusCode() == sample.response().statusCode() && sample.timingData().isPresent()){
                    sampleList.add(sample);
                }
                else{
                    failCount++;
                }
            }
            if (failCount <= SAMPLE_COUNT/5){
                for (HttpRequestResponse reqResp : sampleList){
                    timeSamples.add((long) reqResp.timingData().get().timeBetweenRequestSentAndStartOfResponse().getNano());
                }
                return true;
            }
        }
        return false;
    }

    public static String randstr(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        Random rnd = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        HttpResponse r1=this.baseReqResp.response(), r2=((RequestSamples)obj).baseReqResp.response();
        if (r1.statusCode() != r2.statusCode()) return false;
        if (!r1.reasonPhrase().equals(r1.reasonPhrase())) return false;
        for (HttpHeader hd : r1.headers()){
            if (!r2.hasHeader(hd.name())) return false;
        }
        for (HttpHeader hd : r2.headers()){
            if (!r1.hasHeader(hd.name())) return false;
        }
        if (r1.hasHeader("server") && !r1.header("server").value().equals(r2.header("server").value())) return false;
        return true;
    }
}
