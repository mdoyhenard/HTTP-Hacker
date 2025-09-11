import httpraider.model.network.HttpParserModel;
import httpraider.parser.ParserChainRunner;
import httpraider.parser.ParserUtils;
import httpraider.parser.ParserResult;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class DebugIncompleteTest2 {
    public static void main(String[] args) {
        HttpParserModel model = new HttpParserModel();
        
        // First part: incomplete headers (no double CRLF)
        String part1 = "GET /test HTTP/1.1\r\nHost: example.com\r\nUser-Agent: test";
        byte[] input1 = part1.getBytes(StandardCharsets.ISO_8859_1);
        
        // Test splitHeaders to see what it returns
        ParserResult split = ParserUtils.splitHeaders(model, input1);
        System.out.println("Headers bytes length: " + split.getParsedPayload().length);
        System.out.println("Error: " + split.getError());
        if (split.getError() != null) {
            System.out.println("Error contains 'Header/body delimiter not found': " + 
                split.getError().contains("Header/body delimiter not found"));
        }
    }
}