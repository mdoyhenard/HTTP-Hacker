package httpraider.controller;

import burp.api.montoya.http.message.requests.HttpRequest;
import extension.HTTPRaiderContextMenu;
import extension.HTTPRaiderExtension;
import httpraider.model.PersistenceManager;
import httpraider.model.Session;
import httpraider.view.panels.ApplicationPanel;
import httpraider.view.panels.SessionPanel;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Optional;

public final class ApplicationController {

    private static final String KEY = "HTTPStreamHacker.sessions.number";
    private static final String KEY_NUMBER = "HTTPStreamHacker.sessions.number";
    private final ApplicationPanel rootView;
    private final ArrayList<SessionController> sessionControllers;
    private int nameSuffix;

    public ApplicationController() {
        sessionControllers = new ArrayList<>();
        rootView = new ApplicationPanel();
        nameSuffix = 1;

        rootView.getSessionsTabbedPane().addPlusMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                addSessionTab();
            }
        });
        rootView.getSessionsTabbedPane().addTabRemovedListener(e -> removeSessionTab((int) e.getSource()));

        HTTPRaiderContextMenu contextMenu = new HTTPRaiderContextMenu(this);
        HTTPRaiderExtension.API.userInterface().registerContextMenuItemsProvider(contextMenu);
        HTTPRaiderExtension.API.userInterface().registerSuiteTab("HTTP Raider", rootView);
        HTTPRaiderExtension.API.extension().registerUnloadingHandler(this::saveAll);

        loadSaved();
        if (sessionControllers.isEmpty()) addSessionTab();

    }

    public void setSessionName(String name, int index){
        sessionControllers.get(index).setName(name);
        rootView.setSessionName(name, index);
    }

    public int getNameSuffix(){
        return nameSuffix++;
    }

    public SessionController getLastController(){
        return sessionControllers.get(sessionControllers.size()-1);
    }

    public ArrayList<SessionController> getSessionControllers(){
        return sessionControllers;
    }

    public void addSessionTab() {
        Session sessionModel = new Session("Session " + nameSuffix++, 1);
        addSessionTab(sessionModel);
    }

    private void addSessionTab(Session sessionModel) {
        SessionPanel sessionPanel = new SessionPanel();
        sessionControllers.add(new SessionController(sessionModel, sessionPanel));
        rootView.addSessionTab(sessionModel.getName(), sessionPanel);
    }

    private void removeSessionTab(int index){
        sessionControllers.remove(index);
    }

    @SuppressWarnings("unchecked")
    private void loadSaved() {
        try {
            nameSuffix = PersistenceManager.loadInt(KEY_NUMBER);
        }
        catch (Exception e){
            nameSuffix = 1;
        }

        Optional<?> optRaw = PersistenceManager.load(KEY, ArrayList.class);
        if (optRaw.isEmpty()) return;
        ArrayList<Session> saved;
        try {
            saved = (ArrayList<Session>) optRaw.get();
            if (saved.isEmpty()) return;
        } catch (NoSuchElementException e){
            return;
        }
        for (Session s : saved) addSessionTab(s);
    }

    private void saveAll() {
        ArrayList<Session> out = new ArrayList<>();
        for (SessionController sc : sessionControllers) out.add(sc.getModel());
        PersistenceManager.saveInt(KEY_NUMBER, nameSuffix);
        PersistenceManager.save(KEY, out);
    }
}
