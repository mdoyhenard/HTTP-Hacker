package RawRepeater.Filters;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;

public class HexDocumentFilter extends DocumentFilter {
    @Override
    public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
        if (isValidInput(fb.getDocument(), string)) {
            super.insertString(fb, offset, string, attr);
        }
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
        if (isValidInput(fb.getDocument(), text)) {
            super.replace(fb, offset, length, text, attrs);
        }
    }

    @Override
    public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
        super.remove(fb, offset, length);
    }

    // Método para validar la entrada
    private boolean isValidInput(Document doc, String text) throws BadLocationException {
        // Verificar si el nuevo texto combinado excede los 2 caracteres permitidos
        int newLength = doc.getLength() + text.length();
        if (newLength > 2) {
            return false;
        }

        // Verificar si el texto ingresado es válido (0-9, a-f)
        return text.matches("[0-9a-f]*");
    }
}