package httpraider.view.panels.parser;

import httpraider.model.network.HttpParserModel;
import httpraider.view.panels.JSCodeEditorPanel;
import httpraider.view.components.UI.UIutils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.List;

public class RequestLineParserPanel extends JPanel {
    private final DelimiterTableModel delimiterTableModel;
    private final JTable delimiterTable;
    private final JButton addDelimiterButton, removeDelimiterButton;
    private final JPanel fromToPanel, decodingRangePanel;

    private final JCheckBox rewriteMethodCheckbox;
    private final JComboBox<String> fromMethodCombo;
    private final JTextField fromCustomField;
    private final JComboBox<String> toMethodCombo;
    private final JTextField toCustomField;

    private final JCheckBox decodeUrlCheckbox;
    private final JTextField urlDecodeFromField, urlDecodeToField;

    private final JComboBox<HttpParserModel.ForcedHttpVersion> versionCombo;
    private final JTextField customVersionField;

    private final JCheckBox useJsCheckbox;
    private final JSCodeEditorPanel jsEditorPanel;

    public static final int SETTINGS_PANEL_WIDTH = 300;

    private static final String[] HTTP_METHODS = new String[] {
            "GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS", "TRACE", "CONNECT", "PATCH", "Custom"
    };

    public RequestLineParserPanel() {
        super(new BorderLayout());

        UIutils.setBorderLayoutGaps(this, 5);

        JPanel delimitersTablePanel = new JPanel(new BorderLayout());
        TitledBorder border = BorderFactory.createTitledBorder("Request Line Delimiters");
        Font font = UIManager.getFont("TitledBorder.font");
        border.setTitleFont(font.deriveFont(12f));
        delimitersTablePanel.setBorder(border);

        setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JPanel settingsPanel = new JPanel();
        settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
        settingsPanel.setPreferredSize(new Dimension(SETTINGS_PANEL_WIDTH, 10));
        settingsPanel.setMaximumSize(new Dimension(SETTINGS_PANEL_WIDTH, Integer.MAX_VALUE));

        JPanel delimiterPanel = new JPanel(new BorderLayout());


        delimiterTableModel = new DelimiterTableModel();
        delimiterTable = new JTable(delimiterTableModel);
        delimiterTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        delimiterTable.getColumnModel().getColumn(0).setPreferredWidth(180);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        delimiterTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);

        JScrollPane delimiterScroll = new JScrollPane(delimiterTable);


        addDelimiterButton = new JButton("+");
        removeDelimiterButton = new JButton("–");
        JPanel delimiterBtnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        delimiterBtnPanel.add(addDelimiterButton);
        delimiterBtnPanel.add(removeDelimiterButton);

        delimiterPanel.add(delimiterScroll, BorderLayout.CENTER);
        delimiterPanel.add(delimiterBtnPanel, BorderLayout.SOUTH);

        JPanel rewritePanel = new JPanel();
        rewritePanel.setLayout(new BoxLayout(rewritePanel, BoxLayout.Y_AXIS));
        rewritePanel.setBorder(BorderFactory.createTitledBorder("Method Rewrite"));
        rewritePanel.add(Box.createVerticalStrut(8));

        rewriteMethodCheckbox = new JCheckBox("Rewrite method");
        fromMethodCombo = new JComboBox<>(HTTP_METHODS);
        fromMethodCombo.setSelectedIndex(fromMethodCombo.getItemCount()-1);
        fromCustomField = new JTextField(8);
        toMethodCombo = new JComboBox<>(HTTP_METHODS);
        toMethodCombo.setSelectedIndex(toMethodCombo.getItemCount()-1);
        toCustomField = new JTextField(8);

        rewriteMethodCheckbox.setAlignmentX(Component.CENTER_ALIGNMENT);
        rewritePanel.add(rewriteMethodCheckbox);

        fromToPanel = new JPanel(new FlowLayout());
        fromToPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        fromToPanel.add(new JLabel("From:"));
        //fromToPanel.add(fromMethodCombo);
        fromToPanel.add(fromCustomField);
        fromToPanel.add(new JLabel("To:"));
        //fromToPanel.add(toMethodCombo);
        fromToPanel.add(toCustomField);

        rewritePanel.add(fromToPanel);

        JPanel decodePanel = new JPanel();
        decodePanel.setLayout(new BoxLayout(decodePanel, BoxLayout.Y_AXIS));
        decodePanel.setBorder(BorderFactory.createTitledBorder("URL Decoding"));
        decodePanel.add(Box.createVerticalStrut(8));

        decodeUrlCheckbox = new JCheckBox("Decode URL before forwarding");
        decodeUrlCheckbox.setAlignmentX(Component.CENTER_ALIGNMENT);

        decodingRangePanel = new JPanel(new FlowLayout());
        decodingRangePanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        decodingRangePanel.add(new JLabel("Range:"));

        urlDecodeFromField = new JTextField("%21", 4);
        decodingRangePanel.add(urlDecodeFromField);

        decodingRangePanel.add(new JLabel("-"));

        urlDecodeToField = new JTextField("%7f", 4);
        decodingRangePanel.add(urlDecodeToField);

        decodePanel.add(decodeUrlCheckbox);
        decodePanel.add(decodingRangePanel);

        JPanel versionPanel = new JPanel();
        versionPanel.setLayout(new BoxLayout(versionPanel, BoxLayout.Y_AXIS));
        versionPanel.setBorder(BorderFactory.createTitledBorder("HTTP Version"));
        versionPanel.add(Box.createVerticalStrut(8));

        JPanel forceVersionPanel = new JPanel(new FlowLayout());
        forceVersionPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        versionCombo = new JComboBox<>(HttpParserModel.ForcedHttpVersion.values());
        customVersionField = new JTextField(10);
        customVersionField.setVisible(false);

        forceVersionPanel.add(new JLabel("Forward version: "));
        forceVersionPanel.add(versionCombo);
        versionPanel.add(forceVersionPanel);

        UIutils.setBorderLayoutGaps(delimitersTablePanel, 15, 10);
        delimitersTablePanel.add(delimiterPanel, BorderLayout.CENTER);

        settingsPanel.add(delimitersTablePanel);
        settingsPanel.add(Box.createVerticalStrut(8));
        settingsPanel.add(rewritePanel);
        settingsPanel.add(Box.createVerticalStrut(8));
        settingsPanel.add(decodePanel);
        settingsPanel.add(Box.createVerticalStrut(8));
        settingsPanel.add(versionPanel);
        settingsPanel.add(Box.createVerticalGlue());

        JPanel scriptingPanel = new JPanel();
        scriptingPanel.setBorder(BorderFactory.createTitledBorder("Advanced JavaScript Parser"));
        scriptingPanel.setLayout(new BorderLayout());

        useJsCheckbox = new JCheckBox("Use JS Parser");
        JPanel topBox = new JPanel();
        topBox.setLayout(new BoxLayout(topBox, BoxLayout.Y_AXIS));
        topBox.setOpaque(false);
        topBox.add(useJsCheckbox);
        topBox.add(Box.createVerticalStrut(8));

        jsEditorPanel = new JSCodeEditorPanel();
        jsEditorPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        jsEditorPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        scriptingPanel.add(topBox, BorderLayout.NORTH);
        scriptingPanel.add(jsEditorPanel, BorderLayout.CENTER);

        JSeparator verticalSeparator = new JSeparator(SwingConstants.VERTICAL);
        verticalSeparator.setPreferredSize(new Dimension(5, 10));
        verticalSeparator.setMaximumSize(new Dimension(5, Integer.MAX_VALUE));
        verticalSeparator.setForeground(Color.DARK_GRAY);
        verticalSeparator.setBackground(Color.DARK_GRAY);

        JPanel combinedPanel = new JPanel();
        combinedPanel.setLayout(new BoxLayout(combinedPanel, BoxLayout.X_AXIS));
        combinedPanel.add(settingsPanel);
        combinedPanel.add(Box.createHorizontalStrut(10));
        combinedPanel.add(verticalSeparator);
        combinedPanel.add(Box.createHorizontalStrut(10));
        combinedPanel.add(scriptingPanel);

        add(combinedPanel, BorderLayout.CENTER);

        useJsCheckbox.addActionListener(e -> updateJsEnabled());
        updateJsEnabled();

        addDelimiterButton.addActionListener(e -> delimiterTableModel.addDelimiter(""));
        removeDelimiterButton.addActionListener(e -> {
            int row = delimiterTable.getSelectedRow();
            if (row >= 0) delimiterTableModel.removeDelimiter(row);
        });

        rewriteMethodCheckbox.addActionListener(e -> updateRewriteVisibility());
        fromMethodCombo.addActionListener(e -> updateRewriteVisibility());
        toMethodCombo.addActionListener(e -> updateRewriteVisibility());

        decodeUrlCheckbox.addActionListener(e -> updateDecodeVisibility());

        updateRewriteVisibility();
        updateDecodeVisibility();
    }

    private void updateJsEnabled() {
        jsEditorPanel.setEnabled(useJsCheckbox.isSelected());
        jsEditorPanel.getTextPane().setEditable(useJsCheckbox.isSelected());
        jsEditorPanel.getTextPane().setBackground(useJsCheckbox.isSelected() ? Color.WHITE : new Color(245,245,245));
    }

    private void updateRewriteVisibility() {
        boolean enabled = rewriteMethodCheckbox.isSelected();

        for (Component component : fromToPanel.getComponents()){
            component.setEnabled(enabled);
        }

        revalidate();
        repaint();
    }

    private void updateDecodeVisibility() {
        boolean enabled = decodeUrlCheckbox.isSelected();
        for (Component component : decodingRangePanel.getComponents()){
            component.setEnabled(enabled);
        }
    }

    public List<String> getLineDelimiters() {
        return delimiterTableModel.getDelimiters();
    }
    public void setLineDelimiters(List<String> list) {
        delimiterTableModel.setDelimiters(list);
    }

    public boolean isRewriteMethodEnabled() {
        return rewriteMethodCheckbox.isSelected();
    }
    public void setRewriteMethodEnabled(boolean b) {
        rewriteMethodCheckbox.setSelected(b);
        updateRewriteVisibility();
    }
    public String getFromMethod() {
        if ("Custom".equals(fromMethodCombo.getSelectedItem())) {
            return fromCustomField.getText();
        }
        return (String) fromMethodCombo.getSelectedItem();
    }
    public void setFromMethod(String from) {
        setComboOrCustom(fromMethodCombo, fromCustomField, from);
        updateRewriteVisibility();
    }
    public String getToMethod() {
        if ("Custom".equals(toMethodCombo.getSelectedItem())) {
            return toCustomField.getText();
        }
        return (String) toMethodCombo.getSelectedItem();
    }
    public void setToMethod(String to) {
        setComboOrCustom(toMethodCombo, toCustomField, to);
        updateRewriteVisibility();
    }

    public boolean isDecodeUrlBeforeForwarding() {
        return decodeUrlCheckbox.isSelected();
    }
    public void setDecodeUrlBeforeForwarding(boolean b) {
        decodeUrlCheckbox.setSelected(b);
        updateDecodeVisibility();
    }
    public String getUrlDecodeFrom() {
        return urlDecodeFromField.getText();
    }
    public void setUrlDecodeFrom(String v) {
        urlDecodeFromField.setText(v);
    }
    public String getUrlDecodeTo() {
        return urlDecodeToField.getText();
    }
    public void setUrlDecodeTo(String v) {
        urlDecodeToField.setText(v);
    }

    public HttpParserModel.ForcedHttpVersion getForcedHttpVersion() {
        return (HttpParserModel.ForcedHttpVersion) versionCombo.getSelectedItem();
    }
    public void setForcedHttpVersion(HttpParserModel.ForcedHttpVersion v) {
        versionCombo.setSelectedItem(v != null ? v : HttpParserModel.ForcedHttpVersion.AUTO);
    }
    public String getCustomHttpVersion() {
        return customVersionField.getText();
    }
    public void setCustomHttpVersion(String v) {
        customVersionField.setText(v != null ? v : "");
    }

    public boolean isUseJsParser() { return useJsCheckbox.isSelected(); }
    public void setUseJsParser(boolean b) {
        useJsCheckbox.setSelected(b);
        updateJsEnabled();
    }
    public String getJsScript() { return jsEditorPanel.getCode(); }
    public void setJsScript(String code) {
        jsEditorPanel.setCode(code != null ? code : "");
    }

    public JTable getDelimiterTable() { return delimiterTable; }

    private void setComboOrCustom(JComboBox<String> combo, JTextField field, String value) {
        boolean found = false;
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).equalsIgnoreCase(value)) {
                combo.setSelectedIndex(i);
                field.setText("");
                found = true;
                break;
            }
        }
        if (!found) {
            combo.setSelectedItem("Custom");
            field.setText(value != null ? value : "");
        }
    }

    private static class DelimiterTableModel extends AbstractTableModel {
        private final java.util.List<String> delimiters = new java.util.ArrayList<>();
        @Override public int getRowCount() { return delimiters.size(); }
        @Override public int getColumnCount() { return 1; }
        @Override public String getColumnName(int column) { return "Delimiter"; }
        @Override public Object getValueAt(int row, int col) { return delimiters.get(row); }
        @Override public boolean isCellEditable(int row, int col) { return true; }
        @Override public void setValueAt(Object value, int row, int col) {
            delimiters.set(row, value != null ? value.toString() : "");
            fireTableRowsUpdated(row, row);
        }
        public void addDelimiter(String delimiter) {
            delimiters.add(delimiter);
            fireTableRowsInserted(delimiters.size() - 1, delimiters.size() - 1);
        }
        public void removeDelimiter(int row) {
            if (row >= 0 && row < delimiters.size()) {
                delimiters.remove(row);
                fireTableRowsDeleted(row, row);
            }
        }
        public java.util.List<String> getDelimiters() { return new java.util.ArrayList<>(delimiters); }
        public void setDelimiters(java.util.List<String> list) {
            delimiters.clear();
            if (list != null) delimiters.addAll(list);
            fireTableDataChanged();
        }
    }
}
