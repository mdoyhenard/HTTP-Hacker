package RawRepeater;

import burp.api.montoya.ui.editor.HttpRequestEditor;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;

public class HttpRequestEditorHighlighter {

    private final HttpRequestEditor editor;
    private final JTextComponent textComponent;
    private final Highlighter highlighter;


    public HttpRequestEditorHighlighter(HttpRequestEditor editor) {
        this.editor = editor;
        // Se utiliza el texto del request para ayudar a identificar el componente
        String text = this.editor.getRequest().toString();
        // Se asume que editor.uiComponent() es un JComponent
        JComponent uiComponent = (JComponent) editor.uiComponent();
        this.textComponent = findTextComponent(uiComponent, text);
        if (this.textComponent == null) {
            throw new IllegalStateException("No se pudo encontrar un componente de texto en el editor.");
        }
        this.highlighter = textComponent.getHighlighter();
    }


    public void highlightRange(int start, int end, Color color) {
        clearHighlights();
        try {
            Highlighter.HighlightPainter painter = new DefaultHighlighter.DefaultHighlightPainter(color);
            highlighter.addHighlight(start, end, painter);
            repaint();
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    public void addHighlightRange(int start, int end, Color color) {
        try {
            Highlighter.HighlightPainter painter = new DefaultHighlighter.DefaultHighlightPainter(color);
            highlighter.addHighlight(start, end, painter);
            repaint();
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }


    public void repaint() {
        textComponent.repaint();
    }


    public void clearHighlights() {
        highlighter.removeAllHighlights();
        repaint();
    }


    public void addAutoUpdateHighlightListener(final int start, final int end, final Color color) {
        Document doc = textComponent.getDocument();
        doc.addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(() -> highlightRange(start, end, color));
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(() -> highlightRange(start, end, color));
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(() -> highlightRange(start, end, color));
            }
        });
    }


    private JTextComponent findTextComponent(Component comp, String text) {
        if (comp instanceof JTextComponent) {
            if (((JTextComponent) comp).getText().contains(text)) {
                return (JTextComponent) comp;
            }
        }
        if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents()) {
                JTextComponent result = findTextComponent(child, text);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    public HttpRequestEditor getEditor() {
        return editor;
    }
}
