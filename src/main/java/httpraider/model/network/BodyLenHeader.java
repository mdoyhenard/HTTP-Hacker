// file: httpraider/model/network/BodyLenHeader.java
package httpraider.model.network;

import java.io.Serial;
import java.io.Serializable;

public class BodyLenHeader implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String headerNamePattern;
    private boolean chunkedEncoding;

    public BodyLenHeader() {
        this("", false);
    }

    public BodyLenHeader(String headerNamePattern, boolean chunkedEncoding) {
        this.headerNamePattern = headerNamePattern;
        this.chunkedEncoding = chunkedEncoding;
    }

    public String getHeaderNamePattern() {
        return headerNamePattern;
    }

    public void setHeaderNamePattern(String headerNamePattern) {
        this.headerNamePattern = headerNamePattern;
    }

    public boolean isChunkedEncoding() {
        return chunkedEncoding;
    }

    public void setChunkedEncoding(boolean chunkedEncoding) {
        this.chunkedEncoding = chunkedEncoding;
    }
}
