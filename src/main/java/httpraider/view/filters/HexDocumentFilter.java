package httpraider.view.filters;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

public class HexDocumentFilter extends DocumentFilter {

    @Override
    public void insertString(FilterBypass fb, int offs, String str, AttributeSet a)
            throws BadLocationException {
        if (str != null && str.matches("[0-9a-fA-F ]+"))
            super.insertString(fb, offs, str.toUpperCase(), a);
    }
    @Override
    public void replace(FilterBypass fb, int offs, int len, String str, AttributeSet a)
            throws BadLocationException {
        if (str != null && str.matches("[0-9a-fA-F ]+"))
            super.replace(fb, offs, len, str.toUpperCase(), a);
    }
}
