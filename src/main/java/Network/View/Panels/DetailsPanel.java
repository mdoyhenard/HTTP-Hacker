package Network.View.Panels;

import javax.swing.*;
import java.awt.*;

public class DetailsPanel extends JPanel {

    private JTextField vendorField;
    private JTextField typeField;
    private JTextField pathField;
    private JTextArea descriptionArea;

    public DetailsPanel(){
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JLabel vendorLabel = new JLabel("Vendor");
        vendorLabel.setFont(new Font("Arial", Font.BOLD, 13));
        vendorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(vendorLabel);
        add(Box.createRigidArea(new Dimension(0, 5)));
        vendorField = new JTextField(15);
        vendorField.setMaximumSize(vendorField.getPreferredSize());
        //vendorField.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        add(vendorField);
        add(Box.createRigidArea(new Dimension(0, 15)));

        JLabel typeLabel = new JLabel("Component Type");
        typeLabel.setFont(new Font("Arial", Font.BOLD, 13));
        typeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(typeLabel);
        add(Box.createRigidArea(new Dimension(0, 5)));
        typeField = new JTextField(15);
        typeField.setMaximumSize(typeField.getPreferredSize());
        //typeField.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        add(typeField);
        add(Box.createRigidArea(new Dimension(0, 10)));

        JLabel pathLabel = new JLabel("Path");
        pathLabel.setFont(new Font("Arial", Font.BOLD, 13));
        pathLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(pathLabel);
        add(Box.createRigidArea(new Dimension(0, 5)));
        pathField = new JTextField(15);
        pathField.setMaximumSize(pathField.getPreferredSize());
        //pathField.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        add(pathField);
        add(Box.createRigidArea(new Dimension(0, 15)));

        JLabel descriptionLabel = new JLabel("Description");
        descriptionLabel.setFont(new Font("Arial", Font.BOLD, 13));
        descriptionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(descriptionLabel);
        add(Box.createRigidArea(new Dimension(0, 5)));
        descriptionArea = new JTextArea(3, 15);
        descriptionArea.setMaximumSize(descriptionArea.getPreferredSize());
        descriptionArea.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        add(descriptionArea);
        add(Box.createRigidArea(new Dimension(0, 15)));
    }

    public String getVendorField() {
        return vendorField.getText();
    }

    public void setVendorField(String vendorField) {
        this.vendorField.setText(vendorField);
    }

    public String getTypeField() {
        return typeField.getText();
    }

    public void setTypeField(String typeField) {
        this.typeField.setText(typeField);
    }

    public String getPathField() {
        return pathField.getText();
    }

    public void setPathField(String pathField) {
        this.pathField.setText(pathField);
    }

    public String getDescriptionArea() {
        return descriptionArea.getText();
    }

    public void setDescriptionArea(String descriptionArea) {
        this.descriptionArea.setText(descriptionArea);
    }
}
