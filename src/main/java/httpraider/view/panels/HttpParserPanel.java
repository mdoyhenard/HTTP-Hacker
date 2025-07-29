package httpraider.view.panels;

import burp.api.montoya.ui.editor.EditorOptions;
import extension.HTTPRaiderExtension;
import httpraider.view.panels.parser.HeaderLinesParserPanel;
import httpraider.view.panels.parser.LoadBalancingParserPanel;
import httpraider.view.panels.parser.MessageLengthParserPanel;
import httpraider.view.panels.parser.RequestLineParserPanel;
import httpraider.view.panels.parser.FirewallRulesParserPanel;

import javax.swing.*;
import java.awt.*;

public class HttpParserPanel extends JDialog {

    private final JTabbedPane tabbedPane;
    private final HeaderLinesParserPanel headerLinesParserPanel;
    private final RequestLineParserPanel requestLineParserPanel;
    private final MessageLengthParserPanel messageLengthParserPanel;
    private final LoadBalancingParserPanel loadBalancingParserPanel;
    private final FirewallRulesParserPanel firewallRulesParserPanel;

    private final JButton testButton;
    private final JButton saveButton;

    private final HttpEditorPanel inputEditorPanel;
    private final HttpMultiEditorPanel resultEditorPanel;

    private final JSplitPane mainSplitPane;
    private final JSplitPane editorsSplitPane;

    public HttpParserPanel(Window parent) {
        super(parent, "HTTP Parser Configuration", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 800));
        setPreferredSize(new Dimension(1100, 850));

        // --- Tabs
        tabbedPane = new JTabbedPane();
        headerLinesParserPanel = new HeaderLinesParserPanel();
        requestLineParserPanel = new RequestLineParserPanel();
        messageLengthParserPanel = new MessageLengthParserPanel();
        loadBalancingParserPanel = new LoadBalancingParserPanel();
        firewallRulesParserPanel = new FirewallRulesParserPanel();

        tabbedPane.addTab("Headers", headerLinesParserPanel);
        tabbedPane.addTab("Request Line", requestLineParserPanel);
        tabbedPane.addTab("Message-Length", messageLengthParserPanel);
        tabbedPane.addTab("Forwarding Rules", loadBalancingParserPanel);
        tabbedPane.addTab("Firewall Rules", firewallRulesParserPanel);

        // --- Center bar with Test/Save and border
        JPanel buttonBar = new JPanel();
        buttonBar.setLayout(new BoxLayout(buttonBar, BoxLayout.Y_AXIS));
        buttonBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(180,180,180)));
        JPanel innerFlowPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 24, 4));
        testButton = new JButton("Test");
        saveButton = new JButton("Save");
        innerFlowPanel.add(testButton);
        innerFlowPanel.add(saveButton);
        buttonBar.add(Box.createVerticalStrut(8));
        buttonBar.add(innerFlowPanel);
        buttonBar.add(Box.createVerticalStrut(2));
        buttonBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        // --- Top: Config + Button Bar
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());
        topPanel.add(tabbedPane, BorderLayout.CENTER);
        topPanel.add(buttonBar, BorderLayout.SOUTH);

        // --- Editors (Bottom)
        inputEditorPanel = new HttpEditorPanel("Client Request",
                HTTPRaiderExtension.API.userInterface().createHttpRequestEditor());
        resultEditorPanel = new HttpMultiEditorPanel("Parsed Request",
                HTTPRaiderExtension.API.userInterface().createHttpRequestEditor(EditorOptions.READ_ONLY));

        editorsSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, inputEditorPanel, resultEditorPanel);
        editorsSplitPane.setResizeWeight(0.5);

        // --- Main vertical split: Top = config+bar, Bottom = editors
        mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topPanel, editorsSplitPane);
        mainSplitPane.setResizeWeight(0.40);
        mainSplitPane.setDividerSize(7);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(mainSplitPane, BorderLayout.CENTER);
        pack();
        
        // Position dialog on same screen as parent
        if (parent != null && parent.isShowing()) {
            // Get the parent's screen device
            GraphicsConfiguration gc = parent.getGraphicsConfiguration();
            Rectangle screenBounds = gc.getBounds();
            
            // Get dialog dimensions
            Dimension dialogSize = getSize();
            
            // Calculate center position on parent's screen
            int x = screenBounds.x + (screenBounds.width - dialogSize.width) / 2;
            int y = screenBounds.y + (screenBounds.height - dialogSize.height) / 2;
            
            // Set location
            setLocation(x, y);
        } else {
            // Fallback to center on default screen
            setLocationRelativeTo(null);
        }
    }

    public HeaderLinesParserPanel getHeaderLinesParserPanel() { return headerLinesParserPanel; }
    public RequestLineParserPanel getRequestLineParserPanel() { return requestLineParserPanel; }
    public MessageLengthParserPanel getMessageLengthParserPanel() { return messageLengthParserPanel; }
    public LoadBalancingParserPanel getLoadBalancingParserPanel() { return loadBalancingParserPanel; }
    public FirewallRulesParserPanel getFirewallRulesParserPanel() { return firewallRulesParserPanel; }
    public JButton getTestButton() { return testButton; }
    public JButton getSaveButton() { return saveButton; }
    public HttpEditorPanel getInputEditorPanel() { return inputEditorPanel; }
    public HttpMultiEditorPanel getResultEditorPanel() { return resultEditorPanel; }
    public JSplitPane getEditorsSplitPane() { return editorsSplitPane; }
    public JTabbedPane getTabbedPane() { return tabbedPane; }
    
    public void setInitialRequest(byte[] request) {
        if (request != null && request.length > 0) {
            inputEditorPanel.setBytes(request);
        }
    }
}
