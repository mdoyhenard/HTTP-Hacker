package httpraider.view.panels;

import extension.HTTPRaiderExtension;
import httpraider.view.tabs.CustomTabbedPane;
import httpraider.view.tabs.SessionTabbedPaneUI;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class SessionPanel extends JPanel {

    private static final int   TAB_HEIGHT = 25;
    private static final Color ORANGE     = new Color(0xF47442);

    private final HeaderStrip      header   = new HeaderStrip();
    private final CustomTabbedPane streams;
    private final NetworkHeader    netHdr   = new NetworkHeader();
    private final JPanel           content  = new JPanel(new BorderLayout());
    private final NetworkPanel     networkView;

    private Component currentStreamComp  = null;
    private int       currentStreamIndex = -1;

    private java.awt.event.ActionListener tabAddedListener;
    private java.awt.event.ActionListener tabRemovedListener;
    private java.awt.event.ActionListener tabSelectedListener;


    public SessionPanel() {

        super(new BorderLayout());
        this.networkView = new NetworkPanel();

        streams = new CustomTabbedPane(new SessionTabbedPaneUI());
        streams.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        streams.setBorder(null);
        netHdr.setBackground(HTTPRaiderExtension.BACK_COLOR);

        header.add(streams);
        header.add(netHdr);
        add(header, BorderLayout.NORTH);

        add(content, BorderLayout.CENTER);
        showNetwork();

        streams.addTabSelectedListener(e -> {
            if (currentStreamIndex != -1) {
                streams.setComponentAt(currentStreamIndex, currentStreamComp);
            }
            currentStreamIndex = streams.getSelectedIndex();
            if (currentStreamIndex != -1) {
                currentStreamComp = streams.getComponentAt(currentStreamIndex);
                showStream();
            }
        });


        netHdr.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { showNetwork(); }
        });

        streams.addTabRemovedListener(e -> {
            int removedIdx = (int) e.getSource();

            if (removedIdx < currentStreamIndex) {
                currentStreamIndex--;
            } else if (removedIdx == currentStreamIndex) {
                currentStreamIndex = -1;
                currentStreamComp  = null;
                showNetwork();
            }

            /* 2. bubble event up if someone registered --------------------- */
            if (tabRemovedListener != null) {
                tabRemovedListener.actionPerformed(e);
            }
        });
    }


    /* ------------------------------------------------------------------ */
    /*  getters used by controllers                                       */
    /* ------------------------------------------------------------------ */

    public NetworkPanel     getNetworkView()       { return networkView;          }
    public CustomTabbedPane getStreams()           { return streams;              }
    public Component        getCurrentStreamComp() { return currentStreamComp;    }
    public int              getCurrentStreamIndex(){ return currentStreamIndex;   }

    /* ------------------------------------------------------------------ */
    /*  public API                                                        */
    /* ------------------------------------------------------------------ */

    public void removeAllStreamTabs(){
        streams.removeAllTabs();
    }


    /** Delegate to CustomTabbedPane so controllers can create “stream n”. */
    public void addStreamTab(String name, StreamPanel streamPanel) {
        streams.addPanelTab(name, streamPanel);
    }

    public void setStreamTabName(String name, int index){
        streams.setTabName(name, index);
    }

    /* ------------------------------------------------------------------ */
    /*  view-switching internals                                          */
    /* ------------------------------------------------------------------ */

    /** Show Network panel, deselect stream headers, update pseudo-tab.  */
    private void showNetwork() {
        streams.getModel().clearSelection();          // prevents recursion

        if (currentStreamIndex != -1 &&                // put panel back
                currentStreamIndex < streams.getTabCount()) {
            streams.setComponentAt(currentStreamIndex, currentStreamComp);
        }
        currentStreamIndex = -1;
        currentStreamComp  = null;

        content.removeAll();
        content.add(networkView, BorderLayout.CENTER);
        content.revalidate(); content.repaint();

        netHdr.setSelected(true);
    }

    /** Show {@link #currentStreamComp} in the big centre area. */
    private void showStream() {
        streams.setComponentAt(currentStreamIndex, new JPanel());  // placeholder

        currentStreamComp.setVisible(true);
        currentStreamComp.setEnabled(true);
        content.removeAll();
        content.add(currentStreamComp, BorderLayout.CENTER);
        content.revalidate(); content.repaint();

        netHdr.setSelected(false);
    }

    /* ------------------------------------------------------------------ */
    /*  small helper classes                                              */
    /* ------------------------------------------------------------------ */

    /** Holds stream headers (¾) and the Network header (¼). */
    private class HeaderStrip extends JPanel {
        HeaderStrip() { super(null); setOpaque(false); }
        @Override public Dimension getPreferredSize() { return new Dimension(0, TAB_HEIGHT); }
        @Override public void doLayout() {
            int w = getWidth(), h = getHeight();
            int netW = w / 4;
            streams.setBounds(0, 0, w - netW, h);
            netHdr.setBounds(w - netW, 0, netW, h);
            setBorder(new MatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        }
    }

    /** Custom-painted pseudo-tab for the Network view. */
    private class NetworkHeader extends JComponent {
        private final Font bold;
        private boolean    selected;

        NetworkHeader() {
            Font base = getFont();
            if (base == null) base = UIManager.getFont("Label.font");
            if (base == null) base = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
            bold = base.deriveFont(Font.BOLD);
            setFont(base);

            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Show Network view");

            setOpaque(true);
            setBackground(Color.WHITE);
        }

        void setSelected(boolean b) {
            selected = b;
            setBackground(b ? SessionTabbedPaneUI.SEL : SessionTabbedPaneUI.UNSEL);
            repaint();
        }

        @Override public Dimension getPreferredSize() {
            Container p = getParent();
            int w = (p == null ? 200 : p.getWidth() / 4);
            return new Dimension(Math.max(80, w), TAB_HEIGHT);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            int w = getWidth(), h = getHeight();

            g2.setColor(getBackground());
            g2.fillRect(0, 0, w, h);

            g2.setColor(Color.LIGHT_GRAY);
            g2.drawLine(0, 0, 0, h);

            if (selected) {
                g2.setColor(ORANGE);
                g2.fillRect(2, h - 3, w - 4, 3);
            }

            g2.setFont(bold.deriveFont(Font.BOLD, 14f));
            String txt = "Network";
            FontMetrics fm = g2.getFontMetrics();
            int tx = (w - fm.stringWidth(txt)) / 2;
            int ty = (h + fm.getAscent() - fm.getDescent()) / 2;
            g2.setColor(Color.BLACK);
            g2.drawString(txt, tx, ty);

            g2.dispose();
        }
    }
}
