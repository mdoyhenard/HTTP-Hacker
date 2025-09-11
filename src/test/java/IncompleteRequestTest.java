import httpraider.model.network.HttpParserModel;
import httpraider.parser.ParserChainRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class IncompleteRequestTest {
    
    private HttpParserModel model;
    
    @BeforeEach
    void setUp() {
        model = new HttpParserModel();
    }
    
    @Test
    void testIncompleteRequestCompletedByNextData() {
        // First part: incomplete request (missing body bytes)
        String part1 = "POST /test HTTP/1.1\r\nHost: example.com\r\nContent-Length: 10\r\n\r\nhello"; // 5 bytes, needs 5 more
        
        // Second part: remaining body + new request
        String part2 = "world" + // Completes first request
                      "GET /next HTTP/1.1\r\nHost: example.com\r\n\r\n"; // New request
        
        // Parse first part alone - should get incomplete
        byte[] input1 = part1.getBytes(StandardCharsets.ISO_8859_1);
        List<byte[]> results1 = ParserChainRunner.parseRequestsForProxyRaw(model, input1);
        
        assertEquals(1, results1.size());
        String parsed1 = new String(results1.get(0), StandardCharsets.ISO_8859_1);
        assertTrue(parsed1.contains("<incomplete_request"));
        assertTrue(parsed1.contains("5 body bytes missing"));
        
        // Parse concatenated data - should complete the request
        byte[] combined = (part1 + part2).getBytes(StandardCharsets.ISO_8859_1);
        List<byte[]> results2 = ParserChainRunner.parseRequestsForProxyRaw(model, combined);
        
        assertEquals(2, results2.size());
        
        // First request should be complete now
        String completed = new String(results2.get(0), StandardCharsets.ISO_8859_1);
        assertTrue(completed.contains("helloworld"));
        assertFalse(completed.contains("<incomplete_request"));
        
        // Second request should also be parsed
        String second = new String(results2.get(1), StandardCharsets.ISO_8859_1);
        assertTrue(second.contains("GET /next"));
    }
    
    @Test
    void testMultipleIncompleteRequestsInPipeline() {
        // First incomplete request
        String req1 = "POST /first HTTP/1.1\r\nHost: example.com\r\nContent-Length: 8\r\n\r\ntest"; // 4 bytes, needs 4 more
        
        // Data that completes first and starts second incomplete
        String req2 = "data" + // Completes first request
                     "POST /second HTTP/1.1\r\nHost: example.com\r\nContent-Length: 6\r\n\r\nxyz"; // 3 bytes, needs 3 more
        
        // Data that completes second
        String req3 = "abc"; // Completes second request
        
        // Parse all concatenated
        byte[] allData = (req1 + req2 + req3).getBytes(StandardCharsets.ISO_8859_1);
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, allData);
        
        assertEquals(2, results.size());
        
        // First request should have "testdata"
        String first = new String(results.get(0), StandardCharsets.ISO_8859_1);
        assertTrue(first.contains("testdata"));
        assertFalse(first.contains("<incomplete_request"));
        
        // Second request should have "xyzabc"
        String second = new String(results.get(1), StandardCharsets.ISO_8859_1);
        assertTrue(second.contains("xyzabc"));
        assertFalse(second.contains("<incomplete_request"));
    }
    
    @Test
    void testIncompleteHeadersCompletedByNextData() {
        // First part: incomplete headers (no double CRLF)
        String part1 = "GET /test HTTP/1.1\r\nHost: example.com\r\nUser-Agent: test";
        
        // Second part: complete headers and start new request
        String part2 = "\r\n\r\n" + // Completes headers
                      "GET /next HTTP/1.1\r\nHost: example.com\r\n\r\n";
        
        // Parse first part alone - should get incomplete
        byte[] input1 = part1.getBytes(StandardCharsets.ISO_8859_1);
        List<byte[]> results1 = ParserChainRunner.parseRequestsForProxyRaw(model, input1);
        
        assertEquals(1, results1.size());
        String parsed1 = new String(results1.get(0), StandardCharsets.ISO_8859_1);
        assertTrue(parsed1.contains("<incomplete_request"));
        assertTrue(parsed1.contains("incomplete headers"));
        
        // Parse concatenated - should complete
        byte[] combined = (part1 + part2).getBytes(StandardCharsets.ISO_8859_1);
        List<byte[]> results2 = ParserChainRunner.parseRequestsForProxyRaw(model, combined);
        
        assertEquals(2, results2.size());
        
        // First request should be complete
        String completed = new String(results2.get(0), StandardCharsets.ISO_8859_1);
        assertTrue(completed.contains("User-Agent: test"));
        assertFalse(completed.contains("<incomplete_request"));
        
        // Second request should also be parsed
        String second = new String(results2.get(1), StandardCharsets.ISO_8859_1);
        assertTrue(second.contains("GET /next"));
    }
}