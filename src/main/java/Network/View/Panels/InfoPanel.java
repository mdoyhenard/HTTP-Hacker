package Network.View.Panels;

import Network.NetworkComponent;
import Network.View.Nodes.NetworkNode;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class InfoPanel extends JPanel {
    private NetworkComponent comp;
    private NetworkNode currentNode;
    private CollapsiblePanel section1;
    private CollapsiblePanel section2;
    private CollapsiblePanel section3;
    private CollapsiblePanel section4;
    private DetailsPanel detailsPanel;
    private HttpParserPanel parserPanel;

    public InfoPanel(NetworkPanel networkPanel) {
        // Simple BorderLayout with some spacing
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        detailsPanel = new DetailsPanel();
        section1 = new CollapsiblePanel("Details", detailsPanel);
        section1.setSwitchVisible(false);
        add(section1);

        section2 = new CollapsiblePanel("HTTP Parser");
        section2.setSwitchVisible(true);
        add(section2);

        section3 = new CollapsiblePanel("Forwarding");
        section3.setSwitchVisible(true);
        add(section3);

        section4 = new CollapsiblePanel("Response Samples");
        section4.setSwitchVisible(false);
        add(section4);

        add(Box.createRigidArea(new Dimension(0, 20)));


        // --- 3) Bottom: Save/Cancel (side by side) ---
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton saveBtn = new JButton("Save");
        saveBtn.addActionListener(e -> {
            comp.setVendor(detailsPanel.getVendorField());
            comp.setType(detailsPanel.getTypeField());
            comp.setBasePath(detailsPanel.getPathField());
            comp.setDescription(detailsPanel.getDescriptionArea());
            parserPanel.saveParser(comp.getParser());
            JOptionPane.showMessageDialog(InfoPanel.this, "Configuration saved succesfully");
        });
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e->{
            comp = null;
            currentNode.setActive(false);
            networkPanel.toggleSelection(null);
        });
        bottomPanel.add(saveBtn);
        bottomPanel.add(cancelBtn);

        add(bottomPanel);
    }

    // Called by the network panel when a node is selected
    public void updateDetails(NetworkNode node) {
        if (node != null) {
            section1.collapse();
            section2.collapse();
            currentNode = node;
            comp = node.getComponent();
            detailsPanel.setVendorField(comp.getVendor());
            detailsPanel.setTypeField(comp.getType());
            detailsPanel.setPathField(comp.getBasePath());
            detailsPanel.setDescriptionArea(comp.getDescription());
            section2.clear();
            parserPanel = new HttpParserPanel(comp.getParser());
            section2.addComponent(parserPanel);
        }
        else {
            comp = null;
            section2.clear();
            parserPanel = null;
            currentNode = null;
        }
    }


    public void setParserAction(ActionListener action){
        section2.setSwitchActionListener(action);
    }
}
