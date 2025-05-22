package View.Panels;

import Extension.HTTPRaiderExtension;
import View.MenuBars.ConnectionBar;
import View.MenuBars.EditorToolsPanel;
import View.MenuBars.InspectorBar;
import burp.api.montoya.ui.editor.EditorOptions;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import burp.api.montoya.ui.editor.WebSocketMessageEditor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class StreamPanel extends JPanel {

    private final ConnectionBar connectionBar;
    private final InspectorBar inspectorBar;
    private final HTTPEditorPanel<HttpRequestEditor> clientRequest;
    private final HTTPEditorPanel<HttpRequestEditor> requestQueue;
    private final HTTPEditorPanel<WebSocketMessageEditor> responseQueue;
    private final ArrayList<HTTPEditorPanel<HttpRequestEditor>> proxyRequests;



    public StreamPanel(){
        super(new BorderLayout());
        //setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        connectionBar = new ConnectionBar();
        connectionBar.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1,0,1,0, Color.LIGHT_GRAY),   new EmptyBorder(2, 0, 2, 0)));
        connectionBar.setConnected(false);
        add(connectionBar, BorderLayout.NORTH);


        inspectorBar = new InspectorBar();
        EditorToolsPanel editorTools = new EditorToolsPanel();
        inspectorBar.addTool("EDITOR", "Stream Tools", editorTools);
        add(inspectorBar, BorderLayout.EAST);

        proxyRequests = new ArrayList<>();
        clientRequest = new HTTPEditorPanel<HttpRequestEditor>("Client Request", HTTPRaiderExtension.API.userInterface().createHttpRequestEditor());
        requestQueue = new HTTPEditorPanel<HttpRequestEditor>("Request Queue", HTTPRaiderExtension.API.userInterface().createHttpRequestEditor(EditorOptions.READ_ONLY));
        responseQueue = new HTTPEditorPanel<WebSocketMessageEditor>("Response Queue", HTTPRaiderExtension.API.userInterface().createWebSocketMessageEditor(EditorOptions.READ_ONLY));

        proxyRequests.add(new HTTPEditorPanel<HttpRequestEditor>("Proxy 1", HTTPRaiderExtension.API.userInterface().createHttpRequestEditor(EditorOptions.READ_ONLY)));
        proxyRequests.add(new HTTPEditorPanel<HttpRequestEditor>("Proxy 2", HTTPRaiderExtension.API.userInterface().createHttpRequestEditor(EditorOptions.READ_ONLY)));

        setBaseView();

    }

    public void setBaseView(){
        SwingUtilities.invokeLater(() -> {
            JSplitPane requestSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, clientRequest, requestQueue);
            requestSplitPane.setResizeWeight(0.5);
            JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, requestSplitPane, responseQueue);
            mainSplitPane.setResizeWeight(0.5);
            add(mainSplitPane, BorderLayout.CENTER);
        });
    }

    public void setProxyView(){
        SwingUtilities.invokeLater(() -> {
            JSplitPane queuesPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, requestQueue, responseQueue);
            queuesPane.setResizeWeight(0.5);
            JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, getNestedRequestPane(), queuesPane);
            mainSplitPane.setResizeWeight(0.5);
            add(mainSplitPane);
        });
    }

    private JSplitPane getNestedRequestPane(){
        JSplitPane rootPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        rootPane.setResizeWeight(0.5);
        rootPane.setLeftComponent(clientRequest);
        if (proxyRequests.isEmpty()) return rootPane;

        JSplitPane current = rootPane;
        for (int i = 0; i< proxyRequests.size() - 1; i++){
            JSplitPane next = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            next.setResizeWeight(0.5);
            next.setLeftComponent(proxyRequests.get(i));

            current.setRightComponent(next);
            current = next;
        }
        current.setRightComponent(proxyRequests.get(proxyRequests.size()-1));
        return rootPane;
    }

}
