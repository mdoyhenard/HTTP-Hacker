package Network.View.Panels;

import Network.Connection;
import Network.NetworkComponent;
import Network.View.Nodes.*;
import RawRepeater.MsgLenHeader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class NetworkPanel extends JPanel {
    protected List<JComponent> nodes = new ArrayList<>();
    protected List<ConnectionLine> connections = new ArrayList<>();
    private Point lastDragPoint;
    private boolean backgroundDrag = false;
    private Point bgPressPoint = null;
    private final int DRAG_THRESHOLD = 5;
    private Selectionable selectedNode;
    private NetworkNode<?> connectStartNode;
    private NodeSelectionListener nodeSelectionListener;

    public NetworkPanel() {
        setLayout(null); // Absolute positioning.
        setBackground(new Color(252, 252, 255, 255)); // Light background.
        setPreferredSize(new Dimension(2000, 2000));

        // Add fixed client node.
        ClientNode client = new ClientNode();
        client.setBounds(50, 300, client.getPreferredSize().width, client.getPreferredSize().height);
        addNode(client);

        // Background mouse listener for panning and selection clearing.
        MouseAdapter bgListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                bgPressPoint = e.getPoint();
                backgroundDrag = false;
                lastDragPoint = e.getPoint();
                Component comp = getComponentAt(e.getPoint());
                // If right-click on background, show menu.
                if (SwingUtilities.isRightMouseButton(e) && (!(comp instanceof NetworkNode<?>))) {
                    showBackgroundContextMenu(e.getX(), e.getY(), e.getLocationOnScreen());
                }
            }
            @Override
            public void mouseDragged(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    Point current = e.getPoint();
                    if (!backgroundDrag && bgPressPoint != null &&
                            current.distance(bgPressPoint) > DRAG_THRESHOLD) {
                        backgroundDrag = true;
                    }
                    int dx = current.x - lastDragPoint.x;
                    int dy = current.y - lastDragPoint.y;
                    for (Component comp : getComponents()) {
                        Point loc = comp.getLocation();
                        comp.setLocation(loc.x + dx, loc.y + dy);
                    }
                    lastDragPoint = current;
                    repaint();
                }
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                // On background click (no drag), clear selection.
                if (!backgroundDrag && SwingUtilities.isLeftMouseButton(e)) {
                    connectStartNode = null;
                    Component comp = getComponentAt(e.getPoint());
                    if (comp == null || comp == NetworkPanel.this) {
                        setSelectedNode(null);
                    }
                }
            }
        };
        addMouseListener(bgListener);
        addMouseMotionListener(bgListener);
    }

    public void addNode(JComponent node) {
        nodes.add(node);
        add(node);
        revalidate();
        repaint();
        installNodeListeners(node);
    }

    private void installNodeListeners(JComponent node) {
        // For non‑client nodes, add dragging.
        if (node instanceof ClientNode) {
            ClientDragListener dragListener = new ClientDragListener();
            node.addMouseListener(dragListener);
            node.addMouseMotionListener(dragListener);
        }
        else if (node instanceof NetworkNode<?>) {
            NodeDragListener dragListener = new NodeDragListener();
            node.addMouseListener(dragListener);
            node.addMouseMotionListener(dragListener);

            node.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        showNodeContextMenu((NetworkNode<?>)node);
                        e.consume();
                    }
                }
            });
        }
    }

    // Internal listener to differentiate click vs. drag on nodes.
    private class NodeDragListener extends MouseAdapter {
        private Point offset;
        private boolean dragging = false;

        @Override
        public void mousePressed(MouseEvent e) {
            offset = e.getPoint();
            dragging = false;
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            dragging = true;
            NetworkNode node = (NetworkNode) e.getSource();
            Point current = node.getLocation();
            int newX = current.x + e.getX() - offset.x;
            int newY = current.y + e.getY() - offset.y;
            node.setLocation(newX, newY);
            repaint();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (!dragging && SwingUtilities.isLeftMouseButton(e)) {
                NetworkNode node = (NetworkNode) e.getSource();
                if (connectStartNode != null) {
                    boolean exists = false;
                    for (ConnectionLine ll : connections){
                        if (ll.connectsNodes(connectStartNode, node)) exists = true;
                    }
                    if (!exists && !node.equals(connectStartNode)) {
                        ConnectionLine line = new ConnectionLine(connectStartNode, node, NetworkPanel.this);
                        connections.add(line);
                        add(line);
                    }
                    connectStartNode = null;
                } else {
                    toggleSelection(node);
                }
                revalidate();
                repaint();
            }
        }
    }


    // Internal listener to differentiate click vs. drag on nodes.
    private class ClientDragListener extends MouseAdapter {
        private Point offset;
        private boolean dragging = false;

        @Override
        public void mousePressed(MouseEvent e) {
            offset = e.getPoint();
            dragging = false;
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            dragging = true;
            NetworkNode node = (NetworkNode) e.getSource();
            Point current = node.getLocation();
            int newX = current.x + e.getX() - offset.x;
            int newY = current.y + e.getY() - offset.y;
            node.setLocation(newX, newY);
            repaint();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (!dragging && SwingUtilities.isLeftMouseButton(e)) {
                NetworkNode node = (NetworkNode) e.getSource();
                if (connectStartNode != null) {
                    if (!node.equals(connectStartNode)) {
                        ConnectionLine line = new ConnectionLine(connectStartNode, node, NetworkPanel.this);
                        connections.add(line);
                        add(line);
                        connectStartNode = null;
                    }
                    revalidate();
                    repaint();
                }
            }
        }
    }

    public void toggleSelection(Selectionable node) {
        if (node == null || (node.equals(selectedNode) && node.isActive())) {
            if (node != null) node.setActive(false);
            setSelectedNode(null);
        } else {
            for (JComponent n : nodes) {
                ((Selectionable) n).setActive(false);
            }
            for (JComponent n : connections) {
                ((Selectionable) n).setActive(false);
            }
            node.setActive(true);
            setSelectedNode(node);
        }
        repaint();
    }

    private void setSelectedNode(Selectionable node) {
        selectedNode = node;
        if (selectedNode == null) {
            for (JComponent n : nodes) {
                ((Selectionable)n).setActive(false);
            }
            for (JComponent n : connections) {
                ((Selectionable)n).setActive(false);
            }
        }
        if (nodeSelectionListener != null) {
            nodeSelectionListener.nodeSelected(selectedNode);
        }
        repaint();
    }

    public void setNodeSelectionListener(NodeSelectionListener listener) {
        this.nodeSelectionListener = listener;
    }

    private void showBackgroundContextMenu(int x, int y, Point screenLocation) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem addProxy = new JMenuItem("Add Proxy");
        addProxy.addActionListener(e -> {
            ProxyNode proxy = new ProxyNode();
            proxy.getComponent().getParser().setHeadersEnd("\\r\\n\\r\\n");
            proxy.getComponent().getParser().addMessageLengths(new MsgLenHeader(true, true, "\\r\\n(?i)Transfer-Encoding: *chunked *\\r\\n"));
            proxy.getComponent().getParser().addMessageLengths(new MsgLenHeader("\\r\\n(?i)Content-Length: *<int> *\\r\\n"));
            proxy.setBounds(x, y, proxy.getPreferredSize().width, proxy.getPreferredSize().height);
            addNode(proxy);
        });
        JMenuItem addServer = new JMenuItem("Add Server");
        addServer.addActionListener(e -> {
            ServerNode server = new ServerNode();
            server.setBounds(x, y, server.getPreferredSize().width, server.getPreferredSize().height);
            addNode(server);
        });
        menu.add(addProxy);
        menu.add(addServer);
        menu.show(this, x, y);
    }

    private void showNodeContextMenu(NetworkNode<?> node) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem deleteItem = new JMenuItem("Delete");
        deleteItem.addActionListener(e -> {
            remove(node);
            nodes.remove(node);
            if (node.equals(selectedNode)) {
                setSelectedNode(null);
            }
            List<ConnectionLine> updatedLines = new ArrayList<>();
            for (ConnectionLine line : connections){
                if (line.connectsNode(node)) remove(line);
                else updatedLines.add(line);
            }
            connections = updatedLines;
            revalidate();
            repaint();
        });
        if (node instanceof NetworkNode<?>) {
            JMenuItem connectItem = new JMenuItem("Connect");
            connectItem.addActionListener(e -> {
                connectStartNode = (NetworkNode<?>)node;
            });
            menu.add(deleteItem);
            menu.add(connectItem);
            menu.show(node, 0, node.getHeight());
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
    }

    public interface NodeSelectionListener {
        void nodeSelected(Selectionable node);
    }

}