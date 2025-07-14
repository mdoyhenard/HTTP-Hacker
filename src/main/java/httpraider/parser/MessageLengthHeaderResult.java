package httpraider.parser;

import java.util.List;

public class MessageLengthHeaderResult {
    private final List<String> headerLines;
    private final byte[] body;
    private final byte[] remaining;
    private final String error;
    private final int incompleteBodyBytes;
    private final String chunkedIncompleteTag;

    public MessageLengthHeaderResult(List<String> headerLines, byte[] body, byte[] remaining, String error, int incompleteBodyBytes, String chunkedIncompleteTag) {
        this.headerLines = headerLines;
        this.body = body;
        this.remaining = remaining;
        this.error = error;
        this.incompleteBodyBytes = incompleteBodyBytes;
        this.chunkedIncompleteTag = chunkedIncompleteTag;
    }

    public List<String> getHeaderLines() { return headerLines; }
    public byte[] getBody() { return body; }
    public byte[] getRemaining() { return remaining; }
    public String getError() { return error; }
    public int getIncompleteBodyBytes() { return incompleteBodyBytes; }
    public String getChunkedIncompleteTag() { return chunkedIncompleteTag; }
}
