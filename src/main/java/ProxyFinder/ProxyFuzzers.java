package ProxyFinder;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.requests.HttpRequest;

import java.util.ArrayList;
import java.util.List;

public class ProxyFuzzers {
    private final MontoyaApi api;
    private static final int SAMPLE_COUNT = 10;

    public ProxyFuzzers(MontoyaApi api){
        this.api = api;
    }

    public List<RequestSamples> fuzzMethodd(HttpRequest req){
        List<RequestSamples> interesting = new ArrayList<>();

        List<String> testcase = new ArrayList<>();
        testcase.add("AGET");
        testcase.add("AbcD");
        testcase.add("POST");
        testcase.add("PUT");
        testcase.add("get");
        //testcase.add("GET\tA");
        testcase.add("GET\00A");  //1
        testcase.add("GET\01A");
        testcase.add("GET±A");
        testcase.add("GET#A");
        testcase.add("G%T");
        testcase.add("GE%54");
        testcase.add("G<script>");
        testcase.add("GEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEET");
        testcase.add("TRACE");

        for (String method: testcase){
            RequestSamples reqSamp = new RequestSamples(this.api);
            if (reqSamp.sampleRequest(req.withMethod(method))) interesting.add(reqSamp);
        }

        return interesting;
    }

    public List<RequestSamples> fuzzURL(HttpRequest req){
        List<RequestSamples> interesting = new ArrayList<>();
        String baseURL = req.pathWithoutQuery();

        List<String> testcase = new ArrayList<>();
        testcase.add(baseURL + "#abc");
        testcase.add(baseURL + ";abc");
        testcase.add(baseURL + "eval()abc");
        testcase.add(baseURL + "[exec]abc");
        testcase.add(baseURL + "%00abc");
        testcase.add(baseURL + "%01abc");
        testcase.add(baseURL + "\00abc"); //2
        testcase.add(baseURL + "\01abc");
        testcase.add(baseURL + "\rabc");
        testcase.add(baseURL + "±abc");
        testcase.add(baseURL + "{{1-hjk*±}}");
        testcase.add(baseURL + "<script>alert('XSS')</script>abc");
        testcase.add(baseURL + "'or'1'='1");
        testcase.add(baseURL + "%2500abc");
        testcase.add(baseURL + "%E5%C400abc");
        testcase.add(baseURL + "%25%30%30abc");
        testcase.add(baseURL + "%25%32%35%30%30abc");/*
        testcase.add(baseURL + "A".repeat(1024));
        testcase.add(baseURL + "A".repeat(2048));*/
        testcase.add(baseURL + "A".repeat(4096));/*
        testcase.add(baseURL + "/../../../../../../../../../../../abc");
        testcase.add(baseURL + "\\..\\..\\..\\..\\..\\..\\..\\..\\..\\..\\..\\..\\..\\..\\abc");*//*
        testcase.add(baseURL + "%2f%2e%2e%2fabc");
        testcase.add(baseURL + "%2f%2e%2e%2f%2e%2e%2f%2e%2e%2f%2e%2e%2f%2e%2e%2f%2e%2e%2f%2e%2e%2f%2e%2e%2f%2e%2e%2f%2e%2e%2fabc");*/
        testcase.add(baseURL + "?%00abc");
        testcase.add(baseURL + "?<script>alert('XSS')</script>");

        for (String path: testcase){
            RequestSamples reqSamp = new RequestSamples(this.api);
            if (reqSamp.sampleRequest(req.withPath(path))) interesting.add(reqSamp);
        }

        return interesting;
    }

    public List<RequestSamples> fuzzHeaderName(HttpRequest req){

        List<String> testcase = new ArrayList<>();
        testcase.add("Abc%61def");
        testcase.add("Abc%00def");
        testcase.add("Abc\00def");
        testcase.add("abc\01def");
        testcase.add("abc;ef");
        testcase.add("abc±def");
        testcase.add("abc/../def");
        testcase.add("abc<def");
        testcase.add("abc<script>def");
        testcase.add("abc'def");
        testcase.add("abc'or'1'='1def");
        testcase.add("abc@*&efg");
        testcase.add("abc def");
        testcase.add(" abcdef");

        List<RequestSamples> interesting = new ArrayList<>();
        for (String header: testcase){
            RequestSamples reqSamp = new RequestSamples(this.api);
            if (reqSamp.sampleRequest(req.withAddedHeader(header, "value"))) interesting.add(reqSamp);
        }

        return interesting;

    }

    public List<RequestSamples> fuzzHostHeader(HttpRequest request){
        String host = request.headerValue("host");
        HttpRequest req = request.withRemovedHeader("host");
        List<String> testcase = new ArrayList<>();
        testcase.add("abcdefg");
        testcase.add("aaaa.com/");
        testcase.add("Abc\00def");
        testcase.add("abc\01def");
        testcase.add("abc;ef");
        testcase.add("abcd.com,cdef");
        testcase.add("abc±def");
        testcase.add("google.com|");
        testcase.add("abc/../def");
        testcase.add("abc<def");
        testcase.add("abc<script>def");
        testcase.add("abc'def");
        testcase.add("abc'or'1'='1def");
        testcase.add("abc@*&efg");
        testcase.add("abc def");
        testcase.add(" abcdef");

        List<RequestSamples> interesting = new ArrayList<>();
        for (String header: testcase){
            RequestSamples reqSamp = new RequestSamples(this.api);
            if (reqSamp.sampleRequest(req.withAddedHeader("host", header+"."+host))) interesting.add(reqSamp);
        }

        return interesting;
    }

    public List<RequestSamples> fuzzCLHeader(HttpRequest req){
        List<String> testcase = new ArrayList<>();
        testcase.add("123123123123123");
        testcase.add("-123123");
        testcase.add("Abdef");
        testcase.add("Abc\00def");
        testcase.add("abc\01def");
        testcase.add("0;123");
        testcase.add("0,1234");

        List<RequestSamples> interesting = new ArrayList<>();
        for (String header: testcase){
            RequestSamples reqSamp = new RequestSamples(this.api);
            if (reqSamp.sampleRequest(req.withBody("1234567890").withRemovedHeader("content-length").withRemovedHeader("transfer-encoding").withMethod("POST").withAddedHeader("content-length", header))) interesting.add(reqSamp);
        }

        RequestSamples reqSamp = new RequestSamples(this.api);
        if (reqSamp.sampleRequest(req.withRemovedHeader("content-length").withRemovedHeader("transfer-encoding").withAddedHeader("content-length", "10").withMethod("GET").withBody("1234567890"))) interesting.add(reqSamp);

        return interesting;
    }

    public List<RequestSamples> fuzzTEHeader(HttpRequest req){
        List<String> testcase = new ArrayList<>();
        testcase.add("chunked,abcd");
        testcase.add("abcd,chunked");
        testcase.add("identity");
        testcase.add("Abdef");
        testcase.add("chunked\00def");
        testcase.add("identity,\01chunked");
        testcase.add(" ");
        testcase.add("chunked;chunked;identity");

        List<RequestSamples> interesting = new ArrayList<>();
        for (String header: testcase){
            RequestSamples reqSamp = new RequestSamples(this.api);
            if (reqSamp.sampleRequest(req.withMethod("POST").withBody("0\r\n\r\n").withRemovedHeader("transfer-encoding").withAddedHeader("transfer-encoding", header).withRemovedHeader("content-length"))) interesting.add(reqSamp);
        }
        RequestSamples reqSamp = new RequestSamples(this.api);
        if (reqSamp.sampleRequest(req.withMethod("POST").withBody("0\r\n\r\n").withRemovedHeader("transfer-encoding").withAddedHeader("transfer-encoding", "chunked").withAddedHeader("content-length", "5"))) interesting.add(reqSamp);


        return interesting;
    }

    public List<RequestSamples> fuzzHTTP2(HttpRequest req){
        List<HttpRequest> testcase = new ArrayList<>();
        testcase.add(req.withAddedHeader("connection", "keep-alive"));
        testcase.add(req.withAddedHeader(":authority", "abcd"));
        testcase.add(req.withAddedHeader(":abcde", "abcd"));
        testcase.add(req.withAddedHeader(":method", "abcd"));
        testcase.add(req.withAddedHeader(":aaa bbb", "abcd"));
        testcase.add(req.withAddedHeader("abc", "a".repeat(4000)));
        testcase.add(req.withAddedHeader("a".repeat(4000), "abc"));

        List<RequestSamples> interesting = new ArrayList<>();
        for (HttpRequest request: testcase){
            RequestSamples reqSamp = new RequestSamples(this.api);
            if (reqSamp.sampleRequest(request)) interesting.add(reqSamp);
        }

        return interesting;
    }

    public List<RequestSamples> fuzzUpgradeHeader(HttpRequest req){
        List<String> testcase = new ArrayList<>();
        testcase.add("h2c");
        testcase.add("websocket");
        testcase.add("h2c,websocket");
        testcase.add("h2c,websocket");
        testcase.add("abcde");
        testcase.add("aaaa\00bbbb");
        testcase.add("aaaa\01bbbb");
        testcase.add("aaaa bbbb");
        testcase.add("aaaa/bbbb");
        testcase.add("aaaa<script>bbbb");
        testcase.add("aaaa'or'1'='1--bbbb");
        testcase.add("aaaa∞¢#¡€bbbb");

        List<RequestSamples> interesting = new ArrayList<>();
        for (String header: testcase){
            RequestSamples reqSamp = new RequestSamples(this.api);
            if (reqSamp.sampleRequest(req.withRemovedHeader("upgrade").withAddedHeader("upgrade", header))) interesting.add(reqSamp);
        }

        return interesting;
    }

    public List<RequestSamples> fuzzTEChunk(HttpRequest req){
        List<String> testcase = new ArrayList<>();
        testcase.add("123123123123123\r\nabcd\r\n0\r\n\r\n");
        testcase.add("ghijk\r\nabcd\r\n0\r\n\r\n");
        testcase.add("-ffff\r\n\r\n0\r\n\r\n");
        testcase.add("\00"+"0\r\n\r\n");
        testcase.add("0;\00qwe\r\n\r\n");
        testcase.add("0;/abc/\r\n\r\n");
        testcase.add("0 123\r\n\r\n0\r\n\r\n");
        testcase.add("0:dgsahj\r\n\r\n");
        testcase.add("0;±@jdfkso\r\n\r\n");

        List<RequestSamples> interesting = new ArrayList<>();
        for (String body: testcase){
            RequestSamples reqSamp = new RequestSamples(this.api);
            if (reqSamp.sampleRequest(req.withMethod("POST").withRemovedHeader("content-length").withAddedHeader("transfer-encoding", "chunked").withBody(body))) interesting.add(reqSamp);
        }

        return interesting;
    }
}
