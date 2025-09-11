package httpraider.parser;

import httpraider.model.network.HttpParserModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class ParserEdgeCaseTest {
    
    private HttpParserModel model;
    
    @BeforeEach
    void setUp() {
        model = new HttpParserModel();
    }
    
    // ===== UNUSUAL LINE ENDING COMBINATIONS =====
    
    @Test
    void testMixedLineEndingsInSameRequest() {
        // Configure parser to accept multiple line endings
        model.setHeaderLineEndings(Arrays.asList("\\r\\n", "\\n", "\\r"));
        
        String request = "GET /test HTTP/1.1\nHost: example.com\r\nContent-Length: 4\r\n\rtest";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(1, results.size());
        String parsed = new String(results.get(0), StandardCharsets.ISO_8859_1);
        assertTrue(parsed.contains("test"));
    }
    
    @Test
    void testTabAndNewlineAsHeaderEnding() {
        model.setHeaderLineEndings(Arrays.asList("\\t\\n"));
        
        String request = "GET /test HTTP/1.1\t\nHost: example.com\t\nContent-Length: 5\t\n\t\nhello";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(1, results.size());
        String parsed = new String(results.get(0), StandardCharsets.ISO_8859_1);
        assertTrue(parsed.endsWith("hello"));
    }
    
    // ===== REQUEST LINE DELIMITER VARIATIONS =====
    
    @Test
    void testMultipleSpacesAsDelimiter() {
        model.setRequestLineDelimiters(Arrays.asList("   ")); // Three spaces
        
        String request = "GET   /test   HTTP/1.1\r\nHost: example.com\r\n\r\n";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(1, results.size());
        String parsed = new String(results.get(0), StandardCharsets.ISO_8859_1);
        assertTrue(parsed.startsWith("GET   /test   HTTP/1.1"));
    }
    
    @Test
    void testPipeCharacterAsDelimiter() {
        model.setRequestLineDelimiters(Arrays.asList("|"));
        
        String request = "POST|/api/test|HTTP/1.1\r\nHost: example.com\r\nContent-Length: 7\r\n\r\npayload";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(1, results.size());
        String parsed = new String(results.get(0), StandardCharsets.ISO_8859_1);
        assertTrue(parsed.contains("POST|/api/test|HTTP/1.1"));
        assertTrue(parsed.endsWith("payload"));
    }
    
    // ===== CUSTOM MESSAGE LENGTH HEADERS =====
    
    @Test
    void testMultipleCustomLengthHeaders() {
        // Configure multiple custom headers with priority
        List<HttpParserModel.BodyLenHeaderRule> rules = new ArrayList<>();
        rules.add(new HttpParserModel.BodyLenHeaderRule("X-Priority-Length: ", false, HttpParserModel.DuplicateHandling.FIRST));
        rules.add(new HttpParserModel.BodyLenHeaderRule("Body-Size: ", false, HttpParserModel.DuplicateHandling.FIRST));
        rules.add(new HttpParserModel.BodyLenHeaderRule("Content-Length: ", false, HttpParserModel.DuplicateHandling.FIRST));
        model.setBodyLenHeaderRules(rules);
        
        // Request with multiple length headers - X-Priority-Length should take precedence
        String request = "POST /test HTTP/1.1\r\n" +
                        "Host: example.com\r\n" +
                        "Content-Length: 10\r\n" +
                        "Body-Size: 8\r\n" +
                        "X-Priority-Length: 6\r\n" +
                        "\r\n" +
                        "123456789012345"; // Extra data to test correct length parsing
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(1, results.size());
        String parsed = new String(results.get(0), StandardCharsets.ISO_8859_1);
        assertTrue(parsed.endsWith("123456")); // Should only take 6 bytes
        assertFalse(parsed.endsWith("123456789012345"));
    }
    
    @Test
    void testCustomChunkedHeader() {
        List<HttpParserModel.BodyLenHeaderRule> rules = new ArrayList<>();
        rules.add(new HttpParserModel.BodyLenHeaderRule("X-Chunked: yes", true, HttpParserModel.DuplicateHandling.FIRST));
        model.setBodyLenHeaderRules(rules);
        
        String request = "POST /test HTTP/1.1\r\nHost: example.com\r\nX-Chunked: yes\r\n\r\n" +
                        "3\r\nabc\r\n2\r\nde\r\n0\r\n\r\n";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(1, results.size());
        String parsed = new String(results.get(0), StandardCharsets.ISO_8859_1);
        assertTrue(parsed.contains("abcde"));
    }
    
    // ===== CHUNKED ENCODING EDGE CASES =====
    
    @Test
    void testChunkedWithCustomLineEndings() {
        model.setChunkedLineEndings(Arrays.asList("\\n"));
        
        String request = "POST /test HTTP/1.1\r\nHost: example.com\r\nTransfer-Encoding: chunked\r\n\r\n" +
                        "5\nhello\n3\nbye\n0\n\n";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(1, results.size());
        String parsed = new String(results.get(0), StandardCharsets.ISO_8859_1);
        assertTrue(parsed.contains("hellobye"));
    }
    
    @Test
    void testChunkedWithTrailerHeaders() {
        String request = "POST /test HTTP/1.1\r\nHost: example.com\r\nTransfer-Encoding: chunked\r\n\r\n" +
                        "5\r\nhello\r\n0\r\n" +
                        "X-Trailer: value\r\n" +
                        "Another-Trailer: data\r\n" +
                        "\r\n";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(1, results.size());
        String parsed = new String(results.get(0), StandardCharsets.ISO_8859_1);
        assertTrue(parsed.contains("hello"));
    }
    
    // ===== HEADER FOLDING TESTS =====
    
    @Test
    void testComplexHeaderFoldingWithTabs() {
        model.setAllowHeaderFolding(true);
        
        String request = "GET /test HTTP/1.1\r\n" +
                        "Host: example.com\r\n" +
                        "X-Complex: first line\r\n" +
                        "\tsecond line\r\n" +
                        " third line\r\n" +
                        "\t\tfourth line\r\n" +
                        "Content-Length: 0\r\n\r\n";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(1, results.size());
        String parsed = new String(results.get(0), StandardCharsets.ISO_8859_1);
        assertTrue(parsed.contains("X-Complex: first line second line third line fourth line"));
    }
    
    @Test
    void testHeaderFoldingDisabledBreaksParsing() {
        model.setAllowHeaderFolding(false);
        
        String request = "GET /test HTTP/1.1\r\n" +
                        "Host: example.com\r\n" +
                        "X-Folded: part1\r\n" +
                        " part2\r\n" +
                        "Content-Length: 4\r\n\r\ntest";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        // With folding disabled, the folded line might be treated as a separate header
        assertEquals(1, results.size());
        String parsed = new String(results.get(0), StandardCharsets.ISO_8859_1);
        // Should still parse but folding won't be applied
        assertTrue(parsed.contains("test"));
    }
    
    // ===== BODY ENCODING OUTPUT TESTS =====
    
    @Test
    void testForceChunkedOutput() {
        model.setOutputBodyEncoding(HttpParserModel.MessageLenBodyEncoding.FORCE_CHUNKED);
        
        String request = "POST /test HTTP/1.1\r\nHost: example.com\r\nContent-Length: 11\r\n\r\nhello world";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(1, results.size());
        String parsed = new String(results.get(0), StandardCharsets.ISO_8859_1);
        assertTrue(parsed.contains("Transfer-Encoding: chunked"));
        assertFalse(parsed.contains("Content-Length"));
        // Body should be chunked encoded
        assertTrue(parsed.contains("\r\nb\r\nhello world\r\n0\r\n"));
    }
    
    @Test
    void testForceContentLengthOutput() {
        model.setOutputBodyEncoding(HttpParserModel.MessageLenBodyEncoding.FORCE_CL_HEADER);
        
        String request = "POST /test HTTP/1.1\r\nHost: example.com\r\nTransfer-Encoding: chunked\r\n\r\n" +
                        "5\r\nhello\r\n0\r\n\r\n";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(1, results.size());
        String parsed = new String(results.get(0), StandardCharsets.ISO_8859_1);
        assertTrue(parsed.contains("Content-Length: 5"));
        assertFalse(parsed.contains("Transfer-Encoding"));
        assertTrue(parsed.endsWith("hello"));
    }
    
    // ===== COMPLEX PIPELINE SCENARIOS =====
    
    @Test
    void testPipelineWithIncompleteLastRequest() {
        String req1 = "GET /complete HTTP/1.1\r\nHost: example.com\r\n\r\n";
        String req2 = "POST /incomplete HTTP/1.1\r\nHost: example.com\r\nContent-Length: 10\r\n\r\nhello"; // Only 5 bytes
        byte[] input = (req1 + req2).getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(2, results.size());
        
        // First request should be complete
        String parsed1 = new String(results.get(0), StandardCharsets.ISO_8859_1);
        assertTrue(parsed1.contains("GET /complete"));
        
        // Second request should have incomplete tag
        String parsed2 = new String(results.get(1), StandardCharsets.ISO_8859_1);
        assertTrue(parsed2.contains("<incomplete_request"));
        assertTrue(parsed2.contains("missing 5 bytes"));
    }
    
    @Test
    void testZeroContentLengthWithPipeline() {
        String req1 = "POST /first HTTP/1.1\r\nHost: example.com\r\nContent-Length: 0\r\n\r\n";
        String req2 = "POST /second HTTP/1.1\r\nHost: example.com\r\nContent-Length: 4\r\n\r\ndata";
        byte[] input = (req1 + req2).getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(2, results.size());
        
        String parsed1 = new String(results.get(0), StandardCharsets.ISO_8859_1);
        assertTrue(parsed1.contains("Content-Length: 0"));
        assertFalse(parsed1.endsWith("data")); // Should not include next request's data
        
        String parsed2 = new String(results.get(1), StandardCharsets.ISO_8859_1);
        assertTrue(parsed2.endsWith("data"));
    }
    
    // ===== DUPLICATE HEADER HANDLING =====
    
    @Test
    void testDuplicateContentLengthFirst() {
        // Default is FIRST
        String request = "POST /test HTTP/1.1\r\n" +
                        "Host: example.com\r\n" +
                        "Content-Length: 5\r\n" +
                        "Content-Length: 10\r\n" +
                        "\r\n" +
                        "12345678901234";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(1, results.size());
        String parsed = new String(results.get(0), StandardCharsets.ISO_8859_1);
        assertTrue(parsed.endsWith("12345")); // Should use first value (5)
    }
    
    @Test
    void testDuplicateContentLengthLast() {
        // Configure to use LAST
        List<HttpParserModel.BodyLenHeaderRule> rules = new ArrayList<>();
        rules.add(new HttpParserModel.BodyLenHeaderRule("Content-Length: ", false, HttpParserModel.DuplicateHandling.LAST));
        model.setBodyLenHeaderRules(rules);
        
        String request = "POST /test HTTP/1.1\r\n" +
                        "Host: example.com\r\n" +
                        "Content-Length: 5\r\n" +
                        "Content-Length: 3\r\n" +
                        "\r\n" +
                        "12345678901234";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(1, results.size());
        String parsed = new String(results.get(0), StandardCharsets.ISO_8859_1);
        assertTrue(parsed.endsWith("123")); // Should use last value (3)
    }
}