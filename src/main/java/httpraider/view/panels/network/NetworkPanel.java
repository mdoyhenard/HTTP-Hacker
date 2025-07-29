package httpraider.view.panels.network;

import httpraider.view.menuBars.NetworkBar;
import httpraider.view.menuBars.ProxyBar;
import httpraider.view.panels.HttpEditorPanel;
import httpraider.view.panels.HttpMultiEditorPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class NetworkPanel extends JPanel {

    private final NetworkBar networkBar;
    private final ProxyBar proxyBar;
    private final NetworkCanvas networkCanvas;

    private final JPanel centerPanel;
    private JComponent currentCenter;
    private JSplitPane cachedVerticalSplit;
    private JSplitPane cachedHorizontalSplit;
    private boolean editorsVisible = false;
    private static final double DEFAULT_VERTICAL_SPLIT = 0.5;
    private static final double DEFAULT_HORIZONTAL_SPLIT = 0.5;
    private int savedVerticalDividerLocation = 0;
    private int savedHorizontalDividerLocation = 0;

    public NetworkPanel() {
        super(new BorderLayout());

        networkBar    = new NetworkBar();
        proxyBar      = new ProxyBar();
        networkCanvas = new NetworkCanvas();

        centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(networkCanvas, BorderLayout.CENTER);
        centerPanel.add(proxyBar, BorderLayout.EAST);
        proxyBar.setVisible(false);

        currentCenter = centerPanel;

        add(networkBar, BorderLayout.NORTH);
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
        if (proxyBar.isVisible() != visible) {
            SwingUtilities.invokeLater(() -> {
                proxyBar.setVisible(visible);
                revalidate();
                repaint();
            });
        }
    }

    public void showHttpEditors(HttpEditorPanel<?> left, HttpMultiEditorPanel right) {
        if (editorsVisible && cachedVerticalSplit != null) {
            savedVerticalDividerLocation = cachedVerticalSplit.getDividerLocation();
        }
        if (editorsVisible && cachedHorizontalSplit != null) {
            savedHorizontalDividerLocation = cachedHorizontalSplit.getDividerLocation();
        }
        
        if (editorsVisible) {
            cachedHorizontalSplit.setLeftComponent(left);
            cachedHorizontalSplit.setRightComponent(right);
            return;
        }
        
        if (cachedHorizontalSplit == null) {
            cachedHorizontalSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            cachedHorizontalSplit.setResizeWeight(DEFAULT_HORIZONTAL_SPLIT);
            cachedHorizontalSplit.setOneTouchExpandable(false);
            cachedHorizontalSplit.setContinuousLayout(true);
        }
        
        if (cachedVerticalSplit == null) {
            cachedVerticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
            cachedVerticalSplit.setResizeWeight(DEFAULT_VERTICAL_SPLIT);
            cachedVerticalSplit.setOneTouchExpandable(false);
            cachedVerticalSplit.setContinuousLayout(true);
            cachedVerticalSplit.setTopComponent(centerPanel);
        }
        
        cachedHorizontalSplit.setLeftComponent(left);
        cachedHorizontalSplit.setRightComponent(right);
        cachedVerticalSplit.setBottomComponent(cachedHorizontalSplit);
        
        remove(currentCenter);
        currentCenter = cachedVerticalSplit;
        add(currentCenter, BorderLayout.CENTER);
        editorsVisible = true;
        
        revalidate();
        repaint();
        
        SwingUtilities.invokeLater(() -> {
            int panelHeight = getHeight();
            int panelWidth = getWidth();
            
            if (panelHeight > 0) {
                if (savedVerticalDividerLocation > 0) {
                    cachedVerticalSplit.setDividerLocation(savedVerticalDividerLocation);
                } else {
                    int defaultLocation = (int)(panelHeight * DEFAULT_VERTICAL_SPLIT);
                    cachedVerticalSplit.setDividerLocation(defaultLocation);
                    savedVerticalDividerLocation = defaultLocation;
                }
            }
            if (panelWidth > 0 && cachedHorizontalSplit != null) {
                if (savedHorizontalDividerLocation > 0) {
                    cachedHorizontalSplit.setDividerLocation(savedHorizontalDividerLocation);
                } else {
                    int defaultLocation = (int)(panelWidth * DEFAULT_HORIZONTAL_SPLIT);
                    cachedHorizontalSplit.setDividerLocation(defaultLocation);
                    savedHorizontalDividerLocation = defaultLocation;
                }
            }
        });
    }

    public void hideHttpEditors() {
        SwingUtilities.invokeLater(() -> {
            if (!editorsVisible) {
                return;
            }
            
            if (cachedVerticalSplit != null) {
                savedVerticalDividerLocation = cachedVerticalSplit.getDividerLocation();
            }
            if (cachedHorizontalSplit != null) {
                savedHorizontalDividerLocation = cachedHorizontalSplit.getDividerLocation();
            }
            
            remove(currentCenter);
            currentCenter = centerPanel;
            add(currentCenter, BorderLayout.CENTER);
            editorsVisible = false;
            
            cachedVerticalSplit = null;
            cachedHorizontalSplit = null;
            
            revalidate();
            repaint();
        });
    }
    
    public void resetSplitPanes() {
        SwingUtilities.invokeLater(() -> {
            if (cachedVerticalSplit != null && editorsVisible) {
                int panelHeight = getHeight();
                int panelWidth = getWidth();
                
                if (panelHeight > 0) {
                    if (savedVerticalDividerLocation > 0) {
                        cachedVerticalSplit.setDividerLocation(savedVerticalDividerLocation);
                    } else {
                        cachedVerticalSplit.setDividerLocation((int)(panelHeight * DEFAULT_VERTICAL_SPLIT));
                    }
                }
                if (cachedHorizontalSplit != null && panelWidth > 0) {
                    if (savedHorizontalDividerLocation > 0) {
                        cachedHorizontalSplit.setDividerLocation(savedHorizontalDividerLocation);
                    } else {
                        cachedHorizontalSplit.setDividerLocation((int)(panelWidth * DEFAULT_HORIZONTAL_SPLIT));
                    }
                }
            }
        });
    }
}
