package httpraider.view.components.network;

import java.awt.*;

public class ConnectionLine {

    private final ProxyComponent from;
    private final ProxyComponent to;
    private boolean highlighted;
    private static final BasicStroke HIGHLIGHT_STROKE = new BasicStroke(15);
    private static final BasicStroke NORMAL_STROKE = new BasicStroke(4);
    private static final BasicStroke HIGHLIGHT_INNER_STROKE = new BasicStroke(5);

    public ConnectionLine(ProxyComponent from, ProxyComponent to) {
        this.from = from;
        this.to = to;
        this.highlighted = false;
    }

    public void setHighlighted(boolean highlighted) {
        this.highlighted = highlighted;
    }

    public boolean isHighlighted() {
        return highlighted;
    }

    public void paintLine(Graphics2D g2) {
        Point p1 = from.getCenter();
        Point p2 = to.getCenter();
        if (highlighted) {
            g2.setStroke(HIGHLIGHT_STROKE);
            g2.setColor(new Color(100, 160, 255, 119));
            g2.drawLine(p1.x, p1.y, p2.x, p2.y);
            g2.setStroke(HIGHLIGHT_INNER_STROKE);
            g2.setColor(Color.BLACK);
            g2.drawLine(p1.x, p1.y, p2.x, p2.y);
        } else {
            g2.setStroke(NORMAL_STROKE);
            g2.setColor(Color.BLACK);
            g2.drawLine(p1.x, p1.y, p2.x, p2.y);
        }
    }


    public ProxyComponent getFrom() {
        return from;
    }

    public ProxyComponent getTo() {
        return to;
    }
}
