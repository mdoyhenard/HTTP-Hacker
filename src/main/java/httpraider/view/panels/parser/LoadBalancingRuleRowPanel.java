package httpraider.view.panels.parser;

import httpraider.model.network.*;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class LoadBalancingRuleRowPanel extends JPanel {
    private final JComboBox<ComboItem> forwardToCombo;
    private final JComboBox<RuleType> typeCombo;
    private final JComboBox<HeaderField> headerFieldCombo;
    private final JComboBox<MatchMode> matchModeCombo;
    private final JComboBox<MatchMode> urlMatchOptionCombo;
    private final JTextField patternField;
    private final JTextField methodField;
    private final JTextField bodyField;
    private final JButton jsButton;
    private final JButton removeButton;

    // Host UI
    private final JLabel hostLabel;
    private final JComboBox<MatchMode> hostMatchModeCombo;
    private final JTextField hostPatternField;

    // Cookies UI
    private final JLabel cookieNameLabel;
    private final JComboBox<MatchMode> cookieNameMatchModeCombo;
    private final JTextField cookieNamePatternField;
    private final JLabel cookieValueLabel;
    private final JComboBox<MatchMode> cookieValueMatchModeCombo;
    private final JTextField cookieValuePatternField;

    // Header Name/Value UI
    private final JLabel headerNameLabel;
    private final JComboBox<MatchMode> headerNameMatchModeCombo;
    private final JTextField headerNamePatternField;
    private final JLabel headerValueLabel;
    private final JComboBox<MatchMode> headerValueMatchModeCombo;
    private final JTextField headerValuePatternField;

    private final Map<String, String> proxyIdToName = new LinkedHashMap<>();
    private LoadBalancingRule rule;

    public LoadBalancingRuleRowPanel(LoadBalancingRule rule, Map<String, String> proxies, Consumer<LoadBalancingRuleRowPanel> removeCallback) {
        this.rule = rule;
        setLayout(new FlowLayout(FlowLayout.LEFT, 10, 4));
        setOpaque(true);
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(10, 40));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        forwardToCombo = new JComboBox<>();
        forwardToCombo.addItem(new ComboItem("Disabled", null));
        if (proxies != null) {
            for (Map.Entry<String, String> entry : proxies.entrySet()) {
                forwardToCombo.addItem(new ComboItem(entry.getValue(), entry.getKey()));
                proxyIdToName.put(entry.getKey(), entry.getValue());
            }
        }
        add(new JLabel("Forward to:"));
        add(forwardToCombo);

        typeCombo = new JComboBox<>(RuleType.values());
        add(new JLabel("Type:"));
        add(typeCombo);

        headerFieldCombo = new JComboBox<>(HeaderField.values());
        add(headerFieldCombo);

        matchModeCombo = new JComboBox<>(MatchMode.values());
        add(matchModeCombo);

        patternField = new JTextField(10);
        add(patternField);

        urlMatchOptionCombo = new JComboBox<>(MatchMode.values());
        add(urlMatchOptionCombo);

        methodField = new JTextField(8);
        add(methodField);

        bodyField = new JTextField(10);
        add(bodyField);

        // Host UI
        hostLabel = new JLabel("Host:");
        hostMatchModeCombo = new JComboBox<>(MatchMode.values());
        hostPatternField = new JTextField(12);
        add(hostLabel);
        add(hostMatchModeCombo);
        add(hostPatternField);

        // Cookies UI
        cookieNameLabel = new JLabel("Cookie Name:");
        cookieNameMatchModeCombo = new JComboBox<>(MatchMode.values());
        cookieNamePatternField = new JTextField(8);
        cookieValueLabel = new JLabel("Cookie Value:");
        cookieValueMatchModeCombo = new JComboBox<>(MatchMode.values());
        cookieValuePatternField = new JTextField(8);
        add(cookieNameLabel);
        add(cookieNameMatchModeCombo);
        add(cookieNamePatternField);
        add(cookieValueLabel);
        add(cookieValueMatchModeCombo);
        add(cookieValuePatternField);

        // Header Name/Value UI
        headerNameLabel = new JLabel("Header Name:");
        headerNameMatchModeCombo = new JComboBox<>(MatchMode.values());
        headerNamePatternField = new JTextField(8);
        headerValueLabel = new JLabel("Header Value:");
        headerValueMatchModeCombo = new JComboBox<>(MatchMode.values());
        headerValuePatternField = new JTextField(8);
        add(headerNameLabel);
        add(headerNameMatchModeCombo);
        add(headerNamePatternField);
        add(headerValueLabel);
        add(headerValueMatchModeCombo);
        add(headerValuePatternField);

        jsButton = new JButton("Code...");
        jsButton.addActionListener(e -> openJsEditorDialog());
        add(jsButton);

        removeButton = new JButton("–");
        removeButton.addActionListener(e -> removeCallback.accept(this));
        add(removeButton);

        updateFromRule(rule);

        forwardToCombo.addActionListener(e -> updateRowUI());
        typeCombo.addActionListener(e -> updateRowUI());
        headerFieldCombo.addActionListener(e -> updateRowUI());
        updateRowUI();
    }

    public void setEligibleProxies(Map<String, String> proxies) {
        String prevId = getSelectedProxyId();
        forwardToCombo.removeAllItems();
        proxyIdToName.clear();

        forwardToCombo.addItem(new ComboItem("Disabled", null));
        if (proxies != null) {
            for (Map.Entry<String, String> entry : proxies.entrySet()) {
                forwardToCombo.addItem(new ComboItem(entry.getValue(), entry.getKey()));
                proxyIdToName.put(entry.getKey(), entry.getValue());
            }
        }
        if (prevId == null) {
            forwardToCombo.setSelectedIndex(0);
        } else {
            for (int i = 0; i < forwardToCombo.getItemCount(); i++) {
                ComboItem item = forwardToCombo.getItemAt(i);
                if (prevId.equals(item.id)) {
                    forwardToCombo.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    private String getSelectedProxyId() {
        ComboItem item = (ComboItem) forwardToCombo.getSelectedItem();
        return (item != null) ? item.id : null;
    }

    public void updateFromRule(LoadBalancingRule rule) {
        this.rule = rule;
        String val = rule.getForwardToProxyId();
        if (val == null) {
            forwardToCombo.setSelectedIndex(0);
        } else {
            for (int i = 0; i < forwardToCombo.getItemCount(); i++) {
                ComboItem item = forwardToCombo.getItemAt(i);
                if (val.equals(item.id)) {
                    forwardToCombo.setSelectedIndex(i);
                    break;
                }
            }
        }
        if (rule.getRuleType() != null) typeCombo.setSelectedItem(rule.getRuleType());

        if (rule.getHeaderField() != null) headerFieldCombo.setSelectedItem(rule.getHeaderField());
        if (rule.getMatchMode() != null) matchModeCombo.setSelectedItem(rule.getMatchMode());
        if (rule.getMatchOption() != null) urlMatchOptionCombo.setSelectedItem(rule.getMatchOption());

        patternField.setText(rule.getPattern() != null ? rule.getPattern() : "");
        methodField.setText(rule.getPattern() != null ? rule.getPattern() : "");
        bodyField.setText(rule.getPattern() != null ? rule.getPattern() : "");

        hostPatternField.setText(rule.getHostPattern() != null ? rule.getHostPattern() : "");
        hostMatchModeCombo.setSelectedItem(rule.getHostMatchMode());

        cookieNamePatternField.setText(rule.getCookieNamePattern() != null ? rule.getCookieNamePattern() : "");
        cookieNameMatchModeCombo.setSelectedItem(rule.getCookieNameMatchMode());
        cookieValuePatternField.setText(rule.getCookieValuePattern() != null ? rule.getCookieValuePattern() : "");
        cookieValueMatchModeCombo.setSelectedItem(rule.getCookieValueMatchMode());

        headerNamePatternField.setText(rule.getHeaderNamePattern() != null ? rule.getHeaderNamePattern() : "");
        headerNameMatchModeCombo.setSelectedItem(rule.getHeaderNameMatchMode());
        headerValuePatternField.setText(rule.getHeaderValuePattern() != null ? rule.getHeaderValuePattern() : "");
        headerValueMatchModeCombo.setSelectedItem(rule.getHeaderValueMatchMode());

        updateRowUI();
    }

    public LoadBalancingRule toModel() {
        LoadBalancingRule r = new LoadBalancingRule();
        ComboItem selected = (ComboItem) forwardToCombo.getSelectedItem();
        String forwardToId = (selected != null) ? selected.id : null;
        r.setForwardToProxyId(forwardToId);
        r.setEnabled(forwardToId != null);
        r.setRuleType((RuleType) typeCombo.getSelectedItem());
        switch (r.getRuleType()) {
            case URL:
                r.setMatchOption((MatchMode) urlMatchOptionCombo.getSelectedItem());
                r.setPattern(patternField.getText());
                break;
            case HEADERS:
                r.setHeaderField((HeaderField) headerFieldCombo.getSelectedItem());
                if (r.getHeaderField() == HeaderField.NAME_VALUE) {
                    r.setHeaderNameMatchMode((MatchMode) headerNameMatchModeCombo.getSelectedItem());
                    r.setHeaderNamePattern(headerNamePatternField.getText());
                    r.setHeaderValueMatchMode((MatchMode) headerValueMatchModeCombo.getSelectedItem());
                    r.setHeaderValuePattern(headerValuePatternField.getText());
                } else {
                    r.setMatchMode((MatchMode) matchModeCombo.getSelectedItem());
                    r.setPattern(patternField.getText());
                }
                break;
            case HOST:
                r.setHostMatchMode((MatchMode) hostMatchModeCombo.getSelectedItem());
                r.setHostPattern(hostPatternField.getText());
                break;
            case COOKIES:
                r.setCookieNameMatchMode((MatchMode) cookieNameMatchModeCombo.getSelectedItem());
                r.setCookieNamePattern(cookieNamePatternField.getText());
                r.setCookieValueMatchMode((MatchMode) cookieValueMatchModeCombo.getSelectedItem());
                r.setCookieValuePattern(cookieValuePatternField.getText());
                break;
            case METHOD:
                r.setPattern(methodField.getText());
                break;
            case BODY:
                r.setPattern(bodyField.getText());
                break;
            case CUSTOM:
                r.setJsCode(rule.getJsCode());
                break;
        }
        return r;
    }

    private void updateRowUI() {
        RuleType selectedType = (RuleType) typeCombo.getSelectedItem();

        headerFieldCombo.setVisible(false);
        matchModeCombo.setVisible(false);
        patternField.setVisible(false);
        urlMatchOptionCombo.setVisible(false);
        methodField.setVisible(false);
        bodyField.setVisible(false);
        jsButton.setVisible(false);

        // Hide all labeled components by default
        hostLabel.setVisible(false);
        hostMatchModeCombo.setVisible(false);
        hostPatternField.setVisible(false);

        cookieNameLabel.setVisible(false);
        cookieNameMatchModeCombo.setVisible(false);
        cookieNamePatternField.setVisible(false);
        cookieValueLabel.setVisible(false);
        cookieValueMatchModeCombo.setVisible(false);
        cookieValuePatternField.setVisible(false);

        headerNameLabel.setVisible(false);
        headerNameMatchModeCombo.setVisible(false);
        headerNamePatternField.setVisible(false);
        headerValueLabel.setVisible(false);
        headerValueMatchModeCombo.setVisible(false);
        headerValuePatternField.setVisible(false);

        if (selectedType == RuleType.URL) {
            urlMatchOptionCombo.setVisible(true);
            patternField.setVisible(true);
        } else if (selectedType == RuleType.HEADERS) {
            headerFieldCombo.setVisible(true);
            HeaderField sel = (HeaderField) headerFieldCombo.getSelectedItem();
            if (sel == HeaderField.NAME_VALUE) {
                headerNameLabel.setVisible(true);
                headerNameMatchModeCombo.setVisible(true);
                headerNamePatternField.setVisible(true);
                headerValueLabel.setVisible(true);
                headerValueMatchModeCombo.setVisible(true);
                headerValuePatternField.setVisible(true);
            } else {
                matchModeCombo.setVisible(true);
                patternField.setVisible(true);
            }
        } else if (selectedType == RuleType.HOST) {
            hostLabel.setVisible(true);
            hostMatchModeCombo.setVisible(true);
            hostPatternField.setVisible(true);
        } else if (selectedType == RuleType.COOKIES) {
            cookieNameLabel.setVisible(true);
            cookieNameMatchModeCombo.setVisible(true);
            cookieNamePatternField.setVisible(true);
            cookieValueLabel.setVisible(true);
            cookieValueMatchModeCombo.setVisible(true);
            cookieValuePatternField.setVisible(true);
        } else if (selectedType == RuleType.METHOD) {
            methodField.setVisible(true);
        } else if (selectedType == RuleType.BODY) {
            bodyField.setVisible(true);
        } else if (selectedType == RuleType.CUSTOM) {
            jsButton.setVisible(true);
        }
        revalidate();
        repaint();
    }

    private void openJsEditorDialog() {
        httpraider.view.panels.JSCodeEditorPanel editorPanel = new httpraider.view.panels.JSCodeEditorPanel(rule.getJsCode() != null ? rule.getJsCode() : "");
        JScrollPane scrollPane = new JScrollPane(editorPanel);
        scrollPane.setPreferredSize(new Dimension(700, 400));
        int res = JOptionPane.showConfirmDialog(
                this,
                scrollPane,
                "Edit JavaScript Code",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        if (res == JOptionPane.OK_OPTION) {
            rule.setJsCode(editorPanel.getCode());
        }
    }

    private static class ComboItem {
        final String name;
        final String id;
        ComboItem(String name, String id) { this.name = name; this.id = id; }
        @Override public String toString() { return name; }
    }
}
