package httpraider.view.panels;

import javax.swing.*;
import java.awt.*;

public class ParserSettingsRowPanel extends JPanel {
    private final JTextField textField;
    private final JCheckBox checkBox;
    private final JButton removeButton;

    public ParserSettingsRowPanel(int fieldSize, boolean hasCheckbox, boolean checkboxDefault) {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setMaximumSize(new Dimension(3000, 36));
        setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        setBackground(Color.WHITE);

        add(Box.createHorizontalGlue());

        removeButton = new JButton("–");
        removeButton.setFocusPainted(false);
        removeButton.setPreferredSize(new Dimension(34, 34));
        add(removeButton);
        add(Box.createRigidArea(new Dimension(8,0)));

        textField = new JTextField(fieldSize);
        textField.setMaximumSize(new Dimension(250, 30));
        textField.setMinimumSize(new Dimension(100, 30));
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

    public JTextField getTextField() { return textField; }
    public JCheckBox getCheckBox() { return checkBox; }
    public JButton getRemoveButton() { return removeButton; }
}
