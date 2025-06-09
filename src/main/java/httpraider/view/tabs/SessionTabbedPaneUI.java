package httpraider.view.tabs;

import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;

public class SessionTabbedPaneUI extends BasicTabbedPaneUI {
    public static final Color ORANGE = new Color(0xF47442);
    public static final Color SEL       = new Color(0x3FA7B1B0, true);
    public static final Color UNSEL     = new Color(0x1AB5BFBF, true);


    @Override
    protected void installDefaults() {
        super.installDefaults();
        // Make insets smaller so the highlight line is visible
        tabInsets = new Insets(1, 12, 1, 12);
    }

    @Override
    protected void paintTabBackground(Graphics g, int tp, int i,
                                      int x, int y, int w, int h, boolean sel) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(sel ? SEL : UNSEL);
        g2.fillRect(x,y,w,h);
        g2.dispose();
    }

    /** Force 25 px tall tabs. */
    @Override
    protected int calculateTabHeight(int tabPlacement, int tabIndex, int fontHeight) {
        return 25;
    }

    /** Paint each tab, add a vertical divider, plus the orange highlight if selected. */
    @Override
    protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex,
                                  int x, int y, int w, int h, boolean isSelected) {
        //super.paintTabBorder(g, tabPlacement, tabIndex, x, y, w, h, isSelected);

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            // orange highlight at bottom if selected
            if (isSelected) {
                g2.setColor(ORANGE);
                int lineHeight = 3;
                g2.fillRect(x + 2, (y + h) - lineHeight, w - 4, lineHeight);
            }
            // vertical divider on the right edge
          /*  g2.setColor(Color.LIGHT_GRAY);
            int dividerX = x + w - 1;
            g2.drawLine(dividerX, y + 1, dividerX, (y + h) - 2);*/
        } finally {
            g2.dispose();
        }
    }

    /** Remove the extra content border => no empty space below the tabs. */
    @Override
    protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
        // do nothing
    }

    @Override
    protected void paintFocusIndicator(Graphics g, int tabPlacement,
                                       Rectangle[] rects, int tabIndex,
                                       Rectangle iconRect, Rectangle textRect,
                                       boolean isSelected) {
        // no dotted focus
    }
}