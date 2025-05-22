package Network.View.Panels;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import Network.View.Nodes.ConnectionLine;
import Network.View.Nodes.NetworkNode;
import burp.api.montoya.ui.editor.HttpRequestEditor;

public class NetworkPanelContainer extends JPanel {
    private NetworkPanel networkPanel;
    private InfoPanel infoPanel;
    private ConnectionDetailsPanel connectionDetailsPanel;
    private JScrollPane sideScrollPane;
    private HttpRequestEditor previewArea;

    public NetworkPanelContainer() {
        setLayout(new BorderLayout());

        // Top bar with "Detect Network" button.
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topBar.setBackground(new Color(241, 241, 248));
        JButton detectButton = new JButton("Detect Network");

        topBar.add(detectButton);
        add(topBar, BorderLayout.NORTH);

        // Center: Network panel inside a scroll pane.
        networkPanel = new NetworkPanel();
        add(networkPanel, BorderLayout.CENTER);


        connectionDetailsPanel = new ConnectionDetailsPanel(networkPanel);
        //connectionDetailsPanel.setPreferredSize(new Dimension(200, 600));

        sideScrollPane = new JScrollPane();
        sideScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        // Listen for node selection changes.
        networkPanel.setNodeSelectionListener(node -> {
            remove(sideScrollPane);
            if (node != null) {
                if (node instanceof NetworkNode<?>) {
                    sideScrollPane.setViewportView(infoPanel);
                    infoPanel.updateDetails((NetworkNode<?>) node);
                }
                else if (node instanceof ConnectionLine){
                    sideScrollPane.setViewportView(connectionDetailsPanel);
                    connectionDetailsPanel.updateDetails((ConnectionLine) node);
                }
                add(sideScrollPane, BorderLayout.EAST);
            }
            revalidate();
            repaint();
        });
    }

    public NetworkPanel getNetworkPanel(){
        return networkPanel;
    }
}
