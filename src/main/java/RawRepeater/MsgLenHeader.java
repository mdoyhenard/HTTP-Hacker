package RawRepeater;
import org.apache.commons.text.StringEscapeUtils;

public class MsgLenHeader {

    private boolean isTE;
    private String pattern;
    private final String placeHolder = "<int>";
    private boolean useLast;

    public MsgLenHeader() {
        this.isTE = false;
        this.useLast = true;
        this.pattern = "";
    }

    public MsgLenHeader(String pattern) {
        this.isTE = false;
        this.useLast = true;
        this.pattern = pattern;
    }

    public MsgLenHeader(boolean isTE, boolean useLast, String pattern) {
        this.isTE = isTE;
        this.useLast = useLast;
        this.pattern = pattern;
    }

    public MsgLenHeader(MsgLenHeader msglen) {
        this.isTE = msglen.isTE;
        this.useLast = msglen.useLast;
        this.pattern = msglen.pattern;
    }

    public boolean isTE() {
        return isTE;
    }

    public void setTE(boolean TE) {
        isTE = TE;
    }

    public String getPattern() {
        return pattern;
    }

    public String getEscapedPattern(){
        return StringEscapeUtils.unescapeJava(pattern);
    }

    public String getEscapedPreffixPattern(){
        if (isTE || !pattern.contains(placeHolder)) return getEscapedPattern();
        return StringEscapeUtils.unescapeJava(pattern.substring(0, pattern.indexOf(placeHolder)));
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getPlaceHolder() {
        return placeHolder;
    }

    public boolean isUseLast() {
        return useLast;
    }

    public void setUseLast(boolean useLast) {
        this.useLast = useLast;
    }
}
