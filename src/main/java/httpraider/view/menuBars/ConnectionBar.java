package httpraider.view.menuBars;

import httpraider.view.filters.DigitDocumentFilter;
import javax.swing.*;
import javax.swing.text.AbstractDocument;
import java.awt.*;
import java.awt.event.ActionListener;

public class ConnectionBar extends JPanel {

    public enum Action { CONNECT, DISCONNECT }

    private static final Color DISCONNECTED_BACK        = new Color(0x2AE1A9A9, true);
    private static final Color DISCONNECTED_FONT        = new Color(0xCFB51414, true);
    private static final Color CONNECTED_BACK           = new Color(0x3A7FCC6A, true);
    private static final Color CONNECTED_FONT           = new Color(0xFF5B934C, true);
    private static final Color BTN_CONNECTED_COLOR   = new Color(255,95,44);
    private static final Color BTN_DISCONNECTED_COLOR      = new Color(95, 39, 232);

    private static final String BTN_DISCONNECTED = "CONNECT & Send";
    private static final String BTN_CONNECTED = "Send";
    private static final String DISCONNECTED_STATUS = "DISCONNECTED";
    private static final String CONNECTED_STATUS = "CONNECTED";

    private final JButton sendButton;
    private final JCheckBox resetCheckBox;
    private final JTextField hostField;
    private final JTextField portField;
    private final JCheckBox httpsCheckBox;
    private final JButton disconnectButton;
    private final JLabel statusMsg;

    public ConnectionBar(){
        super(new BorderLayout());
        setOpaque(true);
        setBackground(DISCONNECTED_BACK);

        sendButton       = new JButton(BTN_DISCONNECTED);
        resetCheckBox    = new JCheckBox();
        statusMsg        = new JLabel(DISCONNECTED_STATUS);
        hostField        = new JTextField("localhost",15);
        portField        = new JTextField("80",5);
        httpsCheckBox    = new JCheckBox();
        disconnectButton = new JButton("Disconnect");

        sendButton.setActionCommand(Action.CONNECT.name());
        disconnectButton.setActionCommand(Action.DISCONNECT.name());
        ((AbstractDocument)portField.getDocument()).setDocumentFilter(new DigitDocumentFilter(5));

        resetCheckBox.setOpaque(false);
        httpsCheckBox.setOpaque(false);
        sendButton.setBackground(BTN_DISCONNECTED_COLOR);
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);
        sendButton.setBorderPainted(false);
        sendButton.setOpaque(true);
        sendButton.setFont(sendButton.getFont().deriveFont(Font.BOLD));
        statusMsg.setForeground(DISCONNECTED_FONT);
        statusMsg.setHorizontalAlignment(SwingConstants.CENTER);
        statusMsg.setFont(statusMsg.getFont().deriveFont(Font.BOLD,15));
        disconnectButton.setEnabled(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel centre = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        left.setOpaque(false); centre.setOpaque(false); right.setOpaque(false);

        left.add(sendButton); left.add(resetCheckBox); left.add(new JLabel("Reset connection"));
        centre.add(statusMsg);
        right.add(new JLabel("Host:")); right.add(hostField);
        right.add(new JLabel("Port:")); right.add(portField);
        right.add(httpsCheckBox); right.add(new JLabel("TLS"));
        right.add(disconnectButton);

        add(left,BorderLayout.WEST); add(centre,BorderLayout.CENTER); add(right,BorderLayout.EAST);
    }

    public void setSendActionListener(ActionListener l){
        sendButton.addActionListener(l);
    }

    public void setDisconnectActionListener(ActionListener l){
        sendButton.addActionListener(l);
    }

    public void setConnected(boolean connected){
        SwingUtilities.invokeLater(() -> {
            statusMsg.setText(connected ? CONNECTED_STATUS : DISCONNECTED_STATUS);
            statusMsg.setForeground(connected ? CONNECTED_FONT : DISCONNECTED_FONT);
            setBackground(connected ? CONNECTED_BACK : DISCONNECTED_BACK);
            sendButton.setText(connected ? BTN_CONNECTED : BTN_DISCONNECTED);
            sendButton.setBackground(connected ? BTN_CONNECTED_COLOR : BTN_DISCONNECTED_COLOR);
            disconnectButton.setEnabled(connected);
        });
    }

    public String getHost(){ return hostField.getText().trim(); }
    public int getPort(){ try{ return Integer.parseInt(portField.getText().trim()); }catch(NumberFormatException e){ return -1; } }
    public boolean isHttps(){ return httpsCheckBox.isSelected(); }
    public boolean isReset(){ return resetCheckBox.isSelected(); }
    public boolean isTlsChecked(){ return httpsCheckBox.isSelected(); }
    public boolean isResetChecked(){ return resetCheckBox.isSelected(); }
}
