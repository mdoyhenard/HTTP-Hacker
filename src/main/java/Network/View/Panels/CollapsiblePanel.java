package Network.View.Panels;

import Network.View.Utils.SwitchButton;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

public class CollapsiblePanel extends JPanel {
    // Unicode arrows for collapsed/expanded states
    private static final String ARROW_RIGHT = "\u25B6"; // ▶
    private static final String ARROW_DOWN  = "\u25BC"; // ▼

    private final JLabel arrowLabel;
    private final JPanel headerPanel;
    private final JPanel bodyPanel;
    private final SwitchButton switchButton;
    private boolean isCollapsed;

    public CollapsiblePanel(String title, JComponent... components) {

        setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        setBackground(Color.WHITE);

        // == HEADER ==
        headerPanel = new JPanel();
        headerPanel.setLayout(new BorderLayout());
        //headerPanel.setOpaque(true);
        headerPanel.setBackground(Color.WHITE);

        // Arrow label (initially pointing RIGHT because we start collapsed)
        arrowLabel = new JLabel(ARROW_RIGHT, SwingConstants.LEFT);
        // Fix a small preferred size so it doesn’t shrink/expand when toggling
        arrowLabel.setPreferredSize(new Dimension(13, 14));
        JPanel arrowPanel = new JPanel();
        arrowPanel.setLayout(new BoxLayout(arrowPanel, BoxLayout.X_AXIS));
        arrowPanel.add(Box.createRigidArea(new Dimension(5, 0)));
        arrowPanel.add(arrowLabel);
        arrowPanel.add(Box.createRigidArea(new Dimension(5, 0)));

        // Title label (plain text, no bold)
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.PLAIN, 14));

        setLayout(new BorderLayout());
        switchButton = new SwitchButton();
        switchButton.setVisible(true);
        JPanel switchPanel = new JPanel();
        switchPanel.setLayout(new BoxLayout(switchPanel, BoxLayout.X_AXIS));
        switchPanel.add(switchButton);
        switchPanel.add(Box.createRigidArea(new Dimension(20, 0)));

        // Add that to the header
        //headerPanel.add(Box.createRigidArea(new Dimension(0, 7)));
        Component boxSpace = Box.createRigidArea(new Dimension(0, 7));
        headerPanel.add(boxSpace, BorderLayout.NORTH);
        headerPanel.add(arrowPanel, BorderLayout.WEST);
        headerPanel.add(switchPanel, BorderLayout.EAST);
        headerPanel.add(titleLabel, BorderLayout.CENTER);

        // Mouse listener to toggle collapse when user clicks the header
        MouseAdapter toggleListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                toggleCollapse();
            }
        };
        headerPanel.addMouseListener(toggleListener);
        boxSpace.addMouseListener(toggleListener);
        arrowPanel.addMouseListener(toggleListener);
        arrowLabel.addMouseListener(toggleListener);
        titleLabel.addMouseListener(toggleListener);

        // == BODY ==
        bodyPanel = new JPanel();
        // Vertical layout for stacking components inside the body
        bodyPanel.setLayout(new BoxLayout(bodyPanel, BoxLayout.Y_AXIS));
        bodyPanel.setOpaque(true);
        bodyPanel.setBackground(Color.WHITE);
        // Slight left indent to visually separate from the header
        //bodyPanel.setBorder(BorderFactory.createEmptyBorder(4, 16, 4, 4));

        // Add each component to the body
        bodyPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        for (JComponent comp : components) {
            //comp.setAlignmentX(Component.LEFT_ALIGNMENT);
            comp.setBackground(Color.WHITE);
            bodyPanel.add(comp);
        }

        isCollapsed = true;
        this.setPreferredSize(new Dimension(323, 35));
        this.setMaximumSize(this.getPreferredSize());

        // Add header (NORTH) and body (CENTER)
        add(headerPanel, BorderLayout.NORTH);
    }

    public void setSwitchVisible(boolean visible){
        this.switchButton.setVisible(visible);
    }

    public void setSwitchActionListener(ActionListener listener){
        this.switchButton.addActionListener(listener);
    }

    private void toggleCollapse() {
        isCollapsed = !isCollapsed;
        arrowLabel.setText(isCollapsed ? ARROW_RIGHT : ARROW_DOWN);
        if (isCollapsed){
            this.setPreferredSize(new Dimension(323, 40));
            this.setMaximumSize(this.getPreferredSize());
            remove(bodyPanel);
        }
        else{
            this.setPreferredSize(new Dimension(323, 300));
            this.setMaximumSize(this.getPreferredSize());
            add(bodyPanel, BorderLayout.CENTER);
        }
        revalidate(); // re-calculate layout
        repaint();    // redraw
    }

    public boolean isActive(){
        return (this.switchButton.isVisible() && this.switchButton.isSelected());
    }

    public void activate(){
        this.switchButton.setSelected(true);
    }

    public void deactivate(){
        this.switchButton.setSelected(false);
    }

    public void addComponent(JComponent component){
        component.setAlignmentX(LEFT_ALIGNMENT);
        bodyPanel.add(component);
    }

    public void collapse(){
        isCollapsed = true;
        arrowLabel.setText(ARROW_RIGHT);
        this.setPreferredSize(new Dimension(323, 40));
        this.setMaximumSize(this.getPreferredSize());
        remove(bodyPanel);
        revalidate(); // re-calculate layout
        repaint();    // redraw
    }

    public void expand(){
        isCollapsed = false;
        arrowLabel.setText(ARROW_DOWN);
        this.setPreferredSize(new Dimension(323, 300));
        this.setMaximumSize(this.getPreferredSize());
        add(bodyPanel, BorderLayout.CENTER);
        revalidate(); // re-calculate layout
        repaint();    // redraw
    }

    public void addHeaderButton(String title, MouseListener listener){

    }

    public void deleteComponent(JComponent component){
        bodyPanel.remove(component);
    }

    public void clear(){
        bodyPanel.removeAll();
    }
}