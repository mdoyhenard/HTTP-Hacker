package View.Panels;

import Network.View.Panels.NetworkPanelContainer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class NetworkPanel extends JPanel {

    public NetworkPanel(){
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1,0,0,0, Color.LIGHT_GRAY),   new EmptyBorder(2, 0, 2, 0)));
        add(new NetworkPanelContainer(), BorderLayout.CENTER);
    }
}
