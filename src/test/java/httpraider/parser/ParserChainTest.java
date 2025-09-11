package httpraider.parser;

import httpraider.model.network.HttpParserModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class ParserChainTest {
    
    private HttpParserModel model;
    
    @BeforeEach
    void setUp() {
        model = new HttpParserModel();
    }
    
    // ===== BASIC PARSING TESTS =====
    
    @Test
    void testSingleGetRequest() {
        String request = "GET /test HTTP/1.1\r\nHost: example.com\r\nContent-Length: 4\r\n\r\ntest";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(1, results.size());
        String parsed = new String(results.get(0), StandardCharsets.ISO_8859_1);
        assertEquals(request, parsed);
    }
    
    @Test
    void testPipelinedRequests() {
        String req1 = "POST /first HTTP/1.1\r\nHost: example.com\r\nContent-Length: 5\r\n\r\nhello";
        String req2 = "POST /second HTTP/1.1\r\nHost: example.com\r\nContent-Length: 3\r\n\r\nbye";
        byte[] input = (req1 + req2).getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(2, results.size());
        assertEquals(req1, new String(results.get(0), StandardCharsets.ISO_8859_1));
        assertEquals(req2, new String(results.get(1), StandardCharsets.ISO_8859_1));
    }
    
    @Test
    void testLFOnlyLineEndings() {
        // Configure parser for LF only
        model.setHeaderLineEndings(Arrays.asList("\\n"));
        
        String request = "GET /test HTTP/1.1\nHost: example.com\nContent-Length: 4\n\ntest";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(1, results.size());
        assertEquals(request, new String(results.get(0), StandardCharsets.ISO_8859_1));
    }
    
    @Test
    void testChunkedEncoding() {
        String request = "POST /test HTTP/1.1\r\nHost: example.com\r\nTransfer-Encoding: chunked\r\n\r\n" +
                        "5\r\nhello\r\n3\r\nbye\r\n0\r\n\r\n";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(1, results.size());
        String parsed = new String(results.get(0), StandardCharsets.ISO_8859_1);
        // Should have decoded body "hellobye"
        assertTrue(parsed.contains("hellobye"));
    }
    
    // ===== EDGE CASE PARSER CONFIGURATIONS =====
    
    @Test
    void testCustomHeaderDelimiterDoubleCarriageReturn() {
        // Configure parser with \r\r as line ending
        model.setHeaderLineEndings(Arrays.asList("\\r\\r"));
        
        String request = "GET /test HTTP/1.1\r\rHost: example.com\r\rContent-Length: 4\r\r\r\rtest";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(1, results.size());
        String parsed = new String(results.get(0), StandardCharsets.ISO_8859_1);
        assertTrue(parsed.contains("test"));
    }
    
    @Test
    void testTabAsRequestLineDelimiter() {
        // Configure parser with tab as request line delimiter
        model.setRequestLineDelimiters(Arrays.asList("\t"));
        
        String request = "GET\t/test\tHTTP/1.1\r\nHost: example.com\r\nContent-Length: 4\r\n\r\ntest";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(1, results.size());
        String parsed = new String(results.get(0), StandardCharsets.ISO_8859_1);
        // Verify request line parts were parsed correctly
        assertTrue(parsed.startsWith("GET\t/test\tHTTP/1.1"));
    }
    
    @Test
    void testCustomContentLengthHeader() {
        // Configure parser with custom body length header
        List<HttpParserModel.BodyLenHeaderRule> rules = new ArrayList<>();
        rules.add(new HttpParserModel.BodyLenHeaderRule("X-Body-Size: ", false, HttpParserModel.DuplicateHandling.FIRST));
        model.setBodyLenHeaderRules(rules);
        
        String request = "POST /test HTTP/1.1\r\nHost: example.com\r\nX-Body-Size: 10\r\n\r\n0123456789";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(1, results.size());
        String parsed = new String(results.get(0), StandardCharsets.ISO_8859_1);
        assertTrue(parsed.endsWith("0123456789"));
    }
    
    @Test
    void testHeaderFolding() {
        // Enable header folding
        model.setAllowHeaderFolding(true);
        
        String request = "GET /test HTTP/1.1\r\nHost: example.com\r\nX-Long-Header: part1\r\n part2\r\n part3\r\nContent-Length: 0\r\n\r\n";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(1, results.size());
        String parsed = new String(results.get(0), StandardCharsets.ISO_8859_1);
        // Header folding should preserve the folded header
        assertTrue(parsed.contains("X-Long-Header: part1 part2 part3"));
    }
    
    // ===== PROXY CHAIN TESTS =====
    
    @Test
    void testProxyChainChunkedToContentLength() {
        // First parser: accepts chunked, outputs Content-Length
        HttpParserModel parser1 = new HttpParserModel();
        parser1.setOutputBodyEncoding(HttpParserModel.MessageLenBodyEncoding.FORCE_CL_HEADER);
        
        // Second parser: standard configuration
        HttpParserModel parser2 = new HttpParserModel();
        
        String request = "POST /test HTTP/1.1\r\nHost: example.com\r\nTransfer-Encoding: chunked\r\n\r\n" +
                        "5\r\nhello\r\n0\r\n\r\n";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        // Parse through first proxy
        List<byte[]> results1 = ParserChainRunner.parseRequestsForProxyRaw(parser1, input);
        assertEquals(1, results1.size());
        String intermediate = new String(results1.get(0), StandardCharsets.ISO_8859_1);
        assertTrue(intermediate.contains("Content-Length: 5"));
        assertTrue(intermediate.contains("hello"));
        assertFalse(intermediate.contains("Transfer-Encoding"));
        
        // Parse through second proxy
        List<byte[]> results2 = ParserChainRunner.parseRequestsForProxyRaw(parser2, results1.get(0));
        assertEquals(1, results2.size());
        String final_result = new String(results2.get(0), StandardCharsets.ISO_8859_1);
        assertTrue(final_result.contains("hello"));
    }
    
    @Test
    void testProxyChainCustomToStandardLineEndings() {
        // First parser: custom line endings
        HttpParserModel parser1 = new HttpParserModel();
        parser1.setHeaderLineEndings(Arrays.asList("\\r\\r"));
        
        // Second parser: standard line endings
        HttpParserModel parser2 = new HttpParserModel();
        
        String request = "GET /test HTTP/1.1\r\rHost: example.com\r\rContent-Length: 4\r\r\r\rtest";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        // Parse through first proxy
        List<byte[]> results1 = ParserChainRunner.parseRequestsForProxyRaw(parser1, input);
        assertEquals(1, results1.size());
        
        // Output should have standard line endings
        String intermediate = new String(results1.get(0), StandardCharsets.ISO_8859_1);
        assertTrue(intermediate.contains("\r\n"));
        
        // Parse through second proxy
        List<byte[]> results2 = ParserChainRunner.parseRequestsForProxyRaw(parser2, results1.get(0));
        assertEquals(1, results2.size());
        assertTrue(new String(results2.get(0), StandardCharsets.ISO_8859_1).contains("test"));
    }
    
    // ===== ERROR HANDLING TESTS =====
    
    @Test
    void testInvalidContentLength() {
        String request = "POST /test HTTP/1.1\r\nHost: example.com\r\nContent-Length: abc\r\n\r\ntest";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(1, results.size());
        String parsed = new String(results.get(0), StandardCharsets.ISO_8859_1);
        assertTrue(parsed.contains("Invalid Content-Length value"));
    }
    
    @Test
    void testIncompleteChunkedEncoding() {
        String request = "POST /test HTTP/1.1\r\nHost: example.com\r\nTransfer-Encoding: chunked\r\n\r\n" +
                        "5\r\nhello\r\n";  // Missing final 0 chunk
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(1, results.size());
        String parsed = new String(results.get(0), StandardCharsets.ISO_8859_1);
        assertTrue(parsed.contains("<incomplete_request"));
    }
    
    @Test
    void testMissingHeaderBodyDelimiter() {
        // Configure with a delimiter that doesn't exist in the request
        model.setHeaderLineEndings(Arrays.asList("\\r\\n\\r\\n"));
        
        String request = "GET /test HTTP/1.1\r\nHost: example.com\r\n";  // No double CRLF
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(1, results.size());
        String parsed = new String(results.get(0), StandardCharsets.ISO_8859_1);
        assertTrue(parsed.contains("Header/body delimiter not found"));
    }
    
    // ===== COMPLEX SCENARIOS =====
    
    @Test
    void testMixedEncodingsInPipeline() {
        String req1 = "POST /first HTTP/1.1\r\nHost: example.com\r\nContent-Length: 5\r\n\r\nfirst";
        String req2 = "POST /second HTTP/1.1\r\nHost: example.com\r\nTransfer-Encoding: chunked\r\n\r\n" +
                     "6\r\nsecond\r\n0\r\n\r\n";
        byte[] input = (req1 + req2).getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(2, results.size());
        
        String parsed1 = new String(results.get(0), StandardCharsets.ISO_8859_1);
        assertTrue(parsed1.contains("first"));
        assertTrue(parsed1.contains("Content-Length: 5"));
        
        String parsed2 = new String(results.get(1), StandardCharsets.ISO_8859_1);
        assertTrue(parsed2.contains("second"));
    }
    
    @Test
    void testContentLengthAndTransferEncodingPrecedence() {
        // Test that Transfer-Encoding takes precedence over Content-Length
        String request = "POST /test HTTP/1.1\r\nHost: example.com\r\n" +
                        "Content-Length: 100\r\n" +  // Wrong length
                        "Transfer-Encoding: chunked\r\n\r\n" +
                        "4\r\ntest\r\n0\r\n\r\n";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(1, results.size());
        String parsed = new String(results.get(0), StandardCharsets.ISO_8859_1);
        // Should have parsed chunked body correctly
        assertTrue(parsed.contains("test"));
        assertFalse(parsed.contains("<incomplete"));
    }
    
    @Test
    void testTriplePipelinedWithDifferentConfigs() {
        // Three requests with different characteristics
        String req1 = "GET /first HTTP/1.1\r\nHost: example.com\r\n\r\n";  // No body
        String req2 = "POST /second HTTP/1.1\r\nHost: example.com\r\nContent-Length: 6\r\n\r\nmiddle";
        String req3 = "PUT /third HTTP/1.1\r\nHost: example.com\r\nTransfer-Encoding: chunked\r\n\r\n" +
                     "4\r\nlast\r\n0\r\n\r\n";
        byte[] input = (req1 + req2 + req3).getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(3, results.size());
        
        assertTrue(new String(results.get(0), StandardCharsets.ISO_8859_1).contains("GET /first"));
        assertTrue(new String(results.get(1), StandardCharsets.ISO_8859_1).contains("middle"));
        assertTrue(new String(results.get(2), StandardCharsets.ISO_8859_1).contains("last"));
    }
    
    @Test
    void testChunkedWithExtensions() {
        // Chunked encoding with chunk extensions (should be ignored)
        String request = "POST /test HTTP/1.1\r\nHost: example.com\r\nTransfer-Encoding: chunked\r\n\r\n" +
                        "5;name=value\r\nhello\r\n" +
                        "3;ext\r\nbye\r\n" +
                        "0\r\n\r\n";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        assertEquals(1, results.size());
        String parsed = new String(results.get(0), StandardCharsets.ISO_8859_1);
        // Should have decoded body "hellobye" ignoring extensions
        assertTrue(parsed.contains("hellobye"));
    }
    
    @Test
    void testNestedProxyChainWithThreeParsers() {
        // Parser 1: Custom line endings, outputs standard
        HttpParserModel parser1 = new HttpParserModel();
        parser1.setHeaderLineEndings(Arrays.asList("\\n"));
        
        // Parser 2: Forces chunked encoding
        HttpParserModel parser2 = new HttpParserModel();
        parser2.setOutputBodyEncoding(HttpParserModel.MessageLenBodyEncoding.FORCE_CHUNKED);
        
        // Parser 3: Forces Content-Length
        HttpParserModel parser3 = new HttpParserModel();
        parser3.setOutputBodyEncoding(HttpParserModel.MessageLenBodyEncoding.FORCE_CL_HEADER);
        
        String request = "POST /test HTTP/1.1\nHost: example.com\nContent-Length: 11\n\nhello world";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        // Through parser 1
        List<byte[]> results1 = ParserChainRunner.parseRequestsForProxyRaw(parser1, input);
        assertEquals(1, results1.size());
        String p1_out = new String(results1.get(0), StandardCharsets.ISO_8859_1);
        assertTrue(p1_out.contains("\r\n")); // Standard line endings
        
        // Through parser 2
        List<byte[]> results2 = ParserChainRunner.parseRequestsForProxyRaw(parser2, results1.get(0));
        assertEquals(1, results2.size());
        String p2_out = new String(results2.get(0), StandardCharsets.ISO_8859_1);
        assertTrue(p2_out.contains("Transfer-Encoding: chunked"));
        assertFalse(p2_out.contains("Content-Length"));
        
        // Through parser 3
        List<byte[]> results3 = ParserChainRunner.parseRequestsForProxyRaw(parser3, results2.get(0));
        assertEquals(1, results3.size());
        String p3_out = new String(results3.get(0), StandardCharsets.ISO_8859_1);
        assertTrue(p3_out.contains("Content-Length: 11"));
        assertFalse(p3_out.contains("Transfer-Encoding"));
        assertTrue(p3_out.contains("hello world"));
    }
}