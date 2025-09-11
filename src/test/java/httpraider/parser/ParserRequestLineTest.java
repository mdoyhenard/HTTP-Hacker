package httpraider.parser;

import httpraider.model.network.HttpParserModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class ParserRequestLineTest {
    
    private HttpParserModel model;
    
    @BeforeEach
    void setUp() {
        model = new HttpParserModel();
    }
    
    // ===== METHOD REWRITING TESTS =====
    
    @Test
    void testMethodRewriting() {
        // Enable method rewriting from GET to POST
        model.setRewriteMethodEnabled(true);
        model.setFromMethod("GET");
        model.setToMethod("POST");
        
        String request = "GET /api/test HTTP/1.1\r\nHost: example.com\r\n\r\n";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(1, results.size());
        String parsed = new String(results.get(0), StandardCharsets.ISO_8859_1);
        assertTrue(parsed.startsWith("POST /api/test HTTP/1.1"));
        assertFalse(parsed.startsWith("GET"));
    }
    
    @Test
    void testMethodRewritingCaseSensitive() {
        model.setRewriteMethodEnabled(true);
        model.setFromMethod("get");  // lowercase
        model.setToMethod("POST");
        
        String request = "GET /api/test HTTP/1.1\r\nHost: example.com\r\n\r\n";  // uppercase
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(1, results.size());
        String parsed = new String(results.get(0), StandardCharsets.ISO_8859_1);
        // Should not rewrite due to case mismatch
        assertTrue(parsed.startsWith("GET /api/test"));
    }
    
    @Test
    void testMethodRewritingInPipeline() {
        model.setRewriteMethodEnabled(true);
        model.setFromMethod("GET");
        model.setToMethod("DELETE");
        
        String req1 = "GET /resource/1 HTTP/1.1\r\nHost: example.com\r\n\r\n";
        String req2 = "POST /resource/2 HTTP/1.1\r\nHost: example.com\r\nContent-Length: 0\r\n\r\n";
        String req3 = "GET /resource/3 HTTP/1.1\r\nHost: example.com\r\n\r\n";
        byte[] input = (req1 + req2 + req3).getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(3, results.size());
        
        // First and third should be rewritten
        assertTrue(new String(results.get(0), StandardCharsets.ISO_8859_1).startsWith("DELETE /resource/1"));
        assertTrue(new String(results.get(1), StandardCharsets.ISO_8859_1).startsWith("POST /resource/2"));
        assertTrue(new String(results.get(2), StandardCharsets.ISO_8859_1).startsWith("DELETE /resource/3"));
    }
    
    // ===== URL DECODING TESTS =====
    
    @Test
    void testUrlDecoding() {
        model.setDecodeUrlBeforeForwarding(true);
        model.setUrlDecodeFrom("%20");
        model.setUrlDecodeTo("%7E");
        
        String request = "GET /api/test%20space%2Fslash%3Fquery%3Dvalue HTTP/1.1\r\nHost: example.com\r\n\r\n";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(1, results.size());
        String parsed = new String(results.get(0), StandardCharsets.ISO_8859_1);
        // Should decode %20 (space), %2F (/), %3F (?), %3D (=)
        assertTrue(parsed.contains("/api/test space/slash?query=value"));
    }
    
    @Test
    void testPartialUrlDecoding() {
        model.setDecodeUrlBeforeForwarding(true);
        model.setUrlDecodeFrom("%30");  // '0'
        model.setUrlDecodeTo("%39");    // '9'
        
        String request = "GET /test%20%30%31%32%41%42 HTTP/1.1\r\nHost: example.com\r\n\r\n";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(1, results.size());
        String parsed = new String(results.get(0), StandardCharsets.ISO_8859_1);
        // Should only decode %30, %31, %32 (0-9 range), not %20 or %41, %42
        assertTrue(parsed.contains("/test%20012%41%42"));
    }
    
    // ===== HTTP VERSION FORCING TESTS =====
    
    @Test
    void testForceHttp10() {
        model.setForcedHttpVersion(HttpParserModel.ForcedHttpVersion.HTTP_1_0);
        
        String request = "GET /test HTTP/1.1\r\nHost: example.com\r\n\r\n";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(1, results.size());
        String parsed = new String(results.get(0), StandardCharsets.ISO_8859_1);
        assertTrue(parsed.contains("HTTP/1.0"));
        assertFalse(parsed.contains("HTTP/1.1"));
    }
    
    @Test
    void testForceHttp11() {
        model.setForcedHttpVersion(HttpParserModel.ForcedHttpVersion.HTTP_1_1);
        
        String request = "GET /test HTTP/1.0\r\nHost: example.com\r\n\r\n";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(1, results.size());
        String parsed = new String(results.get(0), StandardCharsets.ISO_8859_1);
        assertTrue(parsed.contains("HTTP/1.1"));
        assertFalse(parsed.contains("HTTP/1.0"));
    }
    
    @Test
    void testCustomHttpVersion() {
        model.setForcedHttpVersion(HttpParserModel.ForcedHttpVersion.AUTO);
        model.setCustomHttpVersion("HTTP/2.0");
        
        String request = "GET /test HTTP/1.1\r\nHost: example.com\r\n\r\n";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(1, results.size());
        String parsed = new String(results.get(0), StandardCharsets.ISO_8859_1);
        // When custom version is set, it should be used
        assertTrue(parsed.contains("HTTP/2.0"));
    }
    
    // ===== COMBINED REQUEST LINE MODIFICATIONS =====
    
    @Test
    void testAllRequestLineModifications() {
        // Enable all modifications
        model.setRewriteMethodEnabled(true);
        model.setFromMethod("GET");
        model.setToMethod("PUT");
        model.setDecodeUrlBeforeForwarding(true);
        model.setUrlDecodeFrom("%20");
        model.setUrlDecodeTo("%7E");
        model.setForcedHttpVersion(HttpParserModel.ForcedHttpVersion.HTTP_1_0);
        
        String request = "GET /api%2Ftest%20file HTTP/1.1\r\nHost: example.com\r\n\r\n";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(1, results.size());
        String parsed = new String(results.get(0), StandardCharsets.ISO_8859_1);
        // Should have: PUT method, decoded URL, HTTP/1.0
        assertTrue(parsed.startsWith("PUT /api/test file HTTP/1.0"));
    }
    
    // ===== REQUEST LINE WITH UNUSUAL FORMATS =====
    
    @Test
    void testRequestLineWithoutVersion() {
        String request = "GET /test\r\nHost: example.com\r\n\r\n";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(1, results.size());
        String parsed = new String(results.get(0), StandardCharsets.ISO_8859_1);
        // Parser should handle missing version
        assertTrue(parsed.contains("GET /test"));
    }
    
    @Test
    void testRequestLineWithExtraSpaces() {
        model.setRequestLineDelimiters(Arrays.asList(" "));
        
        String request = "GET     /test     HTTP/1.1\r\nHost: example.com\r\n\r\n";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(1, results.size());
        String parsed = new String(results.get(0), StandardCharsets.ISO_8859_1);
        // Should parse correctly despite extra spaces
        assertTrue(parsed.contains("/test"));
        assertTrue(parsed.contains("HTTP/1.1"));
    }
    
    @Test
    void testAbsoluteUriInRequestLine() {
        String request = "GET http://example.com/test HTTP/1.1\r\nHost: example.com\r\n\r\n";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(1, results.size());
        String parsed = new String(results.get(0), StandardCharsets.ISO_8859_1);
        assertTrue(parsed.contains("http://example.com/test"));
    }
    
    // ===== PROXY CHAIN WITH REQUEST LINE MODIFICATIONS =====
    
    @Test
    void testProxyChainWithDifferentMethodRewriting() {
        // Parser 1: Rewrite GET to POST
        HttpParserModel parser1 = new HttpParserModel();
        parser1.setRewriteMethodEnabled(true);
        parser1.setFromMethod("GET");
        parser1.setToMethod("POST");
        
        // Parser 2: Rewrite POST to PUT
        HttpParserModel parser2 = new HttpParserModel();
        parser2.setRewriteMethodEnabled(true);
        parser2.setFromMethod("POST");
        parser2.setToMethod("PUT");
        
        String request = "GET /test HTTP/1.1\r\nHost: example.com\r\n\r\n";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        // Through parser 1: GET -> POST
        List<byte[]> results1 = ParserChainRunner.parseRequestsForProxyRaw(parser1, input);
        assertEquals(1, results1.size());
        String p1_out = new String(results1.get(0), StandardCharsets.ISO_8859_1);
        assertTrue(p1_out.startsWith("POST"));
        
        // Through parser 2: POST -> PUT
        List<byte[]> results2 = ParserChainRunner.parseRequestsForProxyRaw(parser2, results1.get(0));
        assertEquals(1, results2.size());
        String p2_out = new String(results2.get(0), StandardCharsets.ISO_8859_1);
        assertTrue(p2_out.startsWith("PUT"));
    }
}