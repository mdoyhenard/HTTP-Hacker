package httpraider.controller;

import httpraider.model.network.ProxyModel;
import httpraider.view.components.network.ProxyComponent;

import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class ProxyController {

    private final ProxyModel model;
    private final ProxyComponent view;

    public ProxyController(ProxyModel model, ProxyComponent view) {
        this.model = model;
        this.view = view;
    }

    public ProxyModel getModel() {
        return model;
    }

    public boolean isShowParserEnabled(){
        return model.isShowParser();
    }

    public ProxyComponent getView() {
        return view;
    }

    public void setDomainName(String domainName) {
        model.setDomainName(domainName);
    }

    public void setDescription(String description) {
        model.setDescription(description);
    }

    public String getDomainName() {
        return model.getDomainName();
    }

    public String getDescription() {
        return model.getDescription();
    }

    public boolean isClient() {
        return model.isClient();
    }

    public String getId() {
        return model.getId();
    }

    public void addMouseListener(MouseListener l) {
        view.addMouseListener(l);
    }

    public void addMouseMotionListener(MouseMotionListener l) {
        view.addMouseMotionListener(l);
    }

    public void setSelected(boolean selected) {
        view.setSelected(selected);
    }
    public void setEnabledProxy(boolean enabled){
        model.setShowParser(enabled);
        view.setEnabledProxy(enabled);
    }
}
