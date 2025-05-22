package View.MenuBars;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionListener;

public class ConnectionBar extends JPanel {

    private static final Color DISCONNECTED_BACK = new Color(0x2AE1A9A9, true);
    private static final Color DISCONNECTED_FONT = new Color(0xCFB51414, true);

    private static final Color CONNECTED_BACK = new Color(0x3A7FCC6A, true);
    private static final Color CONNECTED_FONT = new Color(0xFF5B934C, true);

    private final JButton     sendButton;
    private final JCheckBox   resetCheckBox;
    private final JTextField  hostField;
    private final JTextField  portField;
    private final JCheckBox   httpsCheckBox;
    private final JButton     disconnectButton;
    private final JLabel      statusMsg;

    public ConnectionBar() {
        super(new BorderLayout());
        setOpaque(true);

        /* ------- create components ------- */
        sendButton      = new JButton("Connect & Send");
        resetCheckBox   = new JCheckBox();
        statusMsg       = new JLabel("Disconnected");
        hostField       = new JTextField("localhost", 15);
        portField       = new JTextField("80", 5);
        httpsCheckBox   = new JCheckBox();
        disconnectButton= new JButton("Disconnect");

        JLabel resetLabel = new JLabel("Reset connection");
        JLabel httpsLabel = new JLabel("TLS");

        resetCheckBox.setOpaque(false);
        statusMsg.setOpaque(false);
        hostField.setOpaque(false);
        portField.setOpaque(false);
        httpsCheckBox.setOpaque(false);

        /* ------- tweak look-&-feel (unchanged) ------- */
        sendButton.setBackground(new Color(255, 95, 44));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);
        sendButton.setBorderPainted(false);
        sendButton.setOpaque(true);
        sendButton.setFont(sendButton.getFont().deriveFont(Font.BOLD));

        statusMsg.setForeground(new Color(200,  80,  80));
        statusMsg.setHorizontalAlignment(SwingConstants.CENTER);
        statusMsg.setFont(statusMsg.getFont().deriveFont(Font.BOLD, 15));

        disconnectButton.setEnabled(false);

        /* ------- lay out left / centre / right ------- */
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel centrePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        leftPanel.setOpaque(false);
        centrePanel.setOpaque(false);
        rightPanel.setOpaque(false);

        leftPanel.add(sendButton);
        leftPanel.add(resetCheckBox);
        leftPanel.add(resetLabel);

        centrePanel.add(statusMsg);

        rightPanel.add(new JLabel("Host:"));
        rightPanel.add(hostField);
        rightPanel.add(new JLabel("Port:"));
        rightPanel.add(portField);
        rightPanel.add(httpsCheckBox);
        rightPanel.add(httpsLabel);
        rightPanel.add(disconnectButton);

        add(leftPanel,   BorderLayout.WEST);
        add(centrePanel, BorderLayout.CENTER);
        add(rightPanel,  BorderLayout.EAST);
    }

    /** Call once to be notified whenever ANY button / check-box is clicked. */
    public void addActionListenerToAllButtons(ActionListener l) {
        // anything derived from AbstractButton fires ActionEvents
        sendButton     .addActionListener(l);
        resetCheckBox  .addActionListener(l);
        httpsCheckBox  .addActionListener(l);
        disconnectButton.addActionListener(l);

        // JTextField also fires an ActionEvent when the user presses Enter
        hostField      .addActionListener(l);
        portField      .addActionListener(l);
    }

    /** Call once to be notified every time the text of either field changes. */
    public void addDocumentListenerToTextFields(DocumentListener dl) {
        hostField.getDocument().addDocumentListener(dl);
        portField.getDocument().addDocumentListener(dl);
    }

    public void setConnected(boolean connected) {
        SwingUtilities.invokeLater(() -> {
            statusMsg.setText(connected ? "Connected" : "Disconnected");
            statusMsg.setForeground(connected ? CONNECTED_FONT : DISCONNECTED_FONT);
            setBackground(connected ? CONNECTED_BACK : DISCONNECTED_BACK);
        });
    }

}
