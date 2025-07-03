package httpraider.view.panels;

import httpraider.model.network.BodyLenHeader;
import httpraider.view.components.DragHandleLabel;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
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
    private final boolean draggable;
    private Runnable chunkPanelVisibilityHandler;
    private final List<ParserSettingsRowPanel> rowPanels = new ArrayList<>();

    public ParserSettingsSubPanel(String title, boolean hasCheckbox, int fieldSize, boolean checkboxDefault,
                                  int preferredWidth, Border border, boolean draggable) {
        this.hasCheckbox = hasCheckbox;
        this.fieldSize = fieldSize;
        this.checkboxDefault = checkboxDefault;
        this.draggable = draggable;

        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createMatteBorder(1, 1, 1, 1, Color.GRAY), title,
                TitledBorder.CENTER, TitledBorder.TOP, getFont().deriveFont(Font.BOLD, 15f)));
        setPreferredSize(new Dimension(preferredWidth, 200));
        setMinimumSize(new Dimension(preferredWidth, 150));
        setMaximumSize(new Dimension(preferredWidth + 60, 1000));

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

        if (draggable) {
            DragSource ds = new DragSource();
            DropTarget dt = new DropTarget(rowsPanel, DnDConstants.ACTION_MOVE, new RowDropTargetListener(), true);
        }
    }

    public void setChunkPanelVisibilityHandler(Runnable r) {
        this.chunkPanelVisibilityHandler = r;
    }

    public void addRow() {
        ParserSettingsRowPanel row = draggable ?
                new ParserSettingsRowPanel(fieldSize, hasCheckbox, checkboxDefault, true) :
                new ParserSettingsRowPanel(fieldSize, hasCheckbox, checkboxDefault, false);

        rowPanels.add(row);
        rowsPanel.add(row);

        if (hasCheckbox && chunkPanelVisibilityHandler != null) {
            row.getCheckBox().addActionListener(e -> chunkPanelVisibilityHandler.run());
        }

        if (draggable) {
            DragHandleLabel dragLabel = (DragHandleLabel) row.getComponent(0);
            dragLabel.setDragCallback(() -> {
                int index = rowPanels.indexOf(row);
                if (index != -1) {
                    TransferHandler th = new RowTransferHandler(index);
                    rowsPanel.setTransferHandler(th);
                    th.exportAsDrag(rowsPanel, new MouseEvent(dragLabel, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, 0, 0, 0, false), TransferHandler.MOVE);
                }
            });
        }
        updateRemoveListeners();
        rowsPanel.revalidate();
        rowsPanel.repaint();
        rowsPanel.setBackground(Color.WHITE);
        if (hasCheckbox && chunkPanelVisibilityHandler != null) {
            chunkPanelVisibilityHandler.run();
        }
    }

    public void removeRow(ParserSettingsRowPanel row) {
        rowPanels.remove(row);
        rowsPanel.remove(row);
        rowsPanel.revalidate();
        rowsPanel.repaint();
        rowsPanel.setBackground(Color.WHITE);
        if (hasCheckbox && chunkPanelVisibilityHandler != null) {
            chunkPanelVisibilityHandler.run();
        }
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

    private class RowTransferHandler extends TransferHandler {
        private final int dragIndex;

        public RowTransferHandler(int dragIndex) {
            this.dragIndex = dragIndex;
        }

        @Override
        public int getSourceActions(JComponent c) {
            return MOVE;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            return new StringSelection("row");
        }

        @Override
        public boolean canImport(TransferSupport info) {
            return info.isDrop() && info.getComponent() == rowsPanel;
        }

        @Override
        public boolean importData(TransferSupport info) {
            if (!canImport(info)) return false;
            DropLocation dl = info.getDropLocation();
            Point dropPoint = dl.getDropPoint();

            int dropIndex = getRowIndexAtPoint(dropPoint);
            if (dropIndex < 0 || dropIndex > rowPanels.size()) dropIndex = rowPanels.size();

            if (dragIndex == dropIndex || dragIndex == dropIndex - 1) return false;

            ParserSettingsRowPanel row = rowPanels.remove(dragIndex);
            rowsPanel.remove(dragIndex);

            if (dropIndex > rowPanels.size()) dropIndex = rowPanels.size();

            rowPanels.add(dropIndex, row);
            rowsPanel.add(row, dropIndex);
            updateRemoveListeners();

            rowsPanel.revalidate();
            rowsPanel.repaint();
            return true;
        }

        private int getRowIndexAtPoint(Point p) {
            Component[] components = rowsPanel.getComponents();
            for (int i = 0; i < components.length; i++) {
                Rectangle b = components[i].getBounds();
                if (p.y >= b.y && p.y < b.y + b.height) return i;
            }
            return components.length;
        }
    }

    private class RowDropTargetListener extends DropTargetAdapter {
        @Override
        public void drop(DropTargetDropEvent dtde) {
            dtde.acceptDrop(DnDConstants.ACTION_MOVE);
            try {
                dtde.dropComplete(true);
            } catch (Exception ex) {
                dtde.dropComplete(false);
            }
        }
    }

    public void updateRemoveListeners() {
        List<ParserSettingsRowPanel> rows = getRows();
        for (ParserSettingsRowPanel row : rows) {
            setRemoveButtonListener(row, e -> {
                removeRow(row);
                updateRemoveListeners();
            });
        }
    }
}
