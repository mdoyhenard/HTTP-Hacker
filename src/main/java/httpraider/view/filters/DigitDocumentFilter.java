package httpraider.view.filters;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

public class DigitDocumentFilter extends DocumentFilter {

    private final int size;

    public DigitDocumentFilter(){
        super();
        size = Integer.MAX_VALUE;
    }

    public DigitDocumentFilter(int size){
        super();
        this.size = size;
    }

    @Override
    public void insertString(FilterBypass fb, int offs, String str, AttributeSet a)
            throws BadLocationException {
        if (str == null || !str.matches("\\d+")) {
            return;
        }
        int newLength = fb.getDocument().getLength() + str.length();
        if (newLength<=size)
            super.insertString(fb, offs, str, a);
    }

    @Override
    public void replace(FilterBypass fb, int offs, int len, String str, AttributeSet a)
            throws BadLocationException {
        if (str == null || !str.matches("\\d+")) {
            return;
        }
        int newLength = fb.getDocument().getLength() + str.length() - len;
        if (newLength<=size)
            super.replace(fb, offs, len, str, a);
    }
}
