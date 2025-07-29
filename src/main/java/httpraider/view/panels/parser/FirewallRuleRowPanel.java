package httpraider.view.panels.parser;

import httpraider.model.network.FirewallRule;
import httpraider.view.panels.JSCodeEditorPanel;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class FirewallRuleRowPanel extends JPanel {
    private final JComboBox<FirewallRule.Source> sourceCombo;
    private final JButton codeButton;
    private final JCheckBox closeConnectionCheckBox;
    private final JButton removeButton;
    private final JCheckBox enabledCheckBox;
    
    private FirewallRule rule;

    public FirewallRuleRowPanel(FirewallRule rule, Consumer<FirewallRuleRowPanel> removeCallback) {
        this.rule = rule;
        setLayout(new FlowLayout(FlowLayout.LEFT, 10, 4));
        setOpaque(true);
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(10, 40));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        add(new JLabel("Active:"));
        enabledCheckBox = new JCheckBox();
        enabledCheckBox.setToolTipText("Enable/Disable this rule");
        add(enabledCheckBox);

        add(new JLabel("Source:"));
        sourceCombo = new JComboBox<>(FirewallRule.Source.values());
        sourceCombo.setPreferredSize(new Dimension(120, sourceCombo.getPreferredSize().height));
        sourceCombo.addActionListener(e -> {
            // Set default code when source changes and code is empty
            if (rule.getJsCode() == null || rule.getJsCode().isEmpty() || 
                rule.getJsCode().equals("// Return true to block the request\nreturn false;")) {
                rule.setJsCode(getDefaultCodeForSource((FirewallRule.Source) sourceCombo.getSelectedItem()));
            }
        });
        add(sourceCombo);

        codeButton = new JButton("Code");
        codeButton.addActionListener(e -> openJsEditorDialog());
        add(codeButton);

        closeConnectionCheckBox = new JCheckBox("Close connection");
        closeConnectionCheckBox.setToolTipText("Close the connection when this rule is triggered");
        add(closeConnectionCheckBox);

        removeButton = new JButton("–");
        removeButton.addActionListener(e -> removeCallback.accept(this));
        add(removeButton);

        updateFromRule(rule);
        
        // Initialize with default code if empty
        if (rule.getJsCode() == null || rule.getJsCode().isEmpty()) {
            rule.setJsCode(getDefaultCodeForSource(rule.getSource()));
        }
    }

    public void updateFromRule(FirewallRule rule) {
        this.rule = rule;
        if (rule.getSource() != null) {
            sourceCombo.setSelectedItem(rule.getSource());
        }
        closeConnectionCheckBox.setSelected(rule.isCloseConnection());
        enabledCheckBox.setSelected(rule.isEnabled());
    }

    public FirewallRule toModel() {
        FirewallRule r = new FirewallRule();
        r.setSource((FirewallRule.Source) sourceCombo.getSelectedItem());
        r.setJsCode(rule.getJsCode());
        r.setCloseConnection(closeConnectionCheckBox.isSelected());
        r.setEnabled(enabledCheckBox.isSelected());
        return r;
    }

    private void openJsEditorDialog() {
        String defaultCode = getDefaultCodeForSource((FirewallRule.Source) sourceCombo.getSelectedItem());
        String currentCode = (rule.getJsCode() != null && !rule.getJsCode().isEmpty()) ? 
                            rule.getJsCode() : defaultCode;
        
        JSCodeEditorPanel editorPanel = new JSCodeEditorPanel(currentCode);
        JScrollPane scrollPane = new JScrollPane(editorPanel);
        scrollPane.setPreferredSize(new Dimension(700, 400));
        
        int res = JOptionPane.showConfirmDialog(
                this,
                scrollPane,
                "Edit Firewall Rule Code - " + sourceCombo.getSelectedItem(),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        
        if (res == JOptionPane.OK_OPTION) {
            rule.setJsCode(editorPanel.getCode());
        }
    }

    private String getDefaultCodeForSource(FirewallRule.Source source) {
        switch (source) {
            case METHOD:
                return """
                    // Input variable: 'input' contains the HTTP method as a string (e.g., "GET", "POST", "TRACE")
                    // Return: true to block the request, false to allow
                    
                    function isMethod(method) {
                        return input.toUpperCase() === method.toUpperCase();
                    }
                    
                    function isMethodInList(methodList) {
                        for (var i = 0; i < methodList.length; i++) {
                            if (input.toUpperCase() === methodList[i].toUpperCase()) {
                                return true;
                            }
                        }
                        return false;
                    }
                    
                    // Your code here
                    return false;
                    """;
            
            case URL:
                return """
                    // Input variable: 'input' contains the URL path as a string (e.g., "/api/users", "/admin/config.php")
                    // Return: true to block the request, false to allow
                    
                    function containsPath(path) {
                        return input.toLowerCase().includes(path.toLowerCase());
                    }
                    
                    function startsWithPath(path) {
                        return input.toLowerCase().startsWith(path.toLowerCase());
                    }
                    
                    function endsWithExtension(extension) {
                        return input.toLowerCase().endsWith(extension.toLowerCase());
                    }
                    
                    // Your code here
                    return false;
                    """;
            
            case VERSION:
                return """
                    // Input variable: 'input' contains the HTTP version as a string (e.g., "HTTP/1.1", "HTTP/2")
                    // Return: true to block the request, false to allow
                    
                    function isVersion(version) {
                        return input === version;
                    }
                    
                    // Your code here
                    return false;
                    """;
            
            case HEADERS:
                return """
                    // Input variable: 'headers' contains an array of header strings (without the request line)
                    // Each header is a string like "Host: example.com" or "User-Agent: Mozilla/5.0"
                    // Return: true to block the request, false to allow
                    
                    function getHeaderValue(headerName) {
                        var searchName = headerName.toLowerCase() + ':';
                        for (var i = 0; i < headers.length; i++) {
                            var lowerHeader = headers[i].toLowerCase();
                            if (lowerHeader.startsWith(searchName)) {
                                return headers[i].substring(headerName.length + 1).trim();
                            }
                        }
                        return null;
                    }
                    
                    function hasHeader(headerName) {
                        return getHeaderValue(headerName) !== null;
                    }
                    
                    function countHeaders(headerName) {
                        var count = 0;
                        var searchName = headerName.toLowerCase() + ':';
                        for (var i = 0; i < headers.length; i++) {
                            if (headers[i].toLowerCase().startsWith(searchName)) {
                                count++;
                            }
                        }
                        return count;
                    }
                    
                    // Your code here
                    return false;
                    """;
            
            case BODY:
                return """
                    // Input variable: 'input' contains the request body as a string
                    // Return: true to block the request, false to allow
                    
                    function containsText(text) {
                        return input.toLowerCase().includes(text.toLowerCase());
                    }
                    
                    function matchesRegex(pattern) {
                        try {
                            var regex = new RegExp(pattern, 'i');
                            return regex.test(input);
                        } catch (e) {
                            return false;
                        }
                    }
                    
                    // Your code here
                    return false;
                    """;
            
            case FULL_REQUEST:
            default:
                return """
                    // Input variable: 'input' contains the full HTTP request as a string
                    // Return: true to block the request, false to allow
                    
                    function getRequestLine() {
                        var lines = input.split('\\n');
                        return lines.length > 0 ? lines[0].trim() : '';
                    }
                    
                    function getMethod() {
                        var requestLine = getRequestLine();
                        var parts = requestLine.split(' ');
                        return parts.length > 0 ? parts[0] : '';
                    }
                    
                    function getUrl() {
                        var requestLine = getRequestLine();
                        var parts = requestLine.split(' ');
                        return parts.length > 1 ? parts[1] : '';
                    }
                    
                    // Your code here
                    return false;
                    """;
        }
    }
}