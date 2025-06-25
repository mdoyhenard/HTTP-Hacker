// file: httpraider/model/network/HTTPParserSettings.java
package httpraider.model.network;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class HTTPParserSettings implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<String> headerEndSequences;
    private List<String> headerSplitSequences;
    private List<BodyLenHeader> bodyLenHeaders;
    private String codeStep1;
    private String codeStep2;
    private String codeStep3;

    public HTTPParserSettings() {
        this.headerEndSequences = new ArrayList<>();
        this.headerEndSequences.add("\r\n\r\n");

        this.headerSplitSequences = new ArrayList<>();
        this.headerSplitSequences.add("\r\n");

        this.bodyLenHeaders = new ArrayList<>();

        this.codeStep1 = "";
        this.codeStep2 = "";
        this.codeStep3 = "";
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

    public String getCodeStep1() {
        return codeStep1;
    }

    public void setCodeStep1(String codeStep1) {
        this.codeStep1 = codeStep1;
    }

    public String getCodeStep2() {
        return codeStep2;
    }

    public void setCodeStep2(String codeStep2) {
        this.codeStep2 = codeStep2;
    }

    public String getCodeStep3() {
        return codeStep3;
    }

    public void setCodeStep3(String codeStep3) {
        this.codeStep3 = codeStep3;
    }
}
