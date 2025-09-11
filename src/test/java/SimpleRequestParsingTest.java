import httpraider.model.network.HttpParserModel;
import httpraider.parser.ParserChainRunner;
import java.util.Arrays;
import java.util.List;

public class SimpleRequestParsingTest {
    public static void main(String[] args) {
        // Create a simple HTTP request
        String rawRequest = "GET / HTTP/1.1\r\nHost: example.com\r\n\r\n";
        byte[] rawBytes = rawRequest.getBytes();
        
        // Create parser model with default settings
        HttpParserModel model = new HttpParserModel();
        model.setHeaderLineEndings(Arrays.asList("\\r\\n", "\\n"));
        
        System.out.println("Original request bytes: " + rawBytes.length);
        System.out.println("Original request: " + new String(rawBytes).replace("\r", "\\r").replace("\n", "\\n"));
        
        // Parse once
        List<byte[]> firstParse = ParserChainRunner.parseRequestsForProxyRaw(model, rawBytes);
        System.out.println("\nFirst parse results: " + firstParse.size() + " requests");
        for (int i = 0; i < firstParse.size(); i++) {
            byte[] req = firstParse.get(i);
            System.out.println("Request " + (i+1) + " bytes: " + req.length);
            System.out.println("Request " + (i+1) + ": " + new String(req).replace("\r", "\\r").replace("\n", "\\n"));
        }
        
        // Parse the first parsed request again
        if (!firstParse.isEmpty()) {
            System.out.println("\n--- Re-parsing first request ---");
            List<byte[]> secondParse = ParserChainRunner.parseRequestsForProxyRaw(model, firstParse.get(0));
            System.out.println("Second parse results: " + secondParse.size() + " requests");
            for (int i = 0; i < secondParse.size(); i++) {
                byte[] req = secondParse.get(i);
                System.out.println("Request " + (i+1) + " bytes: " + req.length);
                System.out.println("Request " + (i+1) + ": " + new String(req).replace("\r", "\\r").replace("\n", "\\n"));
            }
        }
    }
}