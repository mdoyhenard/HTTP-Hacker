package httpraider.view.panels.parser;

import httpraider.view.components.UI.UIutils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class BasicParserHeaderLineEndingsPanel extends JPanel {
    private final EndingsTableModel tableModel;
    private final JTable table;
    private final JScrollPane scrollPane;
    private final JButton addButton;
    private final JButton removeButton;

    public BasicParserHeaderLineEndingsPanel(int maxRows) {
        super(new BorderLayout());
        UIutils.setBorderLayoutGaps(this, 10);

        JPanel mainPanel = new JPanel(new BorderLayout());

        TitledBorder border = BorderFactory.createTitledBorder("Header Line Endings");
        Font font = UIManager.getFont("TitledBorder.font");
        border.setTitleFont(font.deriveFont(12f));
        setBorder(border);

        JPanel tableHolder = new JPanel(new BorderLayout());
        tableHolder.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        tableModel = new EndingsTableModel();
        table = new JTable(tableModel);
        table.setRowHeight(22);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setPreferredWidth(100);
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);

        int preferredHeight = table.getRowHeight() * maxRows + table.getTableHeader().getPreferredSize().height;
        scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(100, preferredHeight));
        scrollPane.setMaximumSize(scrollPane.getPreferredSize());
        scrollPane.setMinimumSize(scrollPane.getPreferredSize());
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

        tableHolder.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        addButton = new JButton("+");
        removeButton = new JButton("–");
        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        mainPanel.add(tableHolder, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(mainPanel, BorderLayout.CENTER);

        addButton.addActionListener(e -> {
            tableModel.addPattern("");
            int row = tableModel.getRowCount() - 1;
            table.editCellAt(row, 0);
            table.setRowSelectionInterval(row, row);
            table.requestFocusInWindow();
        });

        removeButton.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) tableModel.removePattern(row);
        });
    }

    public JTable getTable() { return table; }
    public List<String> getLineEndings() { return tableModel.getPatterns(); }
    public void setLineEndings(List<String> patterns) { tableModel.setPatterns(patterns); }

    private static class EndingsTableModel extends AbstractTableModel {
        private final List<String> patterns = new ArrayList<>();
        @Override public int getRowCount() { return patterns.size(); }
        @Override public int getColumnCount() { return 1; }
        @Override public String getColumnName(int column) { return "Pattern"; }
        @Override public Object getValueAt(int row, int col) { return patterns.get(row); }
        @Override public boolean isCellEditable(int row, int col) { return true; }
        @Override public void setValueAt(Object value, int row, int col) {
            patterns.set(row, value != null ? value.toString() : "");
            fireTableRowsUpdated(row, row);
        }
        public void addPattern(String pattern) {
            patterns.add(pattern);
            fireTableRowsInserted(patterns.size() - 1, patterns.size() - 1);
        }
        public void removePattern(int row) {
            if (row >= 0 && row < patterns.size()) {
                patterns.remove(row);
                fireTableRowsDeleted(row, row);
            }
        }
        public List<String> getPatterns() { return new ArrayList<>(patterns); }
        public void setPatterns(List<String> list) {
            patterns.clear();
            if (list != null) patterns.addAll(list);
            fireTableDataChanged();
        }
    }
}
