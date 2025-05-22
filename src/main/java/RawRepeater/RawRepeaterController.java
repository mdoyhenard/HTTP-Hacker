package RawRepeater;

import burp.api.montoya.core.ByteArray;

import javax.net.ssl.*;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Arrays;

public class RawRepeaterController {
    private Socket socket;
    private BufferedInputStream in;
    private OutputStream out;
    private boolean disconnecting;
    private boolean running;
    private volatile boolean connecting;
    private StringBuilder response;
    private final Object responseLock = new Object();

    public RawRepeaterController() {
        this.running = false;
        this.connecting = false;
        disconnecting = false;
    }

    public void sendRequest(String request) {
        Thread writeThread = new Thread(() -> {
            while (connecting) {
                if (disconnecting) return;
                Thread.onSpinWait();
            }
            try {
                this.out.write(request.getBytes());
                this.out.flush();
            } catch (Exception e) {
                disconnect();
            }
        });
        writeThread.start();
    }

    public void connect(String host, int port, boolean tls) {

    }

    private void startReading() {

    }

    public boolean isConnected(){
        return this.socket != null && this.running;
    }


    public void disconnect() {
        if (this.connecting) this.disconnecting = true;
        try {
            if (this.socket != null) this.socket.close();
        } catch (IOException ignored) {
        } finally {
            this.running = false;
            this.socket = null;
        }
    }
}
