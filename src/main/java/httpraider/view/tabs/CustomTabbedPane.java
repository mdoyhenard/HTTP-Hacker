package httpraider.view.tabs;

import burp.api.montoya.http.message.requests.HttpRequest;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.function.Supplier;

public class CustomTabbedPane extends JTabbedPane {

    private final Component plusDummy = new JPanel();
    private JPanel plusButton;

    private ArrayList<ActionListener> tabRemoveListener;
    private ArrayList<ActionListener> tabSelectedListener;


    public CustomTabbedPane(BasicTabbedPaneUI ui) {
        super(TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        setUI(ui);
        tabRemoveListener = new ArrayList<>();
        tabSelectedListener = new ArrayList<>();
        addPlusTab();
    }

    public void addTabSelectedListener(ActionListener l) {
            tabSelectedListener.add(l);
    }

    public void addTabRemovedListener(ActionListener l) {
            tabRemoveListener.add(l);
    }

    public void removeAllTabs(){
        while (getTabCount() > 1) removeTabAt(0);
    }


    @Override
    public void setSelectedIndex(int idx) {
        if (idx == getTabCount() - 1) {
            return;
        }
        super.setSelectedIndex(idx);

        for (ActionListener listener : tabSelectedListener){
            listener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "tab-selected"));
        }
    }

    public void addPanelTab(String name, JPanel content) {
        SwingUtilities.invokeLater(() -> {
            int lastIndex = Math.max(0, getTabCount() - 1);
            insertTab(null, null, content, null, lastIndex);
            setTabComponentAt(lastIndex, new HeaderComponent(name));
            setSelectedIndex(lastIndex);
        });
    }

    public void addPlusMouseListener(MouseListener l){
        plusButton.addMouseListener(l);
    }

    public void setTabName(String name, int index){
        ((HeaderComponent) getTabComponentAt(index)).setHeaderName(name);
    }

    /* ── internal helpers ──────────────────────────────────────────────── */

    /** Builds the “+” pseudo-tab. */
    private void addPlusTab() {
            addTab(null, plusDummy);

            plusButton = new JPanel(null) {
                @Override
                public Dimension getPreferredSize() {
                    return new Dimension(18, 18);
                }
            };
            plusButton.setOpaque(false);
            plusButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            JLabel plusLabel = new JLabel("+", SwingConstants.CENTER);
            plusLabel.setBorder(BorderFactory.createEmptyBorder(2, 8, 6, 8));
            plusButton.add(plusLabel);
            setTabComponentAt(indexOfComponent(plusDummy), plusButton);
    }

    /* Keep the “+” centred when the tabs scroll. */
    @Override
    public void doLayout() {
        super.doLayout();
        int idx = indexOfComponent(plusDummy);
        if (idx >= 0) {
            Component c = getTabComponentAt(idx);
            if (c instanceof JPanel p && p.getComponentCount() == 1) {
                Component child = p.getComponent(0);
                Dimension sz = p.getSize();
                Dimension cp = child.getPreferredSize();
                child.setBounds((sz.width - cp.width) / 2,
                        (sz.height - cp.height) / 2,
                        cp.width, cp.height);
            }
        }
    }

    /* ── inner class: title + close button ─────────────────────────────── */

    private class HeaderComponent extends JPanel {
        private final JLabel  lbl;
        private final JButton close;

        HeaderComponent(String title) {
            super(null);
            setOpaque(false);

            lbl = new JLabel(title);
            add(lbl);

            close = new JButton("×");
            close.setBorderPainted(false);
            close.setFocusPainted(false);
            close.setContentAreaFilled(false);
            close.setOpaque(false);
            close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            close.setMargin(new Insets(0, 0, 0, 0));
            close.setFocusable(false);

            close.addActionListener(e -> {
                int index = indexOfTabComponent(HeaderComponent.this);
                removeTabAt(index);

                for (ActionListener listener : tabRemoveListener){
                    listener.actionPerformed(new ActionEvent(index, ActionEvent.ACTION_PERFORMED, "tab-removed"));
                }
            });
            add(close);
        }

        public void setHeaderName(String name){
            lbl.setText(name);
        }

        /* preferred size = title + button + padding */
        @Override
        public Dimension getPreferredSize() {
            Dimension lt = lbl.getPreferredSize();
            Dimension bt = close.getPreferredSize();
            int w = lt.width + bt.width + 14;
            int h = Math.max(lt.height, bt.height) + 4;
            return new Dimension(w, h);
        }

        /* manual layout: label left, “×” right */
        @Override
        public void doLayout() {
            Dimension lt = lbl.getPreferredSize();
            lbl.setBounds(4, 0, lt.width, getHeight());

            Dimension bt = close.getPreferredSize();
            int bx = getWidth() - bt.width + 5;
            close.setBounds(bx, -3, bt.width, bt.height);
        }
    }
}
