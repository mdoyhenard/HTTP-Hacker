package httpraider.controller;

import httpraider.model.Session;
import httpraider.model.Stream;
import httpraider.view.panels.StreamPanel;
import httpraider.view.panels.SessionPanel;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public final class SessionController extends AbstractUIController<Session, SessionPanel> {

    private final List<StreamController> streamControllers = new ArrayList<>();
    private int nameSuffix;

    public SessionController(Session model, SessionPanel sessionPanel) {
        super(model, sessionPanel);
        nameSuffix = model.getNameSuffix();
        view.getStreams().addPlusMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                addStreamTab();
            }
        });
        view.getStreams().addTabRemovedListener(e -> {removeStreamTab((int) e.getSource());});
        updateFromModel();
    }

    public void updateFromModel(){
        streamControllers.clear();
        view.removeAllStreamTabs();
        for (Stream streamModel : model.getStreams()){
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
        Stream streamModel = new Stream("Stream " + nameSuffix++);
        model.setNameSuffix(nameSuffix);
        addStreamTab(streamModel);
    }

    private void addStreamTab(Stream streamModel) {
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
