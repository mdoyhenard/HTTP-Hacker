package httpraider.controller;

import httpraider.model.Stream;
import httpraider.model.ConnectionSettings;
import httpraider.view.panels.StreamPanel;
import httpraider.view.menuBars.ConnectionBar;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Controls exactly one StreamPanel / Stream pair.
 */
public class StreamController extends AbstractController<Stream, StreamPanel>
        implements ActionListener {

    public StreamController(Stream model, StreamPanel view) {
        super(model, view);
    }

    @Override
    protected void wireView() {
     /*   *//* 1 ─ connection bar buttons *//*
        view.getConnectionBar().addActionListenerToAllButtons(this);           // :contentReference[oaicite:5]{index=5}

        *//* 2 ─ editors → model binding *//*
        view.bindEditorsToModel(model);    */                                    // new helper in StreamPanel
    }

    /* ---------- ActionListener ------------------------------------------ */

    @Override
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if (ConnectionBar.Action.CONNECT.name().equals(cmd))
            connectAndSend();
        else if (ConnectionBar.Action.DISCONNECT.name().equals(cmd))
            disconnect();
    }

    /* ---------- behaviour ----------------------------------------------- */

    private void connectAndSend() {
        ConnectionSettings cs = view.snapshotConnectionSettings();             // pulls current GUI values
        model.setConnectionSettings(cs);

        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
                // TODO: open socket, send bytes, read response
                //       update model.setResponseQueue(...)
                return null;
            }
            @Override protected void done() {
                ui(() -> view.updateStatus(true));
            }
        }.execute();
    }

    private void disconnect() {
        // TODO: close socket, update UI
        ui(() -> view.updateStatus(false));
    }
}
