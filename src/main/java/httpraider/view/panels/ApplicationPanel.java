package httpraider.view.panels;

import httpraider.view.components.CustomTabbedPane;
import httpraider.view.components.MainTabbedPaneUI;
import javax.swing.*;
import java.awt.*;

public class ApplicationPanel extends JPanel {

    private final CustomTabbedPane sessions;

    public ApplicationPanel(){
        super();
        setBorder(null);
        setLayout(new BorderLayout());
        sessions = new CustomTabbedPane(new MainTabbedPaneUI());
        add(sessions,BorderLayout.CENTER);
    }

    public CustomTabbedPane getSessionsTabbedPane(){
        return sessions;
    }

    public void setSelectedSession(int id){
        sessions.setSelectedIndex(id);
    }

    public void addSessionTab(String name, SessionPanel sessionPanel){
        sessions.addPanelTab(name, sessionPanel);
    }

    public void setSessionName(String name, int index){
        sessions.setTabName(name, index);
    }
}
