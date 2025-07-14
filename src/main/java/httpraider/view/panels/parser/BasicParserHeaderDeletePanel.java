package httpraider.view.panels.parser;

import httpraider.view.components.UI.UIutils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class BasicParserHeaderDeletePanel extends JPanel {
    private final DeleteTableModel tableModel;
    private final JTable table;
    private final JButton addButton, removeButton;

    public BasicParserHeaderDeletePanel() {
        super(new BorderLayout());
        UIutils.setBorderLayoutGaps(this, 8, 5);

        JPanel mainPanel = new JPanel(new BorderLayout());
        TitledBorder border = BorderFactory.createTitledBorder("Delete Header (Match)");
        Font font = UIManager.getFont("TitledBorder.font");
        border.setTitleFont(font.deriveFont(12f));
        setBorder(border);

        tableModel = new DeleteTableModel();
        table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setPreferredWidth(200);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(250, 60));

        addButton = new JButton("+");
        removeButton = new JButton("–");

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);

        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(mainPanel, BorderLayout.CENTER);

        addButton.addActionListener(e -> {
            tableModel.addRule("");
            int row = tableModel.getRowCount() - 1;
            table.editCellAt(row, 0);
            table.setRowSelectionInterval(row, row);
            table.requestFocusInWindow();
        });

        removeButton.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) tableModel.removeRule(row);
        });
    }

    public JTable getTable() { return table; }
    public List<String> getRules() { return tableModel.getRules(); }
    public void setRules(List<String> rules) { tableModel.setRules(rules); }

    private static class DeleteTableModel extends AbstractTableModel {
        private final List<String> rules = new ArrayList<>();
        @Override public int getRowCount() { return rules.size(); }
        @Override public int getColumnCount() { return 1; }
        @Override public String getColumnName(int col) { return "Match"; }
        @Override public Object getValueAt(int row, int col) { return rules.get(row); }
        @Override public boolean isCellEditable(int row, int col) { return true; }
        @Override public void setValueAt(Object value, int row, int col) {
            rules.set(row, value != null ? value.toString() : "");
            fireTableRowsUpdated(row, row);
        }
        public void addRule(String rule) {
            rules.add(rule);
            fireTableRowsInserted(rules.size() - 1, rules.size() - 1);
        }
        public void removeRule(int row) {
            if (row >= 0 && row < rules.size()) {
                rules.remove(row);
                fireTableRowsDeleted(row, row);
            }
        }
        public List<String> getRules() { return new ArrayList<>(rules); }
        public void setRules(List<String> list) {
            rules.clear();
            if (list != null) rules.addAll(list);
            fireTableDataChanged();
        }
    }
}
