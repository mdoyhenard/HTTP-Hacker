package httpraider.view.components.UI;

import javax.swing.*;
import java.awt.*;

public class UIutils {

    public static void setBorderLayoutGaps(JComponent component, int size){
        component.add(Box.createVerticalStrut(size), BorderLayout.NORTH);
        component.add(Box.createVerticalStrut(size), BorderLayout.SOUTH);
        component.add(Box.createHorizontalStrut(size), BorderLayout.EAST);
        component.add(Box.createHorizontalStrut(size), BorderLayout.WEST);
    }

    public static void setBorderLayoutGaps(JComponent component, int horizontal, int vertical){
        component.add(Box.createVerticalStrut(vertical), BorderLayout.NORTH);
        component.add(Box.createVerticalStrut(vertical), BorderLayout.SOUTH);
        component.add(Box.createHorizontalStrut(horizontal), BorderLayout.EAST);
        component.add(Box.createHorizontalStrut(horizontal), BorderLayout.WEST);
    }
}
