package httpraider.parser;

import httpraider.model.network.HttpParserModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class ParserHeaderManipulationTest {
    
    private HttpParserModel model;
    
    @BeforeEach
    void setUp() {
        model = new HttpParserModel();
    }
    
    // ===== HEADER DELETION TESTS =====
    
    @Test
    void testDeleteSingleHeader() {
        model.setDeleteHeaderRules(Arrays.asList("X-Delete-Me"));
        
        String request = "GET /test HTTP/1.1\r\n" +
                        "Host: example.com\r\n" +
                        "X-Delete-Me: value\r\n" +
                        "X-Keep-Me: value\r\n" +
                        "\r\n";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(1, results.size());
        String parsed = new String(results.get(0), StandardCharsets.ISO_8859_1);
        assertFalse(parsed.contains("X-Delete-Me"));
        assertTrue(parsed.contains("X-Keep-Me"));
    }
    
    @Test
    void testDeleteMultipleHeaders() {
        model.setDeleteHeaderRules(Arrays.asList("X-Delete-1", "X-Delete-2", "Authorization"));
        
        String request = "GET /test HTTP/1.1\r\n" +
                        "Host: example.com\r\n" +
                        "X-Delete-1: value1\r\n" +
                        "X-Keep: value\r\n" +
                        "X-Delete-2: value2\r\n" +
                        "Authorization: Bearer token\r\n" +
                        "\r\n";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(1, results.size());
        String parsed = new String(results.get(0), StandardCharsets.ISO_8859_1);
        assertFalse(parsed.contains("X-Delete-1"));
        assertFalse(parsed.contains("X-Delete-2"));
        assertFalse(parsed.contains("Authorization"));
        assertTrue(parsed.contains("Host: example.com"));
        assertTrue(parsed.contains("X-Keep: value"));
    }
    
    @Test
    void testDeleteHeaderCaseInsensitive() {
        model.setDeleteHeaderRules(Arrays.asList("x-delete-me"));  // lowercase
        
        String request = "GET /test HTTP/1.1\r\n" +
                        "Host: example.com\r\n" +
                        "X-Delete-Me: value\r\n" +  // Different case
                        "\r\n";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(1, results.size());
        String parsed = new String(results.get(0), StandardCharsets.ISO_8859_1);
        // Case-sensitive deletion - should NOT delete when case doesn't match
        assertTrue(parsed.contains("X-Delete-Me"));
    }
    
    // ===== HEADER ADDITION TESTS =====
    
    @Test
    void testAddSingleHeader() {
        model.setAddHeaderRules(Arrays.asList("X-Custom-Header: custom-value"));
        
        String request = "GET /test HTTP/1.1\r\nHost: example.com\r\n\r\n";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(1, results.size());
        String parsed = new String(results.get(0), StandardCharsets.ISO_8859_1);
        assertTrue(parsed.contains("X-Custom-Header: custom-value"));
    }
    
    @Test
    void testAddMultipleHeaders() {
        model.setAddHeaderRules(Arrays.asList(
            "X-Forwarded-For: 10.0.0.1",
            "X-Real-IP: 192.168.1.1",
            "X-Custom-Auth: token123"
        ));
        
        String request = "GET /test HTTP/1.1\r\nHost: example.com\r\n\r\n";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(1, results.size());
        String parsed = new String(results.get(0), StandardCharsets.ISO_8859_1);
        assertTrue(parsed.contains("X-Forwarded-For: 10.0.0.1"));
        assertTrue(parsed.contains("X-Real-IP: 192.168.1.1"));
        assertTrue(parsed.contains("X-Custom-Auth: token123"));
    }
    
    @Test
    void testAddHeaderWithExistingHeader() {
        model.setAddHeaderRules(Arrays.asList("Host: override.com"));
        
        String request = "GET /test HTTP/1.1\r\nHost: example.com\r\n\r\n";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(1, results.size());
        String parsed = new String(results.get(0), StandardCharsets.ISO_8859_1);
        // Should have both headers (original and added)
        assertTrue(parsed.contains("Host: example.com"));
        assertTrue(parsed.contains("Host: override.com"));
    }
    
    // ===== COMBINED DELETE AND ADD TESTS =====
    
    @Test
    void testDeleteAndAddHeaders() {
        model.setDeleteHeaderRules(Arrays.asList("User-Agent", "Accept"));
        model.setAddHeaderRules(Arrays.asList(
            "User-Agent: CustomBot/1.0",
            "X-Request-ID: 12345"
        ));
        
        String request = "GET /test HTTP/1.1\r\n" +
                        "Host: example.com\r\n" +
                        "User-Agent: Mozilla/5.0\r\n" +
                        "Accept: */*\r\n" +
                        "\r\n";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(1, results.size());
        String parsed = new String(results.get(0), StandardCharsets.ISO_8859_1);
        // Original headers should be deleted
        assertFalse(parsed.contains("Mozilla/5.0"));
        assertFalse(parsed.contains("Accept: */*"));
        // New headers should be added
        assertTrue(parsed.contains("User-Agent: CustomBot/1.0"));
        assertTrue(parsed.contains("X-Request-ID: 12345"));
    }
    
    @Test
    void testHeaderManipulationInPipeline() {
        model.setDeleteHeaderRules(Arrays.asList("X-Delete"));
        model.setAddHeaderRules(Arrays.asList("X-Added: value"));
        
        String req1 = "GET /first HTTP/1.1\r\nHost: example.com\r\nX-Delete: remove\r\n\r\n";
        String req2 = "GET /second HTTP/1.1\r\nHost: example.com\r\n\r\n";
        byte[] input = (req1 + req2).getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(2, results.size());
        
        // Both requests should have manipulations applied
        String parsed1 = new String(results.get(0), StandardCharsets.ISO_8859_1);
        assertFalse(parsed1.contains("X-Delete"));
        assertTrue(parsed1.contains("X-Added: value"));
        
        String parsed2 = new String(results.get(1), StandardCharsets.ISO_8859_1);
        assertTrue(parsed2.contains("X-Added: value"));
    }
    
    // ===== PROXY CHAIN HEADER MANIPULATION =====
    
    @Test
    void testProxyChainHeaderManipulation() {
        // Parser 1: Add forwarding headers
        HttpParserModel parser1 = new HttpParserModel();
        parser1.setAddHeaderRules(Arrays.asList(
            "X-Forwarded-By: Proxy1",
            "X-Proxy-Timestamp: 2024-01-01"
        ));
        
        // Parser 2: Delete sensitive headers, add new ones
        HttpParserModel parser2 = new HttpParserModel();
        parser2.setDeleteHeaderRules(Arrays.asList("X-Proxy-Timestamp"));
        parser2.setAddHeaderRules(Arrays.asList("X-Forwarded-By: Proxy2"));
        
        String request = "GET /test HTTP/1.1\r\nHost: example.com\r\n\r\n";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        // Through parser 1
        List<byte[]> results1 = ParserChainRunner.parseRequestsForProxyRaw(parser1, input);
        assertEquals(1, results1.size());
        String p1_out = new String(results1.get(0), StandardCharsets.ISO_8859_1);
        assertTrue(p1_out.contains("X-Forwarded-By: Proxy1"));
        assertTrue(p1_out.contains("X-Proxy-Timestamp: 2024-01-01"));
        
        // Through parser 2
        List<byte[]> results2 = ParserChainRunner.parseRequestsForProxyRaw(parser2, results1.get(0));
        assertEquals(1, results2.size());
        String p2_out = new String(results2.get(0), StandardCharsets.ISO_8859_1);
        // Should have both forwarded-by headers
        assertTrue(p2_out.contains("X-Forwarded-By: Proxy1"));
        assertTrue(p2_out.contains("X-Forwarded-By: Proxy2"));
        // Timestamp should be deleted
        assertFalse(p2_out.contains("X-Proxy-Timestamp"));
    }
    
    // ===== HEADER MANIPULATION WITH BODY ENCODING =====
    
    @Test
    void testHeaderManipulationWithChunkedConversion() {
        // Delete Transfer-Encoding, add custom headers, force Content-Length
        model.setDeleteHeaderRules(Arrays.asList("Transfer-Encoding"));
        model.setAddHeaderRules(Arrays.asList("X-Processed: true"));
        model.setOutputBodyEncoding(HttpParserModel.MessageLenBodyEncoding.FORCE_CL_HEADER);
        
        String request = "POST /test HTTP/1.1\r\n" +
                        "Host: example.com\r\n" +
                        "Transfer-Encoding: chunked\r\n" +
                        "\r\n" +
                        "5\r\nhello\r\n0\r\n\r\n";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(1, results.size());
        String parsed = new String(results.get(0), StandardCharsets.ISO_8859_1);
        // Should have Content-Length instead of Transfer-Encoding
        assertTrue(parsed.contains("Content-Length: 5"));
        assertFalse(parsed.contains("Transfer-Encoding"));
        // Custom header should be added
        assertTrue(parsed.contains("X-Processed: true"));
        // Body should be decoded
        assertTrue(parsed.endsWith("hello"));
    }
    
    // ===== SPECIAL HEADER CASES =====
    
    @Test
    void testHeaderWithColonInValue() {
        model.setAddHeaderRules(Arrays.asList("X-URL: https://example.com:8080/path"));
        
        String request = "GET /test HTTP/1.1\r\nHost: example.com\r\n\r\n";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(1, results.size());
        String parsed = new String(results.get(0), StandardCharsets.ISO_8859_1);
        // Should handle colon in header value correctly
        assertTrue(parsed.contains("X-URL: https://example.com:8080/path"));
    }
    
    @Test
    void testEmptyHeaderValue() {
        model.setAddHeaderRules(Arrays.asList("X-Empty:", "X-Space: "));
        
        String request = "GET /test HTTP/1.1\r\nHost: example.com\r\n\r\n";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(1, results.size());
        String parsed = new String(results.get(0), StandardCharsets.ISO_8859_1);
        assertTrue(parsed.contains("X-Empty:"));
        assertTrue(parsed.contains("X-Space: "));
    }
}