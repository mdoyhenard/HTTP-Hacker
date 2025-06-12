package httpraider.view.panels;

import javax.swing.*;
import java.awt.*;

import static httpraider.controller.NetworkController.ICON_HEIGHT;
import static httpraider.controller.NetworkController.ICON_WIDTH;

public class ProxyView extends JComponent {

    private final String id;
    private final boolean isClient;
    private static final Image clientImg;
    private static final Image proxyImg;

    static {
        ImageIcon c = null, p = null;
        try {
            c = new ImageIcon(ProxyView.class.getResource("/clientIcon.png"));
            p = new ImageIcon(ProxyView.class.getResource("/proxyIcon.png"));
        } catch (Exception ignored) {}
        clientImg = (c != null && c.getIconWidth() > 0) ? c.getImage().getScaledInstance(ICON_WIDTH, ICON_HEIGHT, Image.SCALE_SMOOTH) : null;
        proxyImg  = (p != null && p.getIconWidth() > 0) ? p.getImage().getScaledInstance(ICON_WIDTH, ICON_HEIGHT, Image.SCALE_SMOOTH) : null;
    }

    public ProxyView(String id, boolean isClient) {
        this.id = id;
        this.isClient = isClient;
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(ICON_WIDTH, ICON_HEIGHT);
    }

    public String getId() { return id; }

    public boolean isClient() { return isClient; }

    public Point getCenter() {
        Point loc = getLocation();
        return new Point(loc.x + getWidth()/2, loc.y + getHeight()/2);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Image img = isClient ? clientImg : proxyImg;
        if (img != null) {
            int x = (getWidth() - ICON_WIDTH) / 2;
            int y = (getHeight() - ICON_HEIGHT) / 2;
            g.drawImage(img, x, y, ICON_WIDTH, ICON_HEIGHT, this);
        } else {
            g.setColor(isClient ? new Color(210,230,250) : Color.WHITE);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g.setColor(isClient ? new Color(60,130,200) : new Color(160,120,60));
            g.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12);
        }
    }
}
