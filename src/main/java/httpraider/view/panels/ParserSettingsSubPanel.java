package httpraider.view.panels;

import httpraider.model.network.BodyLenHeader;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class ParserSettingsSubPanel extends JPanel {
    private final JPanel rowsPanel;
    private final JScrollPane scrollPane;
    private final JButton addButton;
    private final boolean hasCheckbox;
    private final int fieldSize;
    private final boolean checkboxDefault;
    private final List<ParserSettingsRowPanel> rowPanels = new ArrayList<>();

    public ParserSettingsSubPanel(String title, boolean hasCheckbox, int fieldSize, boolean checkboxDefault, int preferredWidth, Border border) {
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

        rowsPanel = new JPanel();
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
    }

    public void addRow() {
        ParserSettingsRowPanel row = new ParserSettingsRowPanel(fieldSize, hasCheckbox, checkboxDefault);
        rowPanels.add(row);
        rowsPanel.add(row);
        rowsPanel.revalidate();
        rowsPanel.repaint();
        rowsPanel.setBackground(Color.WHITE);
    }

    public void removeRow(ParserSettingsRowPanel row) {
        rowPanels.remove(row);
        rowsPanel.remove(row);
        rowsPanel.revalidate();
        rowsPanel.repaint();
        rowsPanel.setBackground(Color.WHITE);
    }

    public List<ParserSettingsRowPanel> getRows() {
        return new ArrayList<>(rowPanels);
    }

    public JButton getAddButton() { return addButton; }

    public void setAddButtonListener(ActionListener l) {
        for (ActionListener old : addButton.getActionListeners())
            addButton.removeActionListener(old);
        addButton.addActionListener(l);
    }

    public void setRemoveButtonListener(ParserSettingsRowPanel row, ActionListener l) {
        for (ActionListener old : row.getRemoveButton().getActionListeners())
            row.getRemoveButton().removeActionListener(old);
        row.getRemoveButton().addActionListener(l);
    }

    public void clearRows() {
        rowPanels.clear();
        rowsPanel.removeAll();
        rowsPanel.revalidate();
        rowsPanel.repaint();
    }

    // Persistency methods:
    public java.util.List<String> getAllRowTexts() {
        java.util.List<String> values = new java.util.ArrayList<>();
        for (ParserSettingsRowPanel row : rowPanels) {
            values.add(row.getTextField().getText());
        }
        return values;
    }

    public java.util.List<BodyLenHeader> getAllBodyLenHeaders() {
        java.util.List<BodyLenHeader> list = new java.util.ArrayList<>();
        for (ParserSettingsRowPanel row : rowPanels) {
            String pattern = row.getTextField().getText();
            boolean chunked = row.getCheckBox() != null && row.getCheckBox().isSelected();
            list.add(new BodyLenHeader(pattern, chunked));
        }
        return list;
    }
}
