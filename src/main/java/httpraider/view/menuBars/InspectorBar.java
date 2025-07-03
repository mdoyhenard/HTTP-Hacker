package httpraider.view.menuBars;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

public final class InspectorBar extends JPanel {

    private static final Color STRIP_BG        = new Color(0xF5F5F5);
    private static final Color STRIP_LINE      = new Color(0xC8C8C8);
    private static final Color HEADER_BG       = STRIP_BG;
    private static final Color HEADER_BG_HOVER = new Color(0xEAEAEA);
    private static final Color HEADER_BG_SEL   = new Color(0x35B8CBDC, true);   // ★ lighter blue
    private static final int   STRIP_WIDTH     = 30;

    private final JPanel              strip   = new JPanel();
    private final CardLayout          cardsCL = new CardLayout();
    private final JPanel              cards   = new JPanel(cardsCL);
    private final Map<Header,String>  header2Card = new HashMap<>();

    private Header  activeHeader = null;
    private boolean expanded      = true;

    public InspectorBar() {
        super(new BorderLayout());

        strip.setBackground(STRIP_BG);
        strip.setBorder(new MatteBorder(0, 1, 0, 0, STRIP_LINE));
        strip.setLayout(new BoxLayout(strip, BoxLayout.Y_AXIS));

        add(strip, BorderLayout.EAST);
    }

    public void addTool(String id, String displayName, JComponent toolPanel) {
        Header h = new Header(displayName);
        header2Card.put(h, id);
        strip.add(h);
        strip.add(Box.createVerticalStrut(1));

        /* wrap panel so it sits at NORTH and has a left separator line */
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBorder(new MatteBorder(0, 1, 0, 0, STRIP_LINE));     // ★ left border
        wrapper.add(toolPanel, BorderLayout.NORTH);                     // ★ top-aligned
        cards.add(wrapper, id);
    }

    /* ---------- expand / collapse --------------------------------------------- */

    private void toggle(Header h) {
        if (expanded && h == activeHeader) { collapse(); return; }
        if (!expanded) expand();

        cardsCL.show(cards, header2Card.get(h));
        if (activeHeader != null) activeHeader.setSelected(false);
        h.setSelected(true);
        activeHeader = h;
    }
    public void expand()   { add(cards, BorderLayout.CENTER); revalidate(); expanded = true; }
    private void collapse() {
        remove(cards); if (activeHeader!=null) activeHeader.setSelected(false);
        activeHeader = null; revalidate(); repaint(); expanded = false;
    }

    /* ---------- header component ---------------------------------------------- */

    private final class Header extends JComponent {

        private final String text;
        private boolean hover, selected;

        Header(String text) {
            this.text = text;
            setPreferredSize(new Dimension(STRIP_WIDTH, 120));

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e){ hover=true; repaint();}
                @Override public void mouseExited (MouseEvent e){ hover=false; repaint();}
                @Override public void mouseClicked(MouseEvent e){ toggle(Header.this);    }
            });
        }
        void setSelected(boolean b){ selected=b; repaint(); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g2.setColor(selected?HEADER_BG_SEL: hover?HEADER_BG_HOVER: HEADER_BG);
            g2.fillRect(0,0,getWidth(),getHeight());

            g2.setColor(STRIP_LINE);
            //g2.drawLine(0,getHeight()-1,getWidth(),getHeight()-1);

            g2.setColor(Color.BLACK);                     // ★ always black text
            g2.setFont(getFont().deriveFont(Font.BOLD,12f));

            FontMetrics fm=g2.getFontMetrics();
            int w=fm.stringWidth(text), h=fm.getAscent();

            g2.translate(getWidth()/2.0,getHeight()/2.0);
            g2.rotate(Math.PI/2);
            g2.drawString(text,-w/2,h/2);
            g2.dispose();
        }
    }
}
