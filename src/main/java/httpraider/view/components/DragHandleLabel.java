package httpraider.view.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class DragHandleLabel extends JLabel {
    private Runnable dragCallback;

    public DragHandleLabel() {
        super("\u2261"); // Unicode '≡'
        setFont(new Font("Dialog", Font.BOLD, 16));
        setForeground(new Color(150, 150, 150));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(18, 18));
        setAlignmentY(Component.CENTER_ALIGNMENT);
        setOpaque(false);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (dragCallback != null) dragCallback.run();
            }
        });
    }

    public void setDragCallback(Runnable r) {
        this.dragCallback = r;
    }
}
