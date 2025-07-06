// file: httpraider/model/network/HTTPParserSettings.java
package httpraider.model.network;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpParserModel implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<String> headerEndSequences;
    private List<String> headerSplitSequences;
    private List<BodyLenHeader> bodyLenHeaders;
    private List<String> chunkEndSequences;
    private String headerEndCode;
    private String headerSplitCode;
    private String bodyLenCode;

    private final String insertHeaderJS = "\n\nfunction insertHeader(headers, name, value) { var arr = Array.prototype.slice.call(headers, 0); arr.push(name + ': ' + value); return arr; }\n\n//outHeaderLines = insertHeader(outHeaderLines, \"Content-Length\", \"100\");\n\n//outHeaderLines = parseHeaderBlock(headerBlock);"

            ;

    public HttpParserModel() {
        this.headerEndSequences = new ArrayList<>();
        this.headerEndSequences.add("\\r\\n\\r\\n");
        this.headerEndSequences.add("\\n\\n");

        this.headerSplitSequences = new ArrayList<>();
        this.headerSplitSequences.add("\\r\\n");
        this.headerSplitSequences.add("\\n\\n");

        this.bodyLenHeaders = new ArrayList<>();
        this.bodyLenHeaders.add(new BodyLenHeader("Transfer-Encoding: chunked", true));
        this.bodyLenHeaders.add(new BodyLenHeader("Content-Length: ",false));

        this.chunkEndSequences = new ArrayList<>();
        this.chunkEndSequences.add("\\r\\n");

        this.headerEndCode = "outHeaders = headers;\noutBuffer = buffer;";
        this.headerSplitCode = "outHeaderLines = [];\nfor (var i = 0; i < headerLines.length; i++) {\noutHeaderLines.push(headerLines[i]);\n}" + insertHeaderJS;
        this.bodyLenCode = "outBody = body;\noutBuffer = buffer;\n//outBody = parseBodyLength(headerLines, buffer, body);";
    }

    public List<String> getHeaderEndSequences() {
        return headerEndSequences;
    }

    public void setHeaderEndSequences(List<String> headerEndSequences) {
        this.headerEndSequences = headerEndSequences;
    }

    public List<String> getHeaderSplitSequences() {
        return headerSplitSequences;
    }

    public void setHeaderSplitSequences(List<String> headerSplitSequences) {
        this.headerSplitSequences = headerSplitSequences;
    }

    public List<BodyLenHeader> getBodyLenHeaders() {
        return bodyLenHeaders;
    }

    public void setBodyLenHeaders(List<BodyLenHeader> bodyLenHeaders) {
        this.bodyLenHeaders = bodyLenHeaders;
    }

    public String getHeaderEndCode() {
        return headerEndCode;
    }

    public void setHeaderEndCode(String headerEndCode) {
        this.headerEndCode = headerEndCode;
    }

    public String getHeaderSplitCode() {
        return headerSplitCode;
    }

    public void setHeaderSplitCode(String headerSplitCode) {
        this.headerSplitCode = headerSplitCode;
    }

    public String getBodyLenCode() {
        return bodyLenCode;
    }

    public void setBodyLenCode(String bodyLenCode) {
        this.bodyLenCode = bodyLenCode;
    }

    public List<String> getChunkEndSequences() {
        return chunkEndSequences;
    }

    public void setChunkEndSequences(List<String> chunkEndSequences) {
        this.chunkEndSequences = chunkEndSequences;
    }
}
