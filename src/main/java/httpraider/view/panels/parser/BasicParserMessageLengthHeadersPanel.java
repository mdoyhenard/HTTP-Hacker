package httpraider.view.panels.parser;

import httpraider.model.network.HttpParserModel;
import httpraider.view.components.UI.UIutils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class BasicParserMessageLengthHeadersPanel extends JPanel {
    private final RulesTableModel tableModel;
    private final JTable table;
    private final JButton addButton, removeButton, upButton, downButton;
    private Runnable chunkedStateChangedListener;

    public BasicParserMessageLengthHeadersPanel() {
        super(new BorderLayout());
        UIutils.setBorderLayoutGaps(this, 5);

        JPanel mainPanel = new JPanel(new BorderLayout());
        TitledBorder border = BorderFactory.createTitledBorder("Message-Length Headers");
        Font font = UIManager.getFont("TitledBorder.font");
        border.setTitleFont(font.deriveFont(12f));
        setBorder(border);

        tableModel = new RulesTableModel();
        table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setPreferredScrollableViewportSize(new Dimension(470, 90));

        table.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(new JCheckBox()));
        JComboBox<HttpParserModel.DuplicateHandling> dupCombo = new JComboBox<>(HttpParserModel.DuplicateHandling.values());
        table.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(dupCombo));
        table.getColumnModel().getColumn(0).setPreferredWidth(175);
        table.getColumnModel().getColumn(1).setPreferredWidth(65);
        table.getColumnModel().getColumn(2).setPreferredWidth(80);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

        table.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);

        JScrollPane scrollPane = new JScrollPane(table);

        addButton = new JButton("+");
        removeButton = new JButton("–");
        upButton = new JButton("↑");
        downButton = new JButton("↓");

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnPanel.add(addButton);
        btnPanel.add(removeButton);
        btnPanel.add(upButton);
        btnPanel.add(downButton);

        JLabel helpLabel = new JLabel("Specify header patterns to determine body length. Chunked overrides CL. If duplicate, pick first/last/error.");
        helpLabel.setFont(helpLabel.getFont().deriveFont(Font.ITALIC, 11f));
        helpLabel.setForeground(new Color(80, 80, 120));

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(btnPanel, BorderLayout.WEST);

        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        add(mainPanel);

        addButton.addActionListener(e -> {
            tableModel.addRule(new HttpParserModel.BodyLenHeaderRule("", false, HttpParserModel.DuplicateHandling.FIRST));
            int row = tableModel.getRowCount() - 1;
            table.editCellAt(row, 0);
            table.setRowSelectionInterval(row, row);
            table.requestFocusInWindow();
            updateChunkedPanel();
        });

        removeButton.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                tableModel.removeRule(row);
                updateChunkedPanel();
            }
        });

        upButton.addActionListener(e -> moveSelectedRow(-1));
        downButton.addActionListener(e -> moveSelectedRow(1));

        tableModel.setChunkedChangedListener(this::updateChunkedPanel);
    }

    public JTable getTable() { return table; }

    private void moveSelectedRow(int delta) {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int newRow = row + delta;
        if (newRow < 0 || newRow >= tableModel.getRowCount()) return;
        tableModel.swapRules(row, newRow);
        table.setRowSelectionInterval(newRow, newRow);
        updateChunkedPanel();
    }

    public List<HttpParserModel.BodyLenHeaderRule> getRules() {
        return tableModel.getRules();
    }

    public void setRules(List<HttpParserModel.BodyLenHeaderRule> rules) {
        tableModel.setRules(rules);
        updateChunkedPanel();
    }

    public boolean anyChunked() {
        return tableModel.anyChunked();
    }

    public void setChunkedStateChangedListener(Runnable listener) {
        this.chunkedStateChangedListener = listener;
    }

    private void updateChunkedPanel() {
        if (chunkedStateChangedListener != null) {
            chunkedStateChangedListener.run();
        }
    }

    private static class RulesTableModel extends AbstractTableModel {
        private final List<HttpParserModel.BodyLenHeaderRule> rules = new ArrayList<>();
        private Runnable chunkedChangedListener;

        @Override public int getRowCount() { return rules.size(); }
        @Override public int getColumnCount() { return 3; }
        @Override public String getColumnName(int col) {
            switch (col) {
                case 0: return "Pattern";
                case 1: return "Chunked";
                case 2: return "If Duplicate";
                default: return "";
            }
        }
        @Override public Object getValueAt(int row, int col) {
            HttpParserModel.BodyLenHeaderRule r = rules.get(row);
            switch (col) {
                case 0: return r.getPattern();
                case 1: return r.isChunked();
                case 2: return r.getDuplicateHandling();
                default: return null;
            }
        }
        @Override public boolean isCellEditable(int row, int col) { return true; }
        @Override public void setValueAt(Object value, int row, int col) {
            HttpParserModel.BodyLenHeaderRule r = rules.get(row);
            switch (col) {
                case 0: r.setPattern(value != null ? value.toString() : ""); break;
                case 1: r.setChunked(Boolean.TRUE.equals(value)); break;
                case 2: r.setDuplicateHandling((HttpParserModel.DuplicateHandling) value); break;
            }
            fireTableRowsUpdated(row, row);
            if (chunkedChangedListener != null) chunkedChangedListener.run();
        }
        public void addRule(HttpParserModel.BodyLenHeaderRule rule) {
            rules.add(rule);
            fireTableRowsInserted(rules.size() - 1, rules.size() - 1);
            if (chunkedChangedListener != null) chunkedChangedListener.run();
        }
        public void removeRule(int row) {
            if (row >= 0 && row < rules.size()) {
                rules.remove(row);
                fireTableRowsDeleted(row, row);
                if (chunkedChangedListener != null) chunkedChangedListener.run();
            }
        }
        public void swapRules(int a, int b) {
            if (a < 0 || b < 0 || a >= rules.size() || b >= rules.size()) return;
            HttpParserModel.BodyLenHeaderRule tmp = rules.get(a);
            rules.set(a, rules.get(b));
            rules.set(b, tmp);
            fireTableRowsUpdated(Math.min(a,b), Math.max(a,b));
            if (chunkedChangedListener != null) chunkedChangedListener.run();
        }
        public List<HttpParserModel.BodyLenHeaderRule> getRules() {
            List<HttpParserModel.BodyLenHeaderRule> out = new ArrayList<>();
            for (HttpParserModel.BodyLenHeaderRule r : rules)
                out.add(new HttpParserModel.BodyLenHeaderRule(r.getPattern(), r.isChunked(), r.getDuplicateHandling()));
            return out;
        }
        public void setRules(List<HttpParserModel.BodyLenHeaderRule> in) {
            rules.clear();
            if (in != null) for (HttpParserModel.BodyLenHeaderRule r : in)
                rules.add(new HttpParserModel.BodyLenHeaderRule(r.getPattern(), r.isChunked(), r.getDuplicateHandling()));
            fireTableDataChanged();
            if (chunkedChangedListener != null) chunkedChangedListener.run();
        }
        public boolean anyChunked() {
            for (HttpParserModel.BodyLenHeaderRule r : rules) if (r.isChunked()) return true;
            return false;
        }
        public void setChunkedChangedListener(Runnable listener) {
            this.chunkedChangedListener = listener;
        }
    }
}
