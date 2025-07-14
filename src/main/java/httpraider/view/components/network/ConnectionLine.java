package httpraider.view.components.network;

import java.awt.*;

public class ConnectionLine {

    private final ProxyComponent from;
    private final ProxyComponent to;
    private boolean highlighted;

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
            g2.setStroke(new BasicStroke(15));
            g2.setColor(new Color(100, 160, 255, 119));
            g2.drawLine(p1.x, p1.y, p2.x, p2.y);
            g2.setStroke(new BasicStroke(5));
            g2.setColor(Color.BLACK);
            g2.drawLine(p1.x, p1.y, p2.x, p2.y);
        } else {
            g2.setStroke(new BasicStroke(4));
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
