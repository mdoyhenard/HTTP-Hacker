package httpraider.view.components;

import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import java.awt.*;

public class FullLineHighlightPainter implements Highlighter.HighlightPainter {
    private final Color color;
    public FullLineHighlightPainter(Color color) { this.color = color; }

    @Override
    public void paint(Graphics g, int p0, int p1, Shape bounds, JTextComponent c) {
        try {
            Rectangle rect0 = c.modelToView(p0);
            Rectangle rect1 = c.modelToView(p1);
            if (rect0 == null || rect1 == null) return;
            int startY = rect0.y;
            int endY = rect1.y;
            int h = c.getFontMetrics(c.getFont()).getHeight();
            g.setColor(color);
            for (int y = startY; y <= endY; y += h) {
                g.fillRect(0, y, c.getWidth(), h);
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
}
