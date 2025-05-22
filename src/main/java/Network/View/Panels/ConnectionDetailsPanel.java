package Network.View.Panels;

import Network.Connection;
import Network.View.Nodes.ConnectionLine;

import javax.swing.*;
import java.awt.*;

public class ConnectionDetailsPanel extends JPanel {
    private JCheckBox tlsField;
    private JCheckBox pipeliningField;
    private JTextArea descriptionArea;
    private Connection connection;
    private ConnectionLine currentLine;

    public ConnectionDetailsPanel(NetworkPanel networkPanel) {
        // Simple BorderLayout with some spacing
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- 1) Top: General Data Form ---
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 2: Vendor
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("TLS:"), gbc);
        gbc.gridx = 1;
        tlsField = new JCheckBox();
        formPanel.add(tlsField, gbc);

        // Row 3: Type
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Pipelining:"), gbc);
        gbc.gridx = 1;
        pipeliningField = new JCheckBox();
        formPanel.add(pipeliningField, gbc);

        // Row 5: Description
        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1;
        descriptionArea = new JTextArea(5, 20);
        JScrollPane descScroll = new JScrollPane(descriptionArea);
        formPanel.add(descScroll, gbc);

        add(formPanel, BorderLayout.NORTH);

        // --- 2) Center: Action Buttons (vertically aligned & smaller) ---
        JPanel centerWrapper = new JPanel();
        centerWrapper.setLayout(new BoxLayout(centerWrapper, BoxLayout.Y_AXIS));

        // We can define a smaller dimension for the buttons
        Dimension buttonSize = new Dimension(160, 30);

        // Add some top spacing
        centerWrapper.add(Box.createVerticalGlue());

        JButton sampleBtn = createCenteredButton("Delete", buttonSize);
        centerWrapper.add(sampleBtn);
        centerWrapper.add(Box.createVerticalStrut(10));
        sampleBtn.addActionListener(e ->{
            networkPanel.remove(currentLine);
            networkPanel.toggleSelection(null);
            networkPanel.repaint();
        });

        // Add some bottom spacing
        centerWrapper.add(Box.createVerticalGlue());

        add(centerWrapper, BorderLayout.CENTER);

        // --- 3) Bottom: Save/Cancel (side by side) ---
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton saveBtn = new JButton("Save");
        saveBtn.addActionListener(e -> {
            connection.setTls(tlsField.isSelected());
            connection.setPipelining(pipeliningField.isSelected());
            connection.setDescription(descriptionArea.getText());
        });
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e->{
            connection = null;
            currentLine.setActive(false);
        });
        bottomPanel.add(saveBtn);
        bottomPanel.add(cancelBtn);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    // Helper to create a button with a fixed size, centered alignment
    private JButton createCenteredButton(String text, Dimension size) {
        JButton btn = new JButton(text);
        btn.setMaximumSize(size);
        btn.setPreferredSize(size);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        return btn;
    }

    // Called by the network panel when a node is selected
    public void updateDetails(ConnectionLine line) {
        if (line != null) {
            currentLine = line;
            connection = line.getConnection();
            tlsField.setSelected(connection.isTls());
            pipeliningField.setSelected(connection.isPipelining());
            descriptionArea.setText(connection.getDescription());
        }
        else {
            connection = null;
            currentLine = null;
        }
    }
}
