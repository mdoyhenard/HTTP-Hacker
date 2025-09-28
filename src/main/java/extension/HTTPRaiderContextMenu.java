package extension;

import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import httpraider.controller.ApplicationController;
import httpraider.controller.SessionController;

import javax.swing.*;
import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
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
        appController.getLastController().getLastStreamController().setClientRequest(replaceVersion(request.toByteArray().getBytes()));
        appController.getLastController().getLastStreamController().setHttpService(request.httpService() != null ? request.httpService() : getServiceFromRequest(request));
    }

    private void sendRequestToSession(HttpRequest request, SessionController sessionController) {
        sessionController.addStreamTab();
        sessionController.getLastStreamController().setClientRequest(replaceVersion(request.toByteArray().getBytes()));
        sessionController.getLastStreamController().setHttpService(request.httpService() != null ? request.httpService() : getServiceFromRequest(request));
        appController.setSelectedSession(sessionController);
    }

    private byte[] replaceVersion(byte[] input) {
        final byte[] pattern     = "HTTP/2".getBytes(StandardCharsets.ISO_8859_1);
        final byte[] replacement = "HTTP/1.1".getBytes(StandardCharsets.ISO_8859_1);

        outer:
        for (int i = 0; i <= input.length - pattern.length; i++) {
            for (int j = 0; j < pattern.length; j++) {
                if (input[i + j] != pattern[j]) {
                    continue outer;
                }
            }

            byte[] output = new byte[input.length + 2];

            System.arraycopy(input, 0, output, 0, i);
            System.arraycopy(replacement, 0, output, i, replacement.length);
            int tailSrcPos  = i + pattern.length;
            int tailDestPos = i + replacement.length;
            System.arraycopy(input, tailSrcPos, output, tailDestPos,
                    input.length - tailSrcPos);

            return output;
        }

        return Arrays.copyOf(input, input.length);
    }

    private HttpService getServiceFromRequest(HttpRequest request){
        String host = "localhost";
        if (request.hasHeader("Host")) host = request.headerValue("Host");
        return HttpService.httpService(host, 443, true);
    }
}
