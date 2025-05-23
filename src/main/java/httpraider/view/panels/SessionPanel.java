package httpraider.view.panels;

import httpraider.view.tabs.CustomTabbedPane;
import httpraider.view.tabs.MultiTabbedPane;
import httpraider.view.tabs.SessionTabbedPaneUI;
import httpraider.view.tabs.StreamTabbedPaneUI;
import javax.swing.*;
import java.awt.*;

public class SessionPanel extends JPanel {

    private CustomTabbedPane sessions;

    public SessionPanel(){
        super();
        setBorder(null);
        setLayout(new BorderLayout());
        sessions = new CustomTabbedPane(new SessionTabbedPaneUI(),"session",() -> {
            MultiTabbedPane multi = new MultiTabbedPane(new StreamTabbedPaneUI(),new NetworkPanel(),StreamPanel::new);
            multi.setBorder(BorderFactory.createMatteBorder(1,0,0,0,Color.LIGHT_GRAY));
            return multi;
        });
        add(sessions,BorderLayout.CENTER);
    }

    public CustomTabbedPane getSessionsTabbedPane(){ return sessions; }
}
