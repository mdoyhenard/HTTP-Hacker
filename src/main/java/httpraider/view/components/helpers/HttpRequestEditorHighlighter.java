package httpraider.view.components.helpers;

import burp.api.montoya.ui.editor.HttpRequestEditor;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;

public class HttpRequestEditorHighlighter {

    private final JTextComponent textComponent;
    private final Highlighter highlighter;


    public HttpRequestEditorHighlighter(HttpRequestEditor editor) {

        String text = editor.getRequest().toString();
        JComponent uiComponent = (JComponent) editor.uiComponent();

        this.textComponent = findTextComponent(uiComponent, text);
        if (this.textComponent == null) {
            throw new IllegalStateException("No text component found");
        }
        this.highlighter = textComponent.getHighlighter();
    }

    public void addHighlightRange(int start, int end, Color color) {
        try {
            Highlighter.HighlightPainter painter = new FullLineHighlightPainter(color);
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

}
