package View.Tabbs;

import Extension.HTTPRaiderExtension;
import View.Panels.NetworkPanel;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Supplier;

public class MultiTabbedPane extends JPanel {

    private static final int   TAB_HEIGHT = 25;
    private static final Color ORANGE     = new Color(0xF47442);

    private final HeaderStrip      header   = new HeaderStrip();
    private final CustomTabbedPane streams;
    private final NetworkHeader    netHdr   = new NetworkHeader();
    private final JPanel           content  = new JPanel(new BorderLayout());
    private final NetworkPanel        networkView;

    private Component        currentStreamComp  = null;   // panel now in content
    private int             currentStreamIndex = -1;     // its tab index


    public MultiTabbedPane(BasicTabbedPaneUI pillUI,
                           NetworkPanel networkContent,
                           Supplier<Component> factory) {

        super(new BorderLayout());
        this.networkView = networkContent;

        streams = new CustomTabbedPane(pillUI, "stream", factory);
        streams.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        streams.setBorder(null);
        netHdr.setBackground(HTTPRaiderExtension.BACK_COLOR);

        header.add(streams);
        header.add(netHdr);
        add(header, BorderLayout.NORTH);

        /* ── content area ───────────────────────────────────────────────── */
        add(content, BorderLayout.CENTER);
        showNetwork();                                   // default view

        /* ── react to user selections via the new listener ─────────────── */
        streams.setTabSelectedListener( e -> {
            if (currentStreamIndex != -1){
                streams.setComponentAt(currentStreamIndex, currentStreamComp);
            }
            currentStreamIndex = streams.getSelectedIndex();
            currentStreamComp = streams.getComponentAt(currentStreamIndex);
            showStream();
        });

        netHdr.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { showNetwork(); }
        });

        streams.setTabRemovedListener( e -> {
            if (streams.indexOfTabComponent((Component) e.getSource())<currentStreamIndex) currentStreamIndex--;
        });
    }

    public NetworkPanel getNetworkView() {
        return networkView;
    }

    public CustomTabbedPane getStreams() {
        return streams;
    }

    public Component getCurrentStreamComp() {
        return currentStreamComp;
    }

    public void setCurrentStreamComp(Component currentStreamComp) {
        this.currentStreamComp = currentStreamComp;
    }

    public int getCurrentStreamIndex() {
        return currentStreamIndex;
    }

    public void setCurrentStreamIndex(int currentStreamIndex) {
        this.currentStreamIndex = currentStreamIndex;
    }

    /* ═════════════════════════════ public API ════════════════════════════ */
    public void addClosableTab(String title, Component contentPanel) {
        streams.addClosableTab(title, contentPanel);
        header.revalidate();
        header.repaint();
    }

    public void addUntitledTab() { streams.addUntitledTab(); }

    /* ═════════════════════ core view-switching logic ═════════════════════ */

    /** Show the fixed Network panel. */
    private void showNetwork() {
        /* deselect any stream first (no ChangeEvent recursion because the
           selection listener fires only from setSelectedIndex) */
        streams.getModel().clearSelection();

        if (currentStreamIndex != -1){
            streams.setComponentAt(currentStreamIndex, currentStreamComp);
            currentStreamIndex = -1;
        }

        content.removeAll();
        content.add(networkView, BorderLayout.CENTER);
        content.revalidate();
        content.repaint();

        netHdr.setSelected(true);
    }

    /** Show the chosen stream’s panel in the big area. */
    private void showStream() {
        streams.setComponentAt(currentStreamIndex, new JPanel());

        currentStreamComp.setVisible(true);
        currentStreamComp.setEnabled(true);
        content.removeAll();
        content.add(currentStreamComp, BorderLayout.CENTER);
        content.revalidate();
        content.repaint();

        netHdr.setSelected(false);
    }


    /* ═════════════════════ inner classes & helpers ═══════════════════════ */

    /** Strip that holds the stream headers (¾) and the Network header (¼). */
    private class HeaderStrip extends JPanel {
        HeaderStrip() { super(null); setOpaque(false); }
        @Override public Dimension getPreferredSize() { return new Dimension(0, TAB_HEIGHT); }
        @Override public void doLayout() {
            int w = getWidth(), h = getHeight();
            int netW = w / 4;                         // exactly ¼
            streams.setBounds(0, 0, w - netW, h);
            netHdr.setBounds(w - netW, 0, netW, h);
        }
    }

    /** Custom-painted pseudo-tab for the Network view. */
    private class NetworkHeader extends JComponent {
        private final Font  bold;
        private boolean     selected = false;

        NetworkHeader() {
            Font base = getFont();
            if (base == null) base = UIManager.getFont("Label.font");
            if (base == null) base = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
            bold = base.deriveFont(Font.BOLD);
            setFont(base);

            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Show Network view");

            setOpaque(true);                 // make sure the background gets painted
            setBackground(Color.WHITE);      // initial state
        }

        /** Called by whichever component toggles the tab */
        void setSelected(boolean b) {
            selected = b;
            // 0x2487B9C3 = A-R-G-B (0x24 ≈ 14 % opacity)
            setBackground(b ? StreamTabbedPaneUI.SEL : StreamTabbedPaneUI.UNSEL);
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

            // background
            g2.setColor(getBackground());
            g2.fillRect(0, 0, w, h);

            // left divider
            g2.setColor(Color.LIGHT_GRAY);
            g2.drawLine(0, 0, 0, h);

            // orange underline when selected
            if (selected) {
                g2.setColor(ORANGE);
                g2.fillRect(2, h - 3, w - 4, 3);
            }

            // header text
            g2.setFont(bold);
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
