package httpraider.controller;

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import httpraider.model.ConnectionSettings;
import httpraider.model.Stream;
import httpraider.view.menuBars.ConnectionBar;
import httpraider.view.menuBars.EditorToolsPanel;
import httpraider.view.panels.HTTPEditorPanel;
import httpraider.view.panels.StreamPanel;

import javax.net.ssl.*;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Arrays;

public final class StreamController extends AbstractUIController<Stream, StreamPanel>{

    private Socket socket;
    private BufferedInputStream in;
    private OutputStream out;
    private volatile ConnectionBar.State state;
    private ByteArrayOutputStream response;
    private final Object responseLock = new Object();

    private final Timer selTimer;
    private int lastSelectionLen = -1;

    public StreamController(Stream model, StreamPanel view) {
        super(model, view);
        state = ConnectionBar.State.DISCONNECTED;
        updateFromModel();

        EditorToolsPanel tools = view.getEditorToolsPanel();
        selTimer = new Timer(50, ev -> updateSelectedLabel());
        selTimer.start();
        tools.setInsertStringActionListener(this::insertAtCaret);

        view.getConnectionBar().setSendActionListener(this::sendAction);
        view.getConnectionBar().setDisconnectActionListener(this::disconnectAction);
    }

    private void updateSelectedLabel() {
        int len = view.getClientRequestEditor().getSelection()
                .map(sel -> sel.contents().getBytes().length)
                .orElse(0);

        if (len != lastSelectionLen) {                 // avoid needless repaint
            view.getEditorToolsPanel().setSelectedBytes(len);
            lastSelectionLen = len;
        }
    }

    private void insertAtCaret(ActionEvent e) {
        EditorToolsPanel tools = view.getEditorToolsPanel();
        String ascii   = tools.getAsciiText();
        if (ascii.isEmpty())
            return;                                    // nothing to insert

        int repeat     = tools.getRepeatCount();
        byte[] toWrite = ascii.repeat(repeat).getBytes(StandardCharsets.ISO_8859_1);

        HTTPEditorPanel<HttpRequestEditor> panel = view.getClientRequestEditor();

        byte[] original = panel.getBytes();
        int caret = Math.max(0, Math.min(panel.getCaretPosition(), original.length));

        byte[] patched  = new byte[original.length + toWrite.length];
        System.arraycopy(original, 0, patched, 0, caret);
        System.arraycopy(toWrite, 0, patched, caret, toWrite.length);
        System.arraycopy(original, caret, patched, caret+toWrite.length, original.length-caret);

        int pos = panel.getCaretPosition()+toWrite.length;
        panel.setBytes(patched);
        panel.setCaretPosition(pos);
    }

    public void setName(String name){
        model.setName(name);
    }

    public void setClientRequest(byte[] request){
        model.setClientRequest(request);
        view.setClientRequest(request);
    }

    public void setHttpService(HttpService service){
        updateConnectionSettings(new ConnectionSettings(service.host(), service.port(), service.secure()));
    }

    public void updateFromModel(){
        view.setClientRequest(model.getClientRequest());
        view.setRequestQueue(model.getRequestQueue());
        view.setResponseQueue(model.getResponseQueue());
        view.setBaseView();
        view.updateConnectionBar(model.getConnectionSettings().getHost(), model.getConnectionSettings().getPort(), model.getConnectionSettings().isTls());
    }

    private void updateModelFromView(){
        updateConnectionSettingsModelFromView();
        model.setClientRequest(view.getClientRequest());
        model.setRequestQueue(view.getRequestQueue());
        model.setResponseQueue(view.getResponseQueue());
    }

    private void updateConnectionSettingsModelFromView(){
        model.getConnectionSettings().setHost(view.getConnectionBar().getHost());
        model.getConnectionSettings().setPort(view.getConnectionBar().getPort());
        model.getConnectionSettings().setTls(view.getConnectionBar().isTLS());
    }

    private void updateConnectionSettings(ConnectionSettings connectionSettingsModel){
        model.setConnectionSettings(connectionSettingsModel);
        view.updateConnectionBar(connectionSettingsModel.getHost(), connectionSettingsModel.getPort(), connectionSettingsModel.isTls());
    }

    private void sendAction(ActionEvent e){
        if (view.getConnectionBar().isReset() || !isConnected()){
            disconnect();
            connect();
        }
        sendRequest();
    }

    private boolean isConnected() {
        return socket != null && state == ConnectionBar.State.CONNECTED;
    }

    private void sendRequest(){
        sendRequest(view.getClientRequest());
    }

    private void sendRequest(byte[] request) {
        Thread writeThread = new Thread(() -> {
            while (state == ConnectionBar.State.CONNECTING) {
                Thread.onSpinWait();
            }
            if (state != ConnectionBar.State.CONNECTED) return;
            try {
                out.write(request);
                out.flush();
                view.addRequestQueueBytes(request);
            } catch (Exception e) {
                view.setRequestQueue("<!--ERROR SENDING REQUEST DATA-->" + Arrays.toString(e.getStackTrace()));
                disconnect();
            }
            finally {
                updateModelFromView();
            }
        });
        writeThread.start();
    }

    private void disconnectAction(ActionEvent e) {
        disconnect();
    }

    private void disconnect() {
        setState(ConnectionBar.State.DISCONNECTED);
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {
        } finally {
            socket = null;
            in = null;
            out = null;
        }
    }

    private void setState(ConnectionBar.State state){
        this.state = state;
        view.setState(state);
    }


    private void connect() {
        setState(ConnectionBar.State.CONNECTING);
        response = new ByteArrayOutputStream();
        Thread thread = new Thread(()->{
            try {
                if (view.getConnectionBar().isTLS()) {
                    TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {public X509Certificate[] getAcceptedIssuers() {return null;} public void checkClientTrusted(X509Certificate[] certs, String authType) {} public void checkServerTrusted(X509Certificate[] certs, String authType) {}}};
                    SSLContext sc = SSLContext.getInstance("TLS");
                    sc.init(null, trustAllCerts, new java.security.SecureRandom());
                    SSLSocketFactory sslSocketFactory = sc.getSocketFactory();
                    SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(view.getConnectionBar().getHost(), view.getConnectionBar().getPort());
                    sslSocket.setEnabledProtocols(new String[]{"TLSv1.2", "TLSv1.3"});
                    sslSocket.startHandshake();
                    socket = sslSocket;
                } else {
                    socket = new Socket(view.getConnectionBar().getHost(), view.getConnectionBar().getPort());
                }
                socket.setKeepAlive(true);
                socket.setReceiveBufferSize(65538);
                socket.setSendBufferSize(65538);
                in = new BufferedInputStream(socket.getInputStream(), 65536);
                out = socket.getOutputStream();
                if (state == ConnectionBar.State.CONNECTING){
                    setState(ConnectionBar.State.CONNECTED);
                    startReading();
                }
            } catch (Exception e) {
                socket = null;
                in = null;
                out = null;
                view.setState(ConnectionBar.State.ERROR);
            }
        });
        thread.start();
        thread.interrupt();
    }

    private void startReading() {
        Timer updateTimer = new Timer(20, e -> {
            int pos = view.getResponseQueueBytes().length;
            view.setResponseQueue(response.toByteArray());
            view.setResponseQueueCaretPosition(pos);
        });
        updateTimer.setRepeats(false);
        view.setResponseHTTPsearch();

        Thread readThread = new Thread(() -> {
            byte[] buffer = new byte[8192];
            int bytesRead;
            try {
                while (socket != null && state == ConnectionBar.State.CONNECTED && (bytesRead = in.read(buffer)) != -1) {
                    synchronized(responseLock) {
                        response.write(buffer, 0, bytesRead);
                    }
                    if (updateTimer.isRunning()) {
                        updateTimer.restart();
                    } else {
                        updateTimer.start();
                    }
                }
            } catch (IOException ignored) {
            } finally {
                synchronized(responseLock) {
                    if (response.size() > 0) {
                        int pos = view.getResponseQueueBytes().length;
                        view.setResponseQueue(response.toByteArray());
                        view.setResponseQueueCaretPosition(pos);
                    }
                }
                if (state == ConnectionBar.State.CONNECTED) disconnect();
            }
        });
        readThread.start();
    }

}
