package httpraider.view.panels.parser;

import httpraider.view.components.UI.UIutils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class BasicParserChunkedEndingsPanel extends JPanel {
    private final EndingsTableModel tableModel;
    private final JTable table;
    private final JButton addButton;
    private final JButton removeButton;

    public BasicParserChunkedEndingsPanel() {
        super(new BorderLayout());
        UIutils.setBorderLayoutGaps(this, 10, 5);

        JPanel mainPanel = new JPanel(new BorderLayout());

        TitledBorder border = BorderFactory.createTitledBorder("Chunk-Line Endings");
        Font font = UIManager.getFont("TitledBorder.font");
        border.setTitleFont(font.deriveFont(12f));
        setBorder(border);

        tableModel = new EndingsTableModel();
        table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setPreferredScrollableViewportSize(new Dimension(220, 90));
        table.getColumnModel().getColumn(0).setPreferredWidth(190);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);

        JScrollPane scrollPane = new JScrollPane(table);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addButton = new JButton("+");
        removeButton = new JButton("–");
        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);

        JLabel helpLabel = new JLabel("e.g. \\r\\n. Used to split chunked encoding segments.");
        helpLabel.setFont(helpLabel.getFont().deriveFont(Font.ITALIC, 11f));

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(buttonPanel, BorderLayout.WEST);

        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
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

    public List<String> getChunkedEndings() {
        return tableModel.getPatterns();
    }

    public void setChunkedEndings(List<String> patterns) {
        tableModel.setPatterns(patterns);
    }

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
