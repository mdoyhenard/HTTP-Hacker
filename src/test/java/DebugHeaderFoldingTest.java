import httpraider.parser.*;
import httpraider.model.network.HttpParserModel;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class DebugHeaderFoldingTest {
    public static void main(String[] args) {
        HttpParserModel model = new HttpParserModel();
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
        
        System.out.println("Number of results: " + results.size());
        for (int i = 0; i < results.size(); i++) {
            System.out.println("\n=== Result " + i + " ===");
            String parsed = new String(results.get(i), StandardCharsets.ISO_8859_1);
            System.out.println(parsed);
            System.out.println("=== End Result " + i + " ===");
            
            // Check specific header
            String[] lines = parsed.split("\r\n");
            for (String line : lines) {
                if (line.startsWith("X-Complex:")) {
                    System.out.println("X-Complex header: [" + line + "]");
                }
            }
        }
    }
}