package extension;

import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import httpraider.controller.ApplicationController;
import httpraider.controller.SessionController;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class HTTPRaiderContextMenu implements ContextMenuItemsProvider {

    private final ApplicationController appController;

    public HTTPRaiderContextMenu(ApplicationController appController) {
        this.appController= appController;
    }

    @Override
    public java.util.List<Component> provideMenuItems(ContextMenuEvent event) {
        List<Component> menuItems = new ArrayList<>();
        event.messageEditorRequestResponse().ifPresent(requestResponse -> {

            JMenuItem newSessionMenuItem = new JMenuItem("Send to New Session");
            newSessionMenuItem.addActionListener(e -> sendRequestToNewSession(requestResponse.requestResponse().request()));
            menuItems.add(newSessionMenuItem);

            for (SessionController sessionController : appController.getSessionControllers()){
                JMenuItem namedSession = new JMenuItem("Send to " + sessionController.getName());
                namedSession.addActionListener(e -> sendRequestToSession(requestResponse.requestResponse().request(), sessionController));
                menuItems.add(namedSession);
            }

        });

        return menuItems;
    }

    private void sendRequestToNewSession(HttpRequest request) {
        appController.addSessionTab();
        appController.getLastController().getLastStreamController().setClientRequest(request.toByteArray().getBytes());
        appController.getLastController().getLastStreamController().setHttpService(request.httpService());
    }

    private void sendRequestToSession(HttpRequest request, SessionController sessionController) {
        sessionController.addStreamTab();
        sessionController.getLastStreamController().setClientRequest(request.toByteArray().getBytes());
        sessionController.getLastStreamController().setHttpService(request.httpService());
    }
}
