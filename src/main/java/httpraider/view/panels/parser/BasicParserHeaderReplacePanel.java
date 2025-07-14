package httpraider.view.panels.parser;

import httpraider.view.components.UI.UIutils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class BasicParserHeaderReplacePanel extends JPanel {
    private final ReplaceTableModel tableModel;
    private final JTable table;
    private final JButton addButton, removeButton;

    public BasicParserHeaderReplacePanel() {
        super(new BorderLayout());
        UIutils.setBorderLayoutGaps(this, 8, 5);

        JPanel mainPanel = new JPanel(new BorderLayout());
        TitledBorder border = BorderFactory.createTitledBorder("Match and Replace");
        Font font = UIManager.getFont("TitledBorder.font");
        border.setTitleFont(font.deriveFont(12f));
        setBorder(border);

        tableModel = new ReplaceTableModel();
        table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setPreferredWidth(130);
        table.getColumnModel().getColumn(1).setPreferredWidth(180);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(320, 60));

        addButton = new JButton("+");
        removeButton = new JButton("–");

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);

        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(mainPanel, BorderLayout.CENTER);

        addButton.addActionListener(e -> {
            tableModel.addRule(new ReplaceRule("", ""));
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
    public List<ReplaceRule> getRules() { return tableModel.getRules(); }
    public void setRules(List<ReplaceRule> rules) { tableModel.setRules(rules); }

    public static class ReplaceRule {
        private String match;
        private String replace;
        public ReplaceRule(String match, String replace) {
            this.match = match;
            this.replace = replace;
        }
        public String getMatch() { return match; }
        public void setMatch(String match) { this.match = match; }
        public String getReplace() { return replace; }
        public void setReplace(String replace) { this.replace = replace; }
    }

    private static class ReplaceTableModel extends AbstractTableModel {
        private final List<ReplaceRule> rules = new ArrayList<>();
        @Override public int getRowCount() { return rules.size(); }
        @Override public int getColumnCount() { return 2; }
        @Override public String getColumnName(int col) {
            switch (col) {
                case 0: return "Match";
                case 1: return "Replace";
                default: return "";
            }
        }
        @Override public Object getValueAt(int row, int col) {
            ReplaceRule r = rules.get(row);
            switch (col) {
                case 0: return r.getMatch();
                case 1: return r.getReplace();
                default: return null;
            }
        }
        @Override public boolean isCellEditable(int row, int col) { return true; }
        @Override public void setValueAt(Object value, int row, int col) {
            ReplaceRule r = rules.get(row);
            switch (col) {
                case 0: r.setMatch(value != null ? value.toString() : ""); break;
                case 1: r.setReplace(value != null ? value.toString() : ""); break;
            }
            fireTableRowsUpdated(row, row);
        }
        public void addRule(ReplaceRule rule) {
            rules.add(rule);
            fireTableRowsInserted(rules.size() - 1, rules.size() - 1);
        }
        public void removeRule(int row) {
            if (row >= 0 && row < rules.size()) {
                rules.remove(row);
                fireTableRowsDeleted(row, row);
            }
        }
        public List<ReplaceRule> getRules() {
            List<ReplaceRule> out = new ArrayList<>();
            for (ReplaceRule r : rules)
                out.add(new ReplaceRule(r.getMatch(), r.getReplace()));
            return out;
        }
        public void setRules(List<ReplaceRule> in) {
            rules.clear();
            if (in != null) for (ReplaceRule r : in)
                rules.add(new ReplaceRule(r.getMatch(), r.getReplace()));
            fireTableDataChanged();
        }
    }
}
