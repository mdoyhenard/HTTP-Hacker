package httpraider.view.components;

import java.awt.*;
import javax.swing.*;

public class SwitchButton extends JPanel implements ActionComponent{
    private final JLabel label;
    private final JToggleButton toggle;

    public SwitchButton(String labelText) {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setOpaque(false);

        label = new JLabel(labelText);
        label.setAlignmentY(Component.CENTER_ALIGNMENT);

        toggle = new JToggleButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color onColor = new Color(0, 150, 136);
                Color offColor = Color.GRAY;
                Color circleColor = Color.WHITE;

                int width = getWidth();
                int height = getHeight();
                int arc = height;

                g2.setColor(isSelected() ? onColor : offColor);
                g2.fillRoundRect(0, 0, width, height, arc, arc);

                int circleDiameter = height - 4;
                int circleX = isSelected() ? width - circleDiameter - 2 : 2;
                int circleY = 2;

                g2.setColor(circleColor);
                g2.fillOval(circleX, circleY, circleDiameter, circleDiameter);

                g2.dispose();
            }
        };
        toggle.setOpaque(false);
        toggle.setBorderPainted(false);
        toggle.setPreferredSize(new Dimension(30, 15));
        toggle.setMaximumSize(toggle.getPreferredSize());
        toggle.setAlignmentY(Component.CENTER_ALIGNMENT);

        add(label);
        add(Box.createHorizontalStrut(8));
        add(toggle);
        add(Box.createHorizontalStrut(8));
    }

    public boolean isSelected() {
        return toggle.isSelected();
    }

    public void setSelected(boolean selected) {
        toggle.setSelected(selected);
    }

    public void addActionListener(java.awt.event.ActionListener l) {
        toggle.addActionListener(l);
    }

    public JLabel getLabel() {
        return label;
    }

    public JToggleButton getToggleButton() {
        return toggle;
    }
}
