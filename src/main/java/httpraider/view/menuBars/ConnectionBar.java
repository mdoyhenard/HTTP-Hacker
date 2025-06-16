package httpraider.view.menuBars;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static javax.swing.SwingUtilities.invokeLater;

public class ConnectionBar extends JPanel {

    public enum Action { CONNECT, DISCONNECT }
    public enum State { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

    public static final String TXT_BTN_CONNECT_AND_SEND = "Connect & Send";
    public static final String TXT_BTN_SEND = "Send";
    public static final String TXT_STATUS_DISCONNECTED = "DISCONNECTED";
    public static final String TXT_STATUS_CONNECTING = "CONNECTING...";
    public static final String TXT_STATUS_CONNECTED = "CONNECTED";
    public static final String TXT_STATUS_ERROR = "CONNECTION ERROR";

    private static final Color BACK_DISCONNECTED = new Color(0x2AE1A9A9, true);
    private static final Color BACK_CONNECTING = new Color(0x2AE3C66A, true);
    private static final Color BACK_CONNECTED = new Color(0x3A7FCC6A, true);
    private static final Color BACK_ERROR = new Color(0x3ACC6969, true);

    private static final Color FONT_DISCONNECTED = new Color(0xCFB51414, true);
    private static final Color FONT_CONNECTING = new Color(0xFFB79700, true);
    private static final Color FONT_CONNECTED = new Color(0xFF5B934C, true);
    private static final Color FONT_ERROR = new Color(0xFFD32F2F, true);

    private static final Color BTN_COLOR_CONNECT = new Color(85, 149, 96, 213);
    private static final Color BTN_COLOR_SEND = new Color(255, 95, 44);

    private State currentState;
    private final JButton sendButton;
    private final JCheckBox resetCheckBox;
    private final JTextField hostField;
    private final JTextField portField;
    private final JCheckBox tlsCheckBox;
    private final JButton disconnectButton;
    private final JLabel statusMsg;

    public ConnectionBar() {
        super(new BorderLayout());
        currentState = State.DISCONNECTED;
        sendButton = new JButton(TXT_BTN_CONNECT_AND_SEND);
        resetCheckBox = new JCheckBox();
        statusMsg = new JLabel(TXT_STATUS_DISCONNECTED);
        hostField = new JTextField("localhost", 15);
        portField = new JTextField("80", 5);
        tlsCheckBox = new JCheckBox();
        disconnectButton = new JButton("Disconnect");

        sendButton.setActionCommand(Action.CONNECT.name());
        disconnectButton.setActionCommand(Action.DISCONNECT.name());

        ((AbstractDocument) portField.getDocument()).setDocumentFilter(new httpraider.view.filters.DigitDocumentFilter(5));

        sendButton.setBackground(BTN_COLOR_CONNECT);
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);
        sendButton.setBorderPainted(false);
        sendButton.setOpaque(true);
        sendButton.setFont(sendButton.getFont().deriveFont(Font.BOLD));

        resetCheckBox.setOpaque(false);
        resetCheckBox.setBackground(Color.WHITE);
        resetCheckBox.setBorder(null);
        resetCheckBox.addActionListener(this::resetCheckboxListener);
        JLabel rstLabel = new JLabel("Reset connection");
        rstLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                resetCheckBox.setSelected(!isReset());
                resetCheckboxListener();
            }
        });

        tlsCheckBox.setOpaque(false);
        tlsCheckBox.setBackground(Color.WHITE);
        tlsCheckBox.setBorder(null);
        tlsCheckBox.addActionListener(this::tlsCheckboxListener);
        JLabel tlsLabel = new JLabel("TLS");
        tlsLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (tlsCheckBox.isEnabled()) {
                    setTLS(!isTLS());
                    tlsCheckboxListener();
                }
            }
        });

        statusMsg.setHorizontalAlignment(SwingConstants.CENTER);
        statusMsg.setFont(statusMsg.getFont().deriveFont(Font.BOLD, 15));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel centre = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        left.setOpaque(false);
        centre.setOpaque(false);
        right.setOpaque(false);

        left.add(sendButton);
        left.add(resetCheckBox);
        left.add(rstLabel);

        centre.add(statusMsg);

        right.add(new JLabel("Host:"));
        right.add(hostField);
        right.add(new JLabel("Port:"));
        right.add(portField);
        right.add(tlsCheckBox);
        right.add(tlsLabel);
        right.add(disconnectButton);

        add(left, BorderLayout.WEST);
        add(centre, BorderLayout.CENTER);
        add(right, BorderLayout.EAST);

        setState(State.DISCONNECTED);
    }

    private void resetCheckboxListener(){
        resetCheckboxListener(null);
    }

    private void resetCheckboxListener(ActionEvent e){
        invokeLater(()->{
            if (resetCheckBox.isSelected()){
                sendButton.setText(TXT_BTN_CONNECT_AND_SEND);
                sendButton.setBackground(BTN_COLOR_CONNECT);
            }
            else if (currentState == State.CONNECTED){
                sendButton.setText(TXT_BTN_SEND);
                sendButton.setBackground(BTN_COLOR_SEND);
            }
        });
    }

    private void tlsCheckboxListener(){
        tlsCheckboxListener(null);
    }

    private void tlsCheckboxListener(ActionEvent e){
        invokeLater(()-> {
            if (tlsCheckBox.isSelected() && getPort() == 0 || getPort() == 80) setPort(443);
            else if (!tlsCheckBox.isSelected() && getPort() == 443) setPort(80);
        });
    }

    public void setSendActionListener(ActionListener l) {
        addListenerIfAbsent(sendButton, l);
    }

    public void setDisconnectActionListener(ActionListener l) {
        addListenerIfAbsent(disconnectButton, l);
    }

    private static void addListenerIfAbsent(AbstractButton b, ActionListener l) {
        for (ActionListener e : b.getActionListeners()) if (e == l) return;
        b.addActionListener(l);
    }

    public void setState(State st) {
        currentState = st;
        switch (st) {
            case DISCONNECTED -> apply(TXT_STATUS_DISCONNECTED, BACK_DISCONNECTED, FONT_DISCONNECTED, BTN_COLOR_CONNECT, TXT_BTN_CONNECT_AND_SEND, true, false, true);
            case CONNECTING -> apply(TXT_STATUS_CONNECTING, BACK_CONNECTING, FONT_CONNECTING, BTN_COLOR_CONNECT, TXT_BTN_CONNECT_AND_SEND, false, true, false);
            case CONNECTED -> apply(TXT_STATUS_CONNECTED, BACK_CONNECTED, FONT_CONNECTED, BTN_COLOR_SEND, TXT_BTN_SEND, true, true, false);
            case ERROR -> apply(TXT_STATUS_ERROR, BACK_ERROR, FONT_ERROR, BTN_COLOR_CONNECT, TXT_BTN_CONNECT_AND_SEND, true, false, true);
        }
    }

    private void apply(String txt, Color back, Color font, Color btnColor, String btnTxt, boolean sendEnabled, boolean disconnectEnabled, boolean fieldsEnabled) {
        invokeLater(()->{
            setBackground(back);
            statusMsg.setText(txt);
            statusMsg.setForeground(font);
            sendButton.setText(resetCheckBox.isSelected() ? TXT_BTN_CONNECT_AND_SEND :btnTxt);
            sendButton.setEnabled(sendEnabled);
            sendButton.setBackground(resetCheckBox.isSelected() ? BTN_COLOR_CONNECT : btnColor);
            disconnectButton.setEnabled(disconnectEnabled);
            hostField.setEnabled(fieldsEnabled);
            portField.setEnabled(fieldsEnabled);
            tlsCheckBox.setEnabled(fieldsEnabled);
            resetCheckBox.setEnabled(sendEnabled);
        });
    }

    public void setHost(String host){
        invokeLater(()->hostField.setText(host));
    }

    public void setPort(int port){
        invokeLater(()->portField.setText(String.valueOf(port)));
    }

    public void setTLS(boolean tls){
            invokeLater(()->tlsCheckBox.setSelected(tls));
    }

    public String getHost() {
        return hostField.getText().trim();
    }

    public int getPort() {
        try {
            return Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public boolean isTLS() {
        return tlsCheckBox.isSelected();
    }

    public boolean isReset() {
        return resetCheckBox.isSelected();
    }


}
