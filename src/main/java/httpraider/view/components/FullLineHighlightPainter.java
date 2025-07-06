package httpraider.view.components;

import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import java.awt.*;

public class FullLineHighlightPainter implements Highlighter.HighlightPainter {
    private final Color color;

    public FullLineHighlightPainter(Color color) {
        this.color = color;
    }

    @Override
    public void paint(Graphics g, int p0, int p1, Shape bounds, JTextComponent c) {
        try {
            Rectangle r0 = c.modelToView(p0);
            Rectangle r1 = c.modelToView(p1);
            if (r0 == null || r1 == null) return;
            g.setColor(color);

            if (r0.y == r1.y) {
                int y = r0.y;
                int height = r0.height;
                int x0 = r0.x;
                int x1 = r1.x;
                g.fillRect(x0, y, x1 - x0, height);
            } else {
                int h = r0.height;
                // First line
                g.fillRect(r0.x, r0.y, c.getWidth() - r0.x, h);
                // Middle lines
                for (int y = r0.y + h; y < r1.y; y += h) {
                    g.fillRect(0, y, c.getWidth(), h);
                }
                // Last line
                g.fillRect(0, r1.y, r1.x, r1.height);
            }
        } catch (BadLocationException e) {
            // Do nothing (invalid location)
        }
    }
}
