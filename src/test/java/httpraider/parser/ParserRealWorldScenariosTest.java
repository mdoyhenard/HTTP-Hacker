package httpraider.parser;

import httpraider.model.network.HttpParserModel;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class ParserRealWorldScenariosTest {
    
    // ===== REAL PROXY CONFIGURATIONS =====
    
    @Test
    void testNginxLikeProxy() {
        // Nginx-like configuration: adds X-Real-IP, X-Forwarded-For
        HttpParserModel nginx = new HttpParserModel();
        nginx.setAddHeaderRules(Arrays.asList(
            "X-Real-IP: 192.168.1.100",
            "X-Forwarded-For: 192.168.1.100",
            "X-Forwarded-Proto: https"
        ));
        
        String request = "GET /api/data HTTP/1.1\r\n" +
                        "Host: backend.internal\r\n" +
                        "User-Agent: Mozilla/5.0\r\n" +
                        "\r\n";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(nginx, input);
        
        assertEquals(1, results.size());
        String parsed = new String(results.get(0), StandardCharsets.ISO_8859_1);
        assertTrue(parsed.contains("X-Real-IP: 192.168.1.100"));
        assertTrue(parsed.contains("X-Forwarded-For: 192.168.1.100"));
        assertTrue(parsed.contains("X-Forwarded-Proto: https"));
    }
    
    @Test
    void testHAProxyLikeProxy() {
        // HAProxy-like configuration: strict parsing, adds PROXY protocol headers
        HttpParserModel haproxy = new HttpParserModel();
        haproxy.setAddHeaderRules(Arrays.asList(
            "X-Forwarded-For: 10.0.0.1",
            "X-HAProxy-Server-State: UP",
            "X-SSL-Client-Verify: SUCCESS"
        ));
        // HAProxy often normalizes to HTTP/1.1
        haproxy.setForcedHttpVersion(HttpParserModel.ForcedHttpVersion.HTTP_1_1);
        
        String request = "GET /health HTTP/1.0\r\n" +
                        "Host: app.example.com\r\n" +
                        "\r\n";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(haproxy, input);
        
        assertEquals(1, results.size());
        String parsed = new String(results.get(0), StandardCharsets.ISO_8859_1);
        assertTrue(parsed.contains("HTTP/1.1"));  // Forced version
        assertTrue(parsed.contains("X-HAProxy-Server-State: UP"));
    }
    
    @Test
    void testSquidProxyNormalization() {
        // Squid-like proxy: normalizes methods, handles Via headers
        HttpParserModel squid = new HttpParserModel();
        squid.setDeleteHeaderRules(Arrays.asList("Proxy-Connection"));
        squid.setAddHeaderRules(Arrays.asList(
            "Via: 1.1 squid.proxy.local",
            "X-Cache: MISS",
            "X-Cache-Lookup: MISS"
        ));
        // Squid might normalize unusual methods
        squid.setRewriteMethodEnabled(true);
        squid.setFromMethod("PROPFIND");
        squid.setToMethod("POST");
        
        String request = "PROPFIND /webdav/file.txt HTTP/1.1\r\n" +
                        "Host: storage.example.com\r\n" +
                        "Proxy-Connection: keep-alive\r\n" +
                        "\r\n";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(squid, input);
        
        assertEquals(1, results.size());
        String parsed = new String(results.get(0), StandardCharsets.ISO_8859_1);
        assertTrue(parsed.startsWith("POST"));  // Method rewritten
        assertFalse(parsed.contains("Proxy-Connection"));  // Deleted
        assertTrue(parsed.contains("Via: 1.1 squid.proxy.local"));
    }
    
    // ===== COMPLEX PROXY CHAIN SCENARIOS =====
    
    @Test
    void testThreeLayerProxyChain() {
        // Layer 1: WAF that blocks certain headers
        HttpParserModel waf = new HttpParserModel();
        waf.setDeleteHeaderRules(Arrays.asList("X-Attack", "X-Exploit", "X-SQL"));
        waf.setAddHeaderRules(Arrays.asList("X-WAF-Processed: true"));
        
        // Layer 2: Load balancer that adds routing info
        HttpParserModel loadBalancer = new HttpParserModel();
        loadBalancer.setAddHeaderRules(Arrays.asList(
            "X-Backend-Server: app-server-03",
            "X-Request-ID: " + UUID.randomUUID().toString()
        ));
        
        // Layer 3: Application proxy that normalizes encoding
        HttpParserModel appProxy = new HttpParserModel();
        appProxy.setOutputBodyEncoding(HttpParserModel.MessageLenBodyEncoding.FORCE_CL_HEADER);
        appProxy.setAddHeaderRules(Arrays.asList("X-App-Version: 2.0"));
        
        String request = "POST /api/process HTTP/1.1\r\n" +
                        "Host: api.example.com\r\n" +
                        "X-Attack: <script>alert(1)</script>\r\n" +
                        "Transfer-Encoding: chunked\r\n" +
                        "\r\n" +
                        "c\r\n{\"key\":\"val\"}\r\n0\r\n\r\n";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        // Through WAF
        List<byte[]> results1 = ParserChainRunner.parseRequestsForProxyRaw(waf, input);
        String afterWaf = new String(results1.get(0), StandardCharsets.ISO_8859_1);
        assertFalse(afterWaf.contains("X-Attack"));
        assertTrue(afterWaf.contains("X-WAF-Processed: true"));
        
        // Through Load Balancer
        List<byte[]> results2 = ParserChainRunner.parseRequestsForProxyRaw(loadBalancer, results1.get(0));
        String afterLB = new String(results2.get(0), StandardCharsets.ISO_8859_1);
        assertTrue(afterLB.contains("X-Backend-Server: app-server-03"));
        assertTrue(afterLB.contains("X-Request-ID:"));
        
        // Through App Proxy
        List<byte[]> results3 = ParserChainRunner.parseRequestsForProxyRaw(appProxy, results2.get(0));
        String afterApp = new String(results3.get(0), StandardCharsets.ISO_8859_1);
        assertTrue(afterApp.contains("Content-Length: 13"));  // Decoded chunked
        assertFalse(afterApp.contains("Transfer-Encoding"));
        assertTrue(afterApp.contains("X-App-Version: 2.0"));
        assertTrue(afterApp.contains("{\"key\":\"val\"}"));
    }
    
    @Test
    void testProxyChainWithFailingMiddleProxy() {
        // Proxy 1: Normal parsing
        HttpParserModel proxy1 = new HttpParserModel();
        proxy1.setAddHeaderRules(Arrays.asList("X-Proxy-1: passed"));
        
        // Proxy 2: Has incompatible line ending configuration
        HttpParserModel proxy2 = new HttpParserModel();
        proxy2.setHeaderLineEndings(Arrays.asList("\\r\\r\\r\\r"));  // Won't match standard requests
        
        // Proxy 3: Should not be reached if proxy 2 fails
        HttpParserModel proxy3 = new HttpParserModel();
        proxy3.setAddHeaderRules(Arrays.asList("X-Proxy-3: passed"));
        
        String request = "GET /test HTTP/1.1\r\nHost: example.com\r\n\r\n";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        // Through proxy 1 - should work
        List<byte[]> results1 = ParserChainRunner.parseRequestsForProxyRaw(proxy1, input);
        assertEquals(1, results1.size());
        assertTrue(new String(results1.get(0), StandardCharsets.ISO_8859_1).contains("X-Proxy-1: passed"));
        
        // Through proxy 2 - should fail to parse correctly
        List<byte[]> results2 = ParserChainRunner.parseRequestsForProxyRaw(proxy2, results1.get(0));
        assertEquals(1, results2.size());
        String afterProxy2 = new String(results2.get(0), StandardCharsets.ISO_8859_1);
        assertTrue(afterProxy2.contains("Header/body delimiter not found"));
    }
    
    // ===== SMUGGLING AND DESYNC SCENARIOS =====
    
    @Test
    void testCLTEDesyncThroughProxyChain() {
        // Proxy 1: Prioritizes Content-Length
        HttpParserModel proxy1 = new HttpParserModel();
        List<HttpParserModel.BodyLenHeaderRule> rules1 = new ArrayList<>();
        rules1.add(new HttpParserModel.BodyLenHeaderRule("Content-Length: ", false, HttpParserModel.DuplicateHandling.FIRST));
        rules1.add(new HttpParserModel.BodyLenHeaderRule("Transfer-Encoding: chunked", true, HttpParserModel.DuplicateHandling.LAST));
        proxy1.setBodyLenHeaderRules(rules1);
        
        // Proxy 2: Prioritizes Transfer-Encoding
        HttpParserModel proxy2 = new HttpParserModel();
        List<HttpParserModel.BodyLenHeaderRule> rules2 = new ArrayList<>();
        rules2.add(new HttpParserModel.BodyLenHeaderRule("Transfer-Encoding: chunked", true, HttpParserModel.DuplicateHandling.FIRST));
        rules2.add(new HttpParserModel.BodyLenHeaderRule("Content-Length: ", false, HttpParserModel.DuplicateHandling.LAST));
        proxy2.setBodyLenHeaderRules(rules2);
        
        // Ambiguous request with both headers
        String request = "POST /endpoint HTTP/1.1\r\n" +
                        "Host: vulnerable.com\r\n" +
                        "Content-Length: 6\r\n" +
                        "Transfer-Encoding: chunked\r\n" +
                        "\r\n" +
                        "0\r\n\r\nGET /admin HTTP/1.1\r\nHost: vulnerable.com\r\n\r\n";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        // Proxy 1 sees Content-Length: 6, takes "0\r\n\r\n" as body
        List<byte[]> results1 = ParserChainRunner.parseRequestsForProxyRaw(proxy1, input);
        assertTrue(results1.size() >= 1);
        String p1_req1 = new String(results1.get(0), StandardCharsets.ISO_8859_1);
        assertTrue(p1_req1.contains("POST /endpoint"));
        
        // If proxy1 sees the smuggled request
        if (results1.size() > 1) {
            String p1_req2 = new String(results1.get(1), StandardCharsets.ISO_8859_1);
            assertTrue(p1_req2.contains("GET /admin"));
        }
        
        // Proxy 2 would see chunked encoding and interpret differently
        List<byte[]> results2 = ParserChainRunner.parseRequestsForProxyRaw(proxy2, input);
        assertEquals(1, results2.size());  // Sees only one request due to chunked
    }
    
    // ===== ENCODING TRANSFORMATION CHAIN =====
    
    @Test
    void testEncodingTransformationChain() {
        // Start with Content-Length
        String originalRequest = "POST /data HTTP/1.1\r\n" +
                               "Host: example.com\r\n" +
                               "Content-Length: 15\r\n" +
                               "\r\n" +
                               "original-body-1";
        
        // Proxy 1: Convert to chunked
        HttpParserModel proxy1 = new HttpParserModel();
        proxy1.setOutputBodyEncoding(HttpParserModel.MessageLenBodyEncoding.FORCE_CHUNKED);
        
        List<byte[]> results1 = ParserChainRunner.parseRequestsForProxyRaw(proxy1, originalRequest.getBytes(StandardCharsets.ISO_8859_1));
        String after1 = new String(results1.get(0), StandardCharsets.ISO_8859_1);
        assertTrue(after1.contains("Transfer-Encoding: chunked"));
        assertFalse(after1.contains("Content-Length"));
        assertTrue(after1.contains("f\r\noriginal-body-1\r\n0\r\n"));
        
        // Proxy 2: Keep chunked but add headers
        HttpParserModel proxy2 = new HttpParserModel();
        proxy2.setAddHeaderRules(Arrays.asList("X-Processed-By: Proxy2"));
        
        List<byte[]> results2 = ParserChainRunner.parseRequestsForProxyRaw(proxy2, results1.get(0));
        String after2 = new String(results2.get(0), StandardCharsets.ISO_8859_1);
        assertTrue(after2.contains("Transfer-Encoding: chunked"));
        assertTrue(after2.contains("X-Processed-By: Proxy2"));
        
        // Proxy 3: Convert back to Content-Length
        HttpParserModel proxy3 = new HttpParserModel();
        proxy3.setOutputBodyEncoding(HttpParserModel.MessageLenBodyEncoding.FORCE_CL_HEADER);
        
        List<byte[]> results3 = ParserChainRunner.parseRequestsForProxyRaw(proxy3, results2.get(0));
        String after3 = new String(results3.get(0), StandardCharsets.ISO_8859_1);
        assertTrue(after3.contains("Content-Length: 15"));
        assertFalse(after3.contains("Transfer-Encoding"));
        assertTrue(after3.endsWith("original-body-1"));
    }
    
    // ===== PIPELINE PRESERVATION THROUGH PROXY CHAIN =====
    
    @Test
    void testPipelinePreservationThroughChain() {
        // Three pipelined requests
        String req1 = "GET /first HTTP/1.1\r\nHost: example.com\r\n\r\n";
        String req2 = "POST /second HTTP/1.1\r\nHost: example.com\r\nContent-Length: 4\r\n\r\ndata";
        String req3 = "GET /third HTTP/1.1\r\nHost: example.com\r\n\r\n";
        byte[] pipeline = (req1 + req2 + req3).getBytes(StandardCharsets.ISO_8859_1);
        
        // Proxy 1: Add tracking headers
        HttpParserModel proxy1 = new HttpParserModel();
        proxy1.setAddHeaderRules(Arrays.asList("X-Proxy-Hop: 1"));
        
        // Proxy 2: Different line endings
        HttpParserModel proxy2 = new HttpParserModel();
        proxy2.setHeaderLineEndings(Arrays.asList("\\n"));
        proxy2.setAddHeaderRules(Arrays.asList("X-Proxy-Hop: 2"));
        
        // Through proxy 1
        List<byte[]> results1 = ParserChainRunner.parseRequestsForProxyRaw(proxy1, pipeline);
        assertEquals(3, results1.size());
        
        // Concatenate results to maintain pipeline
        ByteArrayOutputStream combined = new ByteArrayOutputStream();
        for (byte[] result : results1) {
            combined.write(result, 0, result.length);
        }
        
        // Through proxy 2
        List<byte[]> results2 = ParserChainRunner.parseRequestsForProxyRaw(proxy2, combined.toByteArray());
        assertEquals(3, results2.size());
        
        // Verify all requests made it through with modifications
        for (int i = 0; i < 3; i++) {
            String parsed = new String(results2.get(i), StandardCharsets.ISO_8859_1);
            assertTrue(parsed.contains("X-Proxy-Hop: 1"));
            assertTrue(parsed.contains("X-Proxy-Hop: 2"));
        }
    }
    
    private static class ByteArrayOutputStream extends java.io.ByteArrayOutputStream {
        // Helper for combining byte arrays
    }
}