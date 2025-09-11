import httpraider.model.network.HttpParserModel;
import httpraider.parser.ParserChainRunner;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class DebugIncompleteTest {
    public static void main(String[] args) {
        HttpParserModel model = new HttpParserModel();
        
        // First part: incomplete headers (no double CRLF)
        String part1 = "GET /test HTTP/1.1\r\nHost: example.com\r\nUser-Agent: test";
        
        // Parse first part alone - should get incomplete
        byte[] input1 = part1.getBytes(StandardCharsets.ISO_8859_1);
        List<byte[]> results1 = ParserChainRunner.parseRequestsForProxyRaw(model, input1);
        
        System.out.println("Results count: " + results1.size());
        if (results1.size() > 0) {
            String parsed1 = new String(results1.get(0), StandardCharsets.ISO_8859_1);
            System.out.println("Result: " + parsed1);
            System.out.println("Contains '<incomplete_request': " + parsed1.contains("<incomplete_request"));
            System.out.println("Contains 'incomplete headers': " + parsed1.contains("incomplete headers"));
        } else {
            System.out.println("No results returned!");
        }
    }
}