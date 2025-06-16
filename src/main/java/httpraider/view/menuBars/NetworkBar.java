package httpraider.view.menuBars;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;

public class NetworkBar extends JPanel {

    private final JButton discoverButton;
    private final JButton autoLayoutButton;

    public NetworkBar() {
        super(new BorderLayout());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 1, 0, Color.LIGHT_GRAY),
                new EmptyBorder(2, 0, 2, 0)));
        setBackground(new Color(0x2AA9BBE1, true)); // This is BACK_DISCONNECTED from ConnectionBar

        discoverButton = new JButton("Discover Network");
        autoLayoutButton = new JButton("Auto-Layout");

        discoverButton.setFont(discoverButton.getFont().deriveFont(Font.BOLD));
        autoLayoutButton.setFont(autoLayoutButton.getFont().deriveFont(Font.BOLD));
        discoverButton.setFocusPainted(false);
        autoLayoutButton.setFocusPainted(false);
        discoverButton.setBackground(new Color(119, 79, 221, 224)); // BTN_COLOR_CONNECT
        discoverButton.setForeground(Color.WHITE);
        autoLayoutButton.setBackground(new Color(255, 95, 44));     // BTN_COLOR_SEND
        autoLayoutButton.setForeground(Color.WHITE);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT));
        left.setOpaque(false);
        left.add(discoverButton);
        left.add(autoLayoutButton);

        add(left, BorderLayout.WEST);
    }

    public void setDiscoverActionListener(ActionListener l) {
        addListenerIfAbsent(discoverButton, l);
    }

    public void setAutoLayoutActionListener(ActionListener l) {
        addListenerIfAbsent(autoLayoutButton, l);
    }

    private static void addListenerIfAbsent(AbstractButton b, ActionListener l) {
        for (ActionListener e : b.getActionListeners()) if (e == l) return;
        b.addActionListener(l);
    }
}
