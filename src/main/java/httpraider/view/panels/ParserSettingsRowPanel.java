package httpraider.view.panels;

import httpraider.view.components.DragHandleLabel;

import javax.swing.*;
import java.awt.*;

public class ParserSettingsRowPanel extends JPanel {
    private final JTextField textField;
    private final JCheckBox checkBox;
    private final JButton removeButton;

    public ParserSettingsRowPanel(int fieldSize, boolean hasCheckbox, boolean checkboxDefault, boolean draggable) {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setMaximumSize(new Dimension(340, 36));
        setMinimumSize(new Dimension(310, 36));
        setPreferredSize(new Dimension(340, 36));
        setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        setBackground(Color.WHITE);

        if (draggable) {
            DragHandleLabel dragLabel = new DragHandleLabel();
            add(dragLabel);
            add(Box.createRigidArea(new Dimension(5,0)));
        }

        if (!hasCheckbox) {add(Box.createRigidArea(new Dimension(18,0)));}

        removeButton = new JButton("–");
        removeButton.setFocusPainted(false);
        removeButton.setPreferredSize(new Dimension(34, 34));
        add(removeButton);
        add(Box.createRigidArea(new Dimension(8,0)));

        textField = new JTextField(fieldSize);
        textField.setMaximumSize(new Dimension(90, 30));
        textField.setMinimumSize(new Dimension(70, 30));
        textField.setPreferredSize(new Dimension(90, 30));
        textField.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(textField);

        if (hasCheckbox) {
            add(Box.createRigidArea(new Dimension(8,0)));
            checkBox = new JCheckBox("chunked");
            checkBox.setSelected(checkboxDefault);
            checkBox.setBackground(Color.WHITE);
            add(checkBox);
        } else {
            checkBox = null;
        }
        add(Box.createHorizontalGlue());
    }

    public ParserSettingsRowPanel(int fieldSize, boolean hasCheckbox, boolean checkboxDefault) {
        this(fieldSize, hasCheckbox, checkboxDefault, false);
    }

    public JTextField getTextField() { return textField; }
    public JCheckBox getCheckBox() { return checkBox; }
    public JButton getRemoveButton() { return removeButton; }
}
