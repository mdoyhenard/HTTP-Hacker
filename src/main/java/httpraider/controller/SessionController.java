package httpraider.controller;

import httpraider.model.SessionModel;
import httpraider.model.StreamModel;
import httpraider.view.panels.StreamPanel;
import httpraider.view.panels.SessionPanel;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public final class SessionController extends AbstractUIController<SessionModel, SessionPanel> {

    private static final String KEY_NETWORK = "HTTPRaider.networks";
    private final List<StreamController> streamControllers = new ArrayList<>();
    private int nameSuffix;

    public SessionController(SessionModel model, SessionPanel sessionPanel) {
        super(model, sessionPanel);
        nameSuffix = model.getNameSuffix();
        view.getStreams().addPlusMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                addStreamTab();
            }
        });
        view.getStreams().addTabRemovedListener(e -> removeStreamTab((int) e.getSource()));
        updateStreamsFromModel();
    }

    private void updateStreamsFromModel(){
        streamControllers.clear();
        view.removeAllStreamTabs();
        for (StreamModel streamModel : model.getStreams()){
            StreamPanel streamPanel = new StreamPanel();
            StreamController streamCtl = new StreamController(streamModel, streamPanel);
            streamControllers.add(streamCtl);
            view.addStreamTab(streamModel.getName(), streamPanel);
        }
        if (streamControllers.isEmpty()) addStreamTab();
    }

    public StreamController getLastStreamController(){
        return streamControllers.get(streamControllers.size()-1);
    }

    public void setName(String name){
        model.setName(name);
    }

    public String getName(){
        return model.getName();
    }

    public void setStreamName(String name, int index){
        model.getStreams().get(index).setName(name);
        view.setStreamTabName(name, index);
    }

    public void addStreamTab() {
        StreamModel streamModel = new StreamModel("" + nameSuffix++);
        model.setNameSuffix(nameSuffix);
        addStreamTab(streamModel);
    }

    private void addStreamTab(StreamModel streamModel) {
        StreamPanel streamPanel = new StreamPanel();
        StreamController streamCtl = new StreamController(streamModel, streamPanel);
        model.addStream(streamModel);
        streamControllers.add(streamCtl);
        view.addStreamTab(streamModel.getName(), streamPanel);
    }

    private void removeStreamTab(int streamIndex) {
        model.removeStream(streamControllers.get(streamIndex).model);
        streamControllers.remove(streamIndex);
    }

}
