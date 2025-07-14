package httpraider.view.panels.parser;

import httpraider.view.components.UI.UIutils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class BasicParserHeaderAddPanel extends JPanel {
    private final AddTableModel tableModel;
    private final JTable table;
    private final JButton addButton, removeButton;

    public BasicParserHeaderAddPanel() {
        super(new BorderLayout());
        UIutils.setBorderLayoutGaps(this, 8, 5);

        JPanel mainPanel = new JPanel(new BorderLayout());
        TitledBorder border = BorderFactory.createTitledBorder("Add Header");
        Font font = UIManager.getFont("TitledBorder.font");
        border.setTitleFont(font.deriveFont(12f));
        setBorder(border);

        tableModel = new AddTableModel();
        table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setPreferredWidth(340);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(340, 60));

        addButton = new JButton("+");
        removeButton = new JButton("–");

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);

        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(mainPanel, BorderLayout.CENTER);

        addButton.addActionListener(e -> {
            tableModel.addHeader("");
            int row = tableModel.getRowCount() - 1;
            table.editCellAt(row, 0);
            table.setRowSelectionInterval(row, row);
            table.requestFocusInWindow();
        });

        removeButton.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) tableModel.removeHeader(row);
        });
    }

    public JTable getTable() { return table; }
    public List<String> getHeaders() { return tableModel.getHeaders(); }
    public void setHeaders(List<String> headers) { tableModel.setHeaders(headers); }

    private static class AddTableModel extends AbstractTableModel {
        private final List<String> headers = new ArrayList<>();
        @Override public int getRowCount() { return headers.size(); }
        @Override public int getColumnCount() { return 1; }
        @Override public String getColumnName(int col) { return "New Header"; }
        @Override public Object getValueAt(int row, int col) { return headers.get(row); }
        @Override public boolean isCellEditable(int row, int col) { return true; }
        @Override public void setValueAt(Object value, int row, int col) {
            headers.set(row, value != null ? value.toString() : "");
            fireTableRowsUpdated(row, row);
        }
        public void addHeader(String header) {
            headers.add(header);
            fireTableRowsInserted(headers.size() - 1, headers.size() - 1);
        }
        public void removeHeader(int row) {
            if (row >= 0 && row < headers.size()) {
                headers.remove(row);
                fireTableRowsDeleted(row, row);
            }
        }
        public List<String> getHeaders() { return new ArrayList<>(headers); }
        public void setHeaders(List<String> list) {
            headers.clear();
            if (list != null) headers.addAll(list);
            fireTableDataChanged();
        }
    }
}
