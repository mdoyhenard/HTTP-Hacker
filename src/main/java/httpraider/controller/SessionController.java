package httpraider.controller;

import httpraider.model.Session;
import httpraider.model.Stream;
import httpraider.model.PersistenceManager;
import httpraider.view.panels.SessionPanel;
import httpraider.view.panels.StreamPanel;
import httpraider.view.tabs.MultiTabbedPane;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * One instance per *session* tab.
 */
public class SessionController extends AbstractController<Session, SessionPanel> {

    private final List<StreamController> streamControllers = new ArrayList<>();

    public SessionController(Session session) {
        super(session, new SessionPanel());
    }

    @Override
    protected void wireView() {
        JTabbedPane tabs = view.getSessionsTabbedPane();                       // :contentReference[oaicite:0]{index=0}

        /* add an empty (untitled) tab on construction so the user lands in a stream */
        addStreamTab("Stream-1");

        /* react to the little [+] button of CustomTabbedPane (already wired in CustomTabbedPane) */
     /*   tabs.setTabAddedListener(e -> addStreamTab("Stream-" + (tabs.getTabCount()+1)));

        *//* clean-up when a stream tab gets closed *//*
        tabs.setTabRemovedListener(e -> {
            int idx = tabs.indexOfTabComponent((java.awt.Component) e.getSource());
            if (idx >= 0) streamControllers.remove(idx).dispose();
        });*/
    }

    /* ---------- public API ---------------------------------------------- */

    public void saveToDisk() {
        PersistenceManager.save(model.getId(), model);                         // :contentReference[oaicite:1]{index=1}
    }

    public void loadFromDisk() {
        PersistenceManager.load(model.getId(), Session.class)
                .ifPresent(saved -> model.getStreams()
                        .addAll(saved.getStreams()));
    }

    /* ---------- helpers -------------------------------------------------- */

    private void addStreamTab(String title) {
        Stream       streamM  = new Stream();                                  // :contentReference[oaicite:2]{index=2}
        StreamPanel  streamV  = new StreamPanel();                             // :contentReference[oaicite:3]{index=3}
        StreamController sc   = new StreamController(streamM, streamV);
        streamControllers.add(sc);

        /* tell the model */
        model.addStream(streamM);

        /* tell the UI */
        MultiTabbedPane pane = (MultiTabbedPane) view.getSessionsTabbedPane()
                .getComponentAt(view.getSessionsTabbedPane()
                        .getTabCount()-1);
        pane.addClosableTab(title, streamV);                                   // :contentReference[oaicite:4]{index=4}
    }
}
