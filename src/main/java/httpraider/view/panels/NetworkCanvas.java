package httpraider.view.panels;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.*;
import java.util.List;

public class NetworkCanvas extends JPanel {

    private final Map<String, ProxyView> proxyViews;
    private final List<ConnectionView> connectionViews;
    private ProxyView connectStartProxy;
    private Point mousePoint;
    private ConnectionView highlightedConnection;
    private Point pan = new Point(0, 0);

    public NetworkCanvas() {
        super(null);
        proxyViews = new HashMap<>();
        connectionViews = new ArrayList<>();
        setBackground(new Color(252, 252, 255, 255));
        setFocusable(true);
    }

    public void addCanvasMouseListener(MouseListener l) {
        addMouseListener(l);
    }

    public void addCanvasMouseMotionListener(MouseMotionListener l) {
        addMouseMotionListener(l);
    }

    public ConnectionView getConnectionAt(Point p) {
        double tolerance = 7.0;
        for (ConnectionView cv : connectionViews) {
            Point p1 = cv.getFrom().getCenter();
            Point p2 = cv.getTo().getCenter();
            double dist = ptSegDist(p1.x, p1.y, p2.x, p2.y, p.x, p.y);
            if (dist < tolerance) return cv;
        }
        return null;
    }

    private double ptSegDist(double x1, double y1, double x2, double y2, double px, double py) {
        double dx = x2 - x1, dy = y2 - y1;
        if (dx == 0 && dy == 0) return Point.distance(x1, y1, px, py);
        double t = ((px - x1) * dx + (py - y1) * dy) / (dx*dx + dy*dy);
        t = Math.max(0, Math.min(1, t));
        double projX = x1 + t * dx, projY = y1 + t * dy;
        return Point.distance(projX, projY, px, py);
    }

    public ConnectionView getHighlightedConnection() {
        return highlightedConnection;
    }

    public void setHighlightedConnection(ConnectionView cv) {
        if (highlightedConnection != null && highlightedConnection != cv) {
            highlightedConnection.setHighlighted(false);
        }
        highlightedConnection = cv;
        if (highlightedConnection != null) {
            highlightedConnection.setHighlighted(true);
        }
        repaint();
    }


    public void setPan(Point pan) {
        this.pan = pan;
        repaint();
    }

    public Point getPan() {
        return pan;
    }

    public void addProxyView(String id, ProxyView pv, Point location) {
        proxyViews.put(id, pv);
        pv.setLocation(location);
        add(pv);
        repaint();
    }

    public void removeProxyView(String id) {
        ProxyView pv = proxyViews.remove(id);
        if (pv != null) {
            remove(pv);
            repaint();
        }
    }

    public ProxyView getProxyView(String id) {
        return proxyViews.get(id);
    }

    public Collection<ProxyView> getProxyViews() {
        return proxyViews.values();
    }

    public void addConnectionView(ConnectionView cv) {
        connectionViews.add(cv);
        repaint();
    }

    public void removeConnectionView(ConnectionView cv) {
        connectionViews.remove(cv);
        repaint();
    }

    public List<ConnectionView> getConnectionViews() {
        return connectionViews;
    }

    public void setConnectStartProxy(ProxyView pv) {
        connectStartProxy = pv;
        repaint();
    }

    public ProxyView getConnectStartProxy() {
        return connectStartProxy;
    }

    public void setMousePoint(Point p) {
        mousePoint = p;
        repaint();
    }

    public Point getMousePoint() {
        return mousePoint;
    }

    public void clear() {
        removeAll();
        proxyViews.clear();
        connectionViews.clear();
        highlightedConnection = null;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        for (ConnectionView cv : connectionViews) {
            cv.paintLine(g2);
        }

        if (connectStartProxy != null && mousePoint != null) {
            Point from = connectStartProxy.getCenter();
            g2.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{6, 6}, 0));
            g2.setColor(Color.GRAY);
            g2.drawLine(from.x, from.y, mousePoint.x, mousePoint.y);
        }

        g2.dispose();
    }
}
