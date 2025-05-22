package View.Tabbs;

import java.awt.*;
import java.awt.event.*;
import java.util.function.Supplier;
import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

/**
 * JTabbedPane with closable tabs, a “+” pseudo-tab, and a pluggable
 * component factory.  The automatically numbered tabs use the
 * baseName provided in the constructor.
 */
public class CustomTabbedPane extends JTabbedPane {

    private final Component plusDummy = new JPanel();
    private final Supplier<Component> newTabFactory;
    private final String baseName;                // ← NEW
    private int untitledCount = 1;
    private ActionListener tabAddedListener;
    private ActionListener tabRemovedListener;
    private ActionListener tabSelectedListener;


    public CustomTabbedPane(String name, Supplier<Component> factory) {
        super();
        baseName = name;
        newTabFactory = factory;
        addPlusTab();
    }

    public CustomTabbedPane(BasicTabbedPaneUI ui,String name, Supplier<Component> factory) {
        super(TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        setUI(ui);
        baseName = name;
        newTabFactory = factory;
        addPlusTab();
    }

    public void setTabAddedListener(ActionListener l) { tabAddedListener = l; }
    public void setTabSelectedListener(ActionListener l) { tabSelectedListener = l; }
    public void setTabRemovedListener(ActionListener l) { tabRemovedListener = l; }

    @Override
    public void setSelectedIndex(int idx) {
        if (idx == getTabCount() - 1) {
            addUntitledTab();
            return;
        }
        super.setSelectedIndex(idx);

        /* NEW: notify only after the super-call, so getComponentAt(idx) is up-to-date */
        if (tabSelectedListener != null) {
            tabSelectedListener.actionPerformed(
                    new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "tab-selected"));
        }
    }


    public void addClosableTab(String title, Component content) {
        int plusIndex = getTabCount() - 1;
        insertTab(null, null, content, null, plusIndex);
        setTabComponentAt(plusIndex, new HeaderComponent(title));
        setSelectedIndex(plusIndex);

        if (tabAddedListener != null) {
            tabAddedListener.actionPerformed(
                    new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "tab-added"));
        }
    }

    public void addUntitledTab() {
        addClosableTab(baseName + " " + untitledCount++, newTabFactory.get());
    }

    /* =========================================================  “ + ” TAB  */

    private void addPlusTab() {
        addTab(null, plusDummy);

        JPanel plusHeader = new JPanel(null) {
            @Override public Dimension getPreferredSize() { return new Dimension(28, 25); }
        };
        plusHeader.setOpaque(false);
        plusHeader.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel plusLabel = new JLabel("+", SwingConstants.CENTER);
        plusLabel.setBorder(BorderFactory.createEmptyBorder(2, 8, 6, 8));
        plusHeader.add(plusLabel);

        plusHeader.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { addUntitledTab(); }
        });
        setTabComponentAt(indexOfComponent(plusDummy), plusHeader);
    }

    /* keep the “+” centred */
    @Override public void doLayout() {
        super.doLayout();
        int idx = indexOfComponent(plusDummy);
        if (idx >= 0) {
            Component c = getTabComponentAt(idx);
            if (c instanceof JPanel p && p.getComponentCount() == 1) {
                Component child = p.getComponent(0);
                Dimension sz = p.getSize();
                Dimension cp = child.getPreferredSize();
                child.setBounds((sz.width - cp.width)/2,
                        (sz.height - cp.height)/2,
                        cp.width, cp.height);
            }
        }
    }

    /* ====================================================  I N N E R   C L A S S  */

    /** Header component (title + close button) for a closable tab. */
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
            close.setMargin(new Insets(0,0,0,0));
            close.setFocusable(false);

            close.addActionListener(e -> {
                if (tabRemovedListener != null) {
                    tabRemovedListener.actionPerformed(
                            new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "tab-removed"));
                }
                int i = indexOfTabComponent(HeaderComponent.this);
                removeTabAt(i);

                if (getTabCount() == 1) {
                    setSelectedIndex(0);
                    return;
                }
                if (getSelectedIndex()==getTabCount()-1){
                    setSelectedIndex(getSelectedIndex()-1);
                    return;
                }

                setSelectedIndex(getSelectedIndex());
            });
            add(close);
        }

        @Override public Dimension getPreferredSize() {
            Dimension lt = lbl.getPreferredSize();
            Dimension bt = close.getPreferredSize();
            int w = lt.width + bt.width + 14;
            int h = Math.max(lt.height, bt.height) + 4;
            return new Dimension(w, h);
        }
        @Override public void doLayout() {
            Dimension lt = lbl.getPreferredSize();
            lbl.setBounds(4, 0, lt.width, getHeight());
            Dimension bt = close.getPreferredSize();
            int bx = getWidth() - bt.width + 5;
            close.setBounds(bx, -3, bt.width, bt.height);
        }
    }
}
