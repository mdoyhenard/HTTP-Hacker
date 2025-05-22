package Network.View.Nodes;

import Network.Connection;
import Network.View.Panels.NetworkPanel;
import Network.View.Panels.NetworkPanelContainer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Line2D;

public class ConnectionLine extends JComponent implements Selectionable {

    private boolean active = false;
    private NetworkNode<?> startNode;
    private NetworkNode<?> endNode;
    private Connection connection;
    private Line2D.Double line;
    private static final double TOLERANCE = 20.0;
    private static final int MARGIN = 15;

    public ConnectionLine(NetworkNode<?> startNode, NetworkNode<?> endNode, NetworkPanel networkPanel) {
        // Get start and end points (assumed in parent's coordinate space)
        this.startNode = startNode;
        this.endNode = endNode;

        updatePosition();

        // Create the connection object as before.
        connection = new Connection(startNode.getComponent(), endNode.getComponent());

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (line.ptSegDist(e.getPoint()) <= TOLERANCE) {
                    active = !active;
                    networkPanel.toggleSelection(active ? ConnectionLine.this : null);
                    repaint();
                }
                else {
                    active = false;
                    networkPanel.toggleSelection(null);
                    repaint();
                }
            }
        });
    }

    public void updatePosition(){
        Rectangle startBounds = startNode.getBounds();
        Rectangle endBounds   = endNode.getBounds();
        Point startCenter = new Point(startBounds.x + startBounds.width/2,
                startBounds.y + startBounds.height/2);
        Point endCenter   = new Point(endBounds.x + endBounds.width/2,
                endBounds.y + endBounds.height/2);

        // Determine the top-left coordinate and the size of the bounding box.
        int x = Math.min(startCenter.x, endCenter.x);
        int y = Math.min(startCenter.y, endCenter.y);
        int width = Math.max(Math.abs(endCenter.x - startCenter.x), 5);
        int height = Math.max(Math.abs(endCenter.y - startCenter.y), 5);

        // Set the bounds for this component (with a margin to ensure the stroke is fully visible).
        setBounds(x - MARGIN/2, y - MARGIN/2, width + MARGIN, height + MARGIN);

        // Adjust the line coordinates relative to this component’s coordinate system.
        int adjustedStartX = startCenter.x - (x - MARGIN/2);
        int adjustedStartY = startCenter.y - (y - MARGIN/2);
        int adjustedEndX   = endCenter.x   - (x - MARGIN/2);
        int adjustedEndY   = endCenter.y   - (y - MARGIN/2);

        line = new Line2D.Double(adjustedStartX, adjustedStartY, adjustedEndX, adjustedEndY);

        // Update the layout and redraw the component.
        revalidate();
        repaint();
    }

    public Connection getConnection() {
        return connection;
    }

    @Override
    public Dimension getPreferredSize() {
        // Return the current size (which was set in the constructor)
        return getSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        // Use a thicker stroke when drawing the line.
        g2.setStroke(new BasicStroke(4));
        g2.setColor(active ? Color.RED : Color.BLACK);

        updatePosition();
        g2.draw(line);
    }


    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public Line2D.Double getLine() {
        return line;
    }

    public void setLine(Line2D.Double line) {
        this.line = line;
        repaint();
    }

    public void deleteConnection(){
        // Implement deletion logic if needed.
    }

    public boolean connectsNode(NetworkNode<?> node){
        return (startNode == node || endNode == node);
    }

    public boolean connectsNodes(NetworkNode<?> nodeA, NetworkNode<?> nodeB){
        return ((startNode == nodeA && endNode == nodeB) || (startNode == nodeB && endNode == nodeA));
    }
}
