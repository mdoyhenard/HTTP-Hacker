package httpraider.view.panels;

import extension.HTTPRaiderExtension;
import httpraider.view.menuBars.ConnectionBar;
import httpraider.view.menuBars.InspectorBar;
import burp.api.montoya.ui.editor.EditorOptions;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import burp.api.montoya.ui.editor.WebSocketMessageEditor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;

import static javax.swing.SwingUtilities.invokeLater;

public class StreamPanel extends JPanel {

    private final ConnectionBar connectionBar;
    private final InspectorBar inspectorBar;
    private final EditorToolsPanel editorToolsGadget;
    private final HTTPEditorPanel<HttpRequestEditor> clientRequest;
    private final HTTPEditorPanel<HttpRequestEditor> requestQueue;
    private final HTTPEditorPanel<WebSocketMessageEditor> responseQueue;
    private final ArrayList<HTTPEditorPanel<HttpRequestEditor>> proxyRequests;

    public StreamPanel() {
        super(new BorderLayout());
        connectionBar = new ConnectionBar();
        connectionBar.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Color.LIGHT_GRAY), new EmptyBorder(2, 0, 2, 0)));
        add(connectionBar, BorderLayout.NORTH);
        inspectorBar = new InspectorBar();
        editorToolsGadget = new EditorToolsPanel();
        add(inspectorBar, BorderLayout.EAST);
        proxyRequests = new ArrayList<>();
        clientRequest = new HTTPEditorPanel<>("Client Request", HTTPRaiderExtension.API.userInterface().createHttpRequestEditor());
        requestQueue = new HTTPEditorPanel<>("Request Queue", HTTPRaiderExtension.API.userInterface().createHttpRequestEditor(EditorOptions.READ_ONLY));
        responseQueue = new HTTPEditorPanel<>("Response Queue", HTTPRaiderExtension.API.userInterface().createWebSocketMessageEditor(EditorOptions.READ_ONLY));
        setState(ConnectionBar.State.DISCONNECTED);
        setBaseView();
    }

    public EditorToolsPanel getEditorToolsPanel() {
        return editorToolsGadget;
    }

    public HTTPEditorPanel<HttpRequestEditor> getClientRequestEditor() {
        return clientRequest;
    }

    public byte[] getClientRequest() {
        return clientRequest.getBytes();
    }

    public byte[] getRequestQueue() {
        return requestQueue.getBytes();
    }

    public byte[] getResponseQueue() {
        return responseQueue.getBytes();
    }

    public void setResponseQueue(byte[] response) {
        responseQueue.setBytes(response);
    }

    public void setResponseHTTPsearch() {
        responseQueue.setSearchExpression("HTTP/1.1");
    }

    public void setResponseQueueCaretPosition(int pos) {
        responseQueue.setCaretPosition(pos);
    }

    public byte[] getResponseQueueBytes() {
        return responseQueue.getBytes();
    }

    public void addRequestQueueBytes(byte[] request) {
        requestQueue.addBytes(request);
    }

    public void addResponseQueueBytes(byte[] response) {
        responseQueue.addBytes(response);
    }

    public void setRequestQueue(byte[] request) {
        requestQueue.setBytes(request);
    }

    public void setRequestQueue(String request) {
        requestQueue.setBytes(request.getBytes());
    }

    public void setClientRequest(byte[] request) {
        clientRequest.setBytes(request);
    }

    public void setBaseView() {
        invokeLater(() -> {
            JSplitPane req = new JSplitPane(JSplitPane.VERTICAL_SPLIT, clientRequest, requestQueue);
            req.setResizeWeight(0.5);
            JSplitPane main = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, req, responseQueue);
            main.setResizeWeight(0.5);
            add(main, BorderLayout.CENTER);
        });
    }

    public void setProxyView() {
        invokeLater(() -> {
            JSplitPane queues = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, requestQueue, responseQueue);
            queues.setResizeWeight(0.5);
            JSplitPane main = new JSplitPane(JSplitPane.VERTICAL_SPLIT, getNestedRequestPane(), queues);
            main.setResizeWeight(0.5);
            add(main);
        });
    }

    private JSplitPane getNestedRequestPane() {
        JSplitPane root = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        root.setResizeWeight(0.5);
        root.setLeftComponent(clientRequest);
        if (proxyRequests.isEmpty()) {
            return root;
        }
        JSplitPane current = root;
        for (int i = 0; i < proxyRequests.size() - 1; i++) {
            JSplitPane next = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            next.setResizeWeight(0.5);
            next.setLeftComponent(proxyRequests.get(i));
            current.setRightComponent(next);
            current = next;
        }
        current.setRightComponent(proxyRequests.get(proxyRequests.size() - 1));
        return root;
    }

    public void updateConnectionBar(String host, int port, boolean tls) {
        invokeLater(() -> {
            connectionBar.setState(ConnectionBar.State.DISCONNECTED);
            connectionBar.setHost(host);
            connectionBar.setPort(port);
            connectionBar.setTLS(tls);
        });
    }

    public void clearQueues() {
        requestQueue.clear();
        responseQueue.clear();
    }

    public ConnectionBar getConnectionBar() {
        return connectionBar;
    }

    public void setState(ConnectionBar.State state) {
        connectionBar.setState(state);
        if (state == ConnectionBar.State.CONNECTING) {
            clearQueues();
        }
    }

    public InspectorBar getInspectorBar() {
        return inspectorBar;
    }

}
