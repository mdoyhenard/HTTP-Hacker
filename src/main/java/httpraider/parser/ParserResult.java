package httpraider.parser;

public class ParserResult {

    private byte[] parsedPayload;
    private byte[] unparsedRemaining;
    private String error;

    public ParserResult(byte[] parsedPayload, byte[] unparsedRemaining, String error) {
        this.parsedPayload = parsedPayload;
        this.unparsedRemaining = unparsedRemaining;
        this.error = error;
    }

    public byte[] getParsedPayload() {
        return parsedPayload;
    }

    public void setParsedPayload(byte[] parsedPayload) {
        this.parsedPayload = parsedPayload;
    }

    public byte[] getUnparsedRemaining() {
        return unparsedRemaining;
    }

    public void setUnparsedRemaining(byte[] unparsedRemaining) {
        this.unparsedRemaining = unparsedRemaining;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
