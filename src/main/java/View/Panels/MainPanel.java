package View.Panels;

import View.Tabbs.CustomTabbedPane;
import View.Tabbs.MultiTabbedPane;
import View.Tabbs.SessionTabbedPaneUI;
import View.Tabbs.StreamTabbedPaneUI;

import javax.swing.*;
import java.awt.*;

public class MainPanel extends JPanel {

    private CustomTabbedPane sessions;

    public MainPanel(){
        super();
        setBorder(null);
        setLayout(new BorderLayout());
        sessions = new CustomTabbedPane(new SessionTabbedPaneUI(), "session",  () -> {
            MultiTabbedPane multiTabbedPane = new MultiTabbedPane(new StreamTabbedPaneUI(), new NetworkPanel(), StreamPanel::new);
            multiTabbedPane.setBorder(BorderFactory.createMatteBorder(1,0,0,0, Color.LIGHT_GRAY));
            return multiTabbedPane;
        });
        add(sessions, BorderLayout.CENTER);
    }

}
