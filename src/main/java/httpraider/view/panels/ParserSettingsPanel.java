package httpraider.view.panels;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class ParserSettingsPanel extends JPanel {
    private final SettingsSubPanel headersEndPanel;
    private final SettingsSubPanel headerSplittingPanel;
    private final SettingsSubPanel messageLengthPanel;

    public ParserSettingsPanel() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

        Border panelBorder = new MatteBorder(1, 1, 1, 1, new Color(180,180,180));

        headersEndPanel = new SettingsSubPanel(
                "Headers-End Sequence", false, 16, false, 220, panelBorder);
        headerSplittingPanel = new SettingsSubPanel(
                "Header-Splitting Sequence", false, 16, false, 220, panelBorder);
        messageLengthPanel = new SettingsSubPanel(
                "Message-Length Pattern", true, 30, true, 320, panelBorder);

        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;

        // Panel 1: leftmost, 10px right gap
        c.gridx = 0;
        c.weightx = 0.8;
        c.insets = new Insets(0, 0, 0, 10);
        add(headersEndPanel, c);

        // Panel 2: 10px right gap
        c.gridx = 1;
        c.weightx = 0.8;
        c.insets = new Insets(0, 0, 0, 10);
        add(headerSplittingPanel, c);

        // Panel 3: rightmost, no gap after
        c.gridx = 2;
        c.weightx = 1.2;
        c.insets = new Insets(0, 0, 0, 0);
        add(messageLengthPanel, c);

        // Add 10px vertical space before test/save bar
        GridBagConstraints sep = new GridBagConstraints();
        sep.gridy = 1;
        sep.gridx = 0;
        sep.gridwidth = 3;
        sep.fill = GridBagConstraints.HORIZONTAL;
        sep.weighty = 0;
        add(Box.createRigidArea(new Dimension(0, 10)), sep);
    }

    public SettingsSubPanel getHeadersEndPanel() { return headersEndPanel; }
    public SettingsSubPanel getHeaderSplittingPanel() { return headerSplittingPanel; }
    public SettingsSubPanel getMessageLengthPanel() { return messageLengthPanel; }

    public static class SettingsSubPanel extends JPanel {
        private final JPanel rowsPanel;
        private final JScrollPane scrollPane;
        private final JButton addButton;
        private final boolean hasCheckbox;
        private final int fieldSize;
        private final boolean checkboxDefault;
        private final List<RowPanel> rowPanels = new ArrayList<>();

        public SettingsSubPanel(String title, boolean hasCheckbox, int fieldSize, boolean checkboxDefault, int preferredWidth, Border border) {
            this.hasCheckbox = hasCheckbox;
            this.fieldSize = fieldSize;
            this.checkboxDefault = checkboxDefault;

            setLayout(new BorderLayout());
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                    border,
                    BorderFactory.createTitledBorder(
                            BorderFactory.createEmptyBorder(), title,
                            TitledBorder.CENTER, TitledBorder.TOP, getFont().deriveFont(Font.BOLD, 15f)))
            );
            setPreferredSize(new Dimension(preferredWidth, 200));
            setMinimumSize(new Dimension(preferredWidth, 150));
            setMaximumSize(new Dimension(preferredWidth + 40, 1000));

            rowsPanel = new JPanel() {
                @Override
                public void add(Component comp, Object constraints) {
                    super.add(comp, constraints);
                    setBackground(Color.WHITE);
                }
                @Override
                public void remove(Component comp) {
                    super.remove(comp);
                    setBackground(Color.WHITE);
                }
            };
            rowsPanel.setLayout(new BoxLayout(rowsPanel, BoxLayout.Y_AXIS));
            rowsPanel.setBackground(Color.WHITE);

            scrollPane = new JScrollPane(rowsPanel);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            scrollPane.setPreferredSize(new Dimension(preferredWidth, 200));
            scrollPane.setMinimumSize(new Dimension(preferredWidth, 150));
            scrollPane.getViewport().setBackground(Color.WHITE);
            add(scrollPane, BorderLayout.CENTER);

            addButton = new JButton("+");
            addButton.setFocusPainted(false);
            addButton.setPreferredSize(new Dimension(34, 34));
            JPanel addPanel = new JPanel();
            addPanel.setLayout(new BoxLayout(addPanel, BoxLayout.X_AXIS));
            addPanel.setOpaque(false);
            addPanel.add(Box.createHorizontalGlue());
            addPanel.add(addButton);
            addPanel.add(Box.createHorizontalGlue());
            add(addPanel, BorderLayout.SOUTH);

            addButton.addActionListener(e -> addRow());
        }

        public void addRow() {
            RowPanel row = new RowPanel(fieldSize, hasCheckbox, checkboxDefault);
            rowPanels.add(row);
            rowsPanel.add(row);
            rowsPanel.revalidate();
            rowsPanel.repaint();
            rowsPanel.setBackground(Color.WHITE);
        }

        public void removeRow(RowPanel row) {
            rowPanels.remove(row);
            rowsPanel.remove(row);
            rowsPanel.revalidate();
            rowsPanel.repaint();
            rowsPanel.setBackground(Color.WHITE);
        }

        public List<RowPanel> getRows() {
            return new ArrayList<>(rowPanels);
        }

        public JButton getAddButton() { return addButton; }

        public static class RowPanel extends JPanel {
            private final JTextField textField;
            private final JCheckBox checkBox;
            private final JButton removeButton;

            public RowPanel(int fieldSize, boolean hasCheckbox, boolean checkboxDefault) {
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

                removeButton.addActionListener(e -> {
                    Container parent = getParent();
                    if (parent instanceof JPanel) {
                        JPanel p = (JPanel) parent;
                        if (p.getParent().getParent().getParent() instanceof SettingsSubPanel) {
                            SettingsSubPanel panel = (SettingsSubPanel) p.getParent().getParent().getParent();
                            panel.removeRow(this);
                        }
                    }
                });
            }

            public JTextField getTextField() { return textField; }
            public JCheckBox getCheckBox() { return checkBox; }
            public JButton getRemoveButton() { return removeButton; }
        }
    }
}
