package httpraider.controller;

import burp.api.montoya.http.HttpService;
import extension.ToolsManager;
import httpraider.controller.engines.TagEngine;
import httpraider.model.ConnectionSettingsModel;
import httpraider.model.StreamModel;
import httpraider.view.menuBars.ConnectionBar;
import httpraider.view.panels.StreamPanel;

import javax.net.ssl.*;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.cert.X509Certificate;
import java.util.Arrays;

public final class StreamController extends AbstractController<StreamModel, StreamPanel> {

    private Socket socket;
    private BufferedInputStream in;
    private OutputStream out;
    private volatile ConnectionBar.State state;
    private java.util.concurrent.CountDownLatch connectedLatch = new java.util.concurrent.CountDownLatch(1);
    private ByteArrayOutputStream response;
    private final Object responseLock = new Object();
    private final ToolsManager toolsManager;
    private boolean tagsEnabled = false;

    public StreamController(StreamModel model, StreamPanel view) {
        super(model, view);
        state = ConnectionBar.State.DISCONNECTED;
        updateFromModel();
        toolsManager = new ToolsManager(view, this);
        view.getConnectionBar().setSendActionListener(this::sendAction);
        view.getConnectionBar().setDisconnectActionListener(this::disconnectAction);
    }

    public void setName(String name) {
        model.setName(name);
    }

    public void setTagsEnabled(boolean tagsEnabled) {
        this.tagsEnabled = tagsEnabled;
    }

    public void setClientRequest(byte[] request) {
        model.setClientRequest(request);
        view.setClientRequest(request);
    }

    public void setHttpService(HttpService service) {
        updateConnectionSettings(new ConnectionSettingsModel(service.host(), service.port(), service.secure()));
    }

    public String getHost(){
        return view.getConnectionBar().getHost();
    }

    public void updateFromModel() {
        view.setClientRequest(model.getClientRequest());
        view.setRequestQueue(model.getRequestQueue());
        view.setResponseQueue(model.getResponseQueue());
        view.setBaseView();
        view.updateConnectionBar(model.getConnectionSettings().getHost(), model.getConnectionSettings().getPort(), model.getConnectionSettings().isTls());
    }

    private void updateModelFromView() {
        updateConnectionSettingsModelFromView();
        model.setClientRequest(view.getClientRequest());
        model.setRequestQueue(view.getRequestQueue());
        model.setResponseQueue(view.getResponseQueue());
    }

    private void updateConnectionSettingsModelFromView() {
        model.getConnectionSettings().setHost(view.getConnectionBar().getHost());
        model.getConnectionSettings().setPort(view.getConnectionBar().getPort());
        model.getConnectionSettings().setTls(view.getConnectionBar().isTLS());
    }

    private void updateConnectionSettings(ConnectionSettingsModel connectionSettingsModel) {
        model.setConnectionSettings(connectionSettingsModel);
        view.updateConnectionBar(connectionSettingsModel.getHost(), connectionSettingsModel.getPort(), connectionSettingsModel.isTls());
    }

    private void sendAction(ActionEvent e) {
        if (view.getConnectionBar().isReset() || !isConnected()) {
            disconnect();
            connect();
        }
        sendRequest();
    }

    private boolean isConnected() {
        return socket != null && state == ConnectionBar.State.CONNECTED;
    }

    private void sendRequest() {
        sendRequest(view.getClientRequest());
    }

    private void sendRequest(byte[] request) {
        Thread writeThread = new Thread(() -> {
            try {
                if (!connectedLatch.await(8000, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                    return;
                }
                if (state != ConnectionBar.State.CONNECTED) {
                    return;
                }
                if (tagsEnabled){
                    int valid = TagEngine.validate(request);
                    if (valid== TagEngine.CORRECT) out.write(TagEngine.resolve(request));
                    else {
                        runOnEDT(() -> view.setResponseQueue(("<!--ERROR procesing the tag at index: "+valid+" -->").getBytes()));
                        disconnect();
                        return;
                    }
                }
                else out.write(request);
                out.flush();
                runOnEDT(() -> view.addRequestQueueBytes(tagsEnabled ? TagEngine.resolve(request) : request));
            } catch (Exception ex) {
                runOnEDT(() -> view.setRequestQueue("<!--ERROR SENDING REQUEST DATA-->" + Arrays.toString(ex.getStackTrace())));
                disconnect();
            } finally {
                runOnEDT(this::updateModelFromView);
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
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
        } finally {
            socket = null;
            in = null;
            out = null;
        }
    }

    private void setState(ConnectionBar.State s) {
        this.state = s;
        runOnEDT(() -> view.setState(s));
    }

    private void connect() {
        setState(ConnectionBar.State.CONNECTING);
        response = new ByteArrayOutputStream();
        connectedLatch = new java.util.concurrent.CountDownLatch(1);
        Thread thread = new Thread(() -> {
            try {
                String host = callOnEDT(() -> view.getConnectionBar().getHost());
                int port = callOnEDT(() -> view.getConnectionBar().getPort());
                boolean tls = callOnEDT(() -> view.getConnectionBar().isTLS());
                if (tls) {
                    Socket plainSocket = new Socket();
                    plainSocket.connect(new InetSocketAddress(host, port), 3000);
                    TrustManager[] trustAllCerts = new TrustManager[]{
                            new X509TrustManager() {
                                public X509Certificate[] getAcceptedIssuers() {
                                    return null;
                                }

                                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                                }

                                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                                }
                            }
                    };
                    SSLContext sc = SSLContext.getInstance("TLS");
                    sc.init(null, trustAllCerts, new java.security.SecureRandom());
                    SSLSocketFactory sslSocketFactory = sc.getSocketFactory();
                    SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(plainSocket, host, port, true);
                    sslSocket.setEnabledProtocols(new String[]{"TLSv1.2", "TLSv1.3"});
                    sslSocket.setSoTimeout(6000);
                    sslSocket.startHandshake();
                    sslSocket.setSoTimeout(0);
                    socket = sslSocket;
                } else {
                    socket = new Socket();
                    socket.connect(new InetSocketAddress(host, port), 5000);
                }
                socket.setKeepAlive(true);
                socket.setReceiveBufferSize(65538);
                socket.setSendBufferSize(65538);
                in = new BufferedInputStream(socket.getInputStream(), 65536);
                out = socket.getOutputStream();
                if (state == ConnectionBar.State.CONNECTING) {
                    setState(ConnectionBar.State.CONNECTED);
                    connectedLatch.countDown();
                    startReading();
                } else {
                    socket = null;
                    in = null;
                    out = null;
                }
            } catch (Exception ex) {
                socket = null;
                in = null;
                out = null;
                setState(ConnectionBar.State.ERROR);
            }
        });
        thread.start();
    }

    private void startReading() {
        SwingUtilities.invokeLater(() -> {
            Timer updateTimer = new Timer(20, e -> {
                int pos = view.getResponseQueueBytes().length;
                view.setResponseQueue(response.toByteArray());
                view.setResponseQueueCaretPosition(pos);
            });
            updateTimer.setRepeats(false);
            //view.setResponseHTTPsearch();
            Thread readThread = new Thread(() -> {
                byte[] buffer = new byte[8192];
                int bytesRead;
                try {
                    while (socket != null && state == ConnectionBar.State.CONNECTED && (bytesRead = in.read(buffer)) != -1) {
                        synchronized (responseLock) {
                            response.write(buffer, 0, bytesRead);
                        }
                        runOnEDT(() -> {
                            if (updateTimer.isRunning()) {
                                updateTimer.restart();
                            } else {
                                updateTimer.start();
                            }
                        });
                    }
                } catch (IOException ignored) {
                } finally {
                    synchronized (responseLock) {
                        if (response.size() > 0) {
                            int pos = view.getResponseQueueBytes().length;
                            runOnEDT(() -> {
                                view.setResponseQueue(response.toByteArray());
                                view.setResponseQueueCaretPosition(pos);
                            });
                        }
                    }
                    if (state == ConnectionBar.State.CONNECTED) {
                        disconnect();
                    }
                }
            });
            readThread.start();
        });
    }

    private void runOnEDT(Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeLater(r);
        }
    }

    private <T> T callOnEDT(java.util.function.Supplier<T> s) {
        if (SwingUtilities.isEventDispatchThread()) {
            return s.get();
        }
        java.util.concurrent.atomic.AtomicReference<T> ref = new java.util.concurrent.atomic.AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(() -> ref.set(s.get()));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return ref.get();
    }

}
