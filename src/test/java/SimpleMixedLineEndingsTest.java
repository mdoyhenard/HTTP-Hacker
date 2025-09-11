import httpraider.parser.*;
import httpraider.model.network.HttpParserModel;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class SimpleMixedLineEndingsTest {
    public static void main(String[] args) {
        HttpParserModel model = new HttpParserModel();
        model.setHeaderLineEndings(Arrays.asList("\\r\\n", "\\n", "\\r"));
        
        String request = "GET /test HTTP/1.1\nHost: example.com\r\nContent-Length: 4\r\n\rtest";
        byte[] input = request.getBytes(StandardCharsets.ISO_8859_1);
        
        List<byte[]> results = ParserChainRunner.parseRequestsForProxyRaw(model, input);
        
        System.out.println("Number of results: " + results.size());
        System.out.println("Test expects: 1");
        System.out.println("Test assertion would " + (results.size() == 1 ? "PASS" : "FAIL"));
    }
}