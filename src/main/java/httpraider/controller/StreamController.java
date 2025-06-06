package httpraider.controller;

import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.message.requests.HttpRequest;
import httpraider.model.ConnectionSettings;
import httpraider.model.Stream;
import httpraider.view.menuBars.ConnectionBar;
import httpraider.view.panels.StreamPanel;

import javax.net.ssl.*;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.Socket;
import java.security.cert.X509Certificate;
import java.util.Arrays;

public final class StreamController extends AbstractUIController<Stream, StreamPanel>{

    private Socket socket;
    private BufferedInputStream in;
    private OutputStream out;
    private volatile ConnectionBar.State state;
    private ByteArrayOutputStream response;
    private final Object responseLock = new Object();

    public StreamController(Stream model, StreamPanel view) {
        super(model, view);
        state = ConnectionBar.State.DISCONNECTED;
        updateFromModel();

        view.getConnectionBar().setSendActionListener(this::sendAction);
        view.getConnectionBar().setDisconnectActionListener(this::disconnectAction);
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
            view.setResponseQueue(response.toByteArray());
        });
        updateTimer.setRepeats(false);

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
                        view.setResponseQueue(response.toByteArray());
                    }
                }
                if (state == ConnectionBar.State.CONNECTED) disconnect();
            }
        });
        readThread.start();
    }

}
