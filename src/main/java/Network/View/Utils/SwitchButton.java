package Network.View.Utils;

import java.awt.*;
import javax.swing.*;

public class SwitchButton extends JToggleButton {
    public SwitchButton() {
        setOpaque(false);
        setBorderPainted(false);
        // Optional: set preferred size for a consistent look
        setPreferredSize(new Dimension(30, 15));
        setMaximumSize(this.getPreferredSize());
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        // Enable anti-aliasing for smooth edges
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Colors for the "on" and "off" states
        Color onColor = new Color(0, 150, 136);  // Teal-ish color when on
        Color offColor = Color.GRAY;             // Gray when off
        Color circleColor = Color.WHITE;         // The slider's color

        int width = getWidth();
        int height = getHeight();
        int arc = height; // arc value makes the rectangle fully rounded

        // Set background color based on state
        g2.setColor(isSelected() ? onColor : offColor);
        g2.fillRoundRect(0, 0, width, height, arc, arc);

        // Calculate position of the slider (circle)
        int circleDiameter = height - 4;  // leave some padding
        int circleX = isSelected() ? width - circleDiameter - 2 : 2;
        int circleY = 2;

        g2.setColor(circleColor);
        g2.fillOval(circleX, circleY, circleDiameter, circleDiameter);

        g2.dispose();
    }
}
