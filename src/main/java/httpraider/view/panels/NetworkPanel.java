package httpraider.view.panels;

import httpraider.view.menuBars.NetworkBar;
import httpraider.view.menuBars.ProxyBar;

import javax.swing.*;
import java.awt.*;

public class NetworkPanel extends JPanel {

    private final NetworkBar networkBar;
    private final ProxyBar proxyBar;
    private final NetworkCanvas networkCanvas;

    // the standalone canvas+proxy bar
    private final JPanel centerPanel;

    // the current center component (either centerPanel or the splitPane)
    private JComponent currentCenter;

    public NetworkPanel() {
        super(new BorderLayout());

        networkBar    = new NetworkBar();
        proxyBar      = new ProxyBar();
        networkCanvas = new NetworkCanvas();

        // build the simple center panel
        centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(networkCanvas, BorderLayout.CENTER);
        centerPanel.add(proxyBar,      BorderLayout.EAST);
        proxyBar.setVisible(false);

        // start with just the canvas+proxy bar
        currentCenter = centerPanel;

        add(networkBar,    BorderLayout.NORTH);
        add(currentCenter, BorderLayout.CENTER);

        setFocusable(true);
    }

    public NetworkBar getNetworkBar() {
        return networkBar;
    }

    public ProxyBar getProxyBar() {
        return proxyBar;
    }

    public NetworkCanvas getNetworkCanvas() {
        return networkCanvas;
    }

    public void setProxyBarVisible(boolean visible) {
        proxyBar.setVisible(visible);
        revalidate();
        repaint();
    }

    public void showHttpEditors(HttpEditorPanel<?> left, HttpEditorPanel<?> right) {
        JSplitPane httpEditorSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        httpEditorSplit.setResizeWeight(0.5);

        JSplitPane vertical = new JSplitPane(JSplitPane.VERTICAL_SPLIT, centerPanel, httpEditorSplit);
        vertical.setResizeWeight(0.6);

        remove(currentCenter);
        currentCenter = vertical;
        add(currentCenter, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    public void hideHttpEditors() {
        remove(currentCenter);
        currentCenter = centerPanel;
        add(currentCenter, BorderLayout.CENTER);

        revalidate();
        repaint();
    }
}
