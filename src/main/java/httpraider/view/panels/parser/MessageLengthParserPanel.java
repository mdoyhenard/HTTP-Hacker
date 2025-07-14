package httpraider.view.panels.parser;

import httpraider.model.network.HttpParserModel;
import httpraider.view.panels.JSCodeEditorPanel;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;

public class MessageLengthParserPanel extends JPanel {
    private final BasicParserMessageLengthHeadersPanel messageLengthHeadersPanel;
    private final BasicParserChunkedEndingsPanel chunkedEndingsPanel;
    private final JComboBox<HttpParserModel.MessageLenBodyEncoding> encodingCombo;

    private final JCheckBox useJsCheckbox;
    private final JSCodeEditorPanel jsEditorPanel;

    public static final int SETTINGS_PANEL_WIDTH = 330;

    public MessageLengthParserPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        // Settings (left) panel
        JPanel settingsPanel = new JPanel();
        settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
        settingsPanel.setPreferredSize(new Dimension(SETTINGS_PANEL_WIDTH, 10));
        settingsPanel.setMaximumSize(new Dimension(SETTINGS_PANEL_WIDTH, Integer.MAX_VALUE));

        messageLengthHeadersPanel = new BasicParserMessageLengthHeadersPanel();
        chunkedEndingsPanel = new BasicParserChunkedEndingsPanel();

        JPanel encodingPanel = new JPanel();
        TitledBorder border = BorderFactory.createTitledBorder("Forwarded Encoding");
        Font font = UIManager.getFont("TitledBorder.font");
        border.setTitleFont(font.deriveFont(12f));
        encodingPanel.setBorder(border);

        encodingCombo = new JComboBox<>(HttpParserModel.MessageLenBodyEncoding.values());
        encodingCombo.setMaximumSize(new Dimension(250, 26));
        encodingCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        encodingPanel.add(Box.createVerticalStrut(1), BorderLayout.NORTH);
        encodingPanel.add(Box.createVerticalStrut(1), BorderLayout.SOUTH);
        encodingPanel.add(Box.createHorizontalStrut(1), BorderLayout.EAST);
        encodingPanel.add(Box.createHorizontalStrut(1), BorderLayout.WEST);
        encodingPanel.add(encodingCombo, BorderLayout.CENTER);

        settingsPanel.add(messageLengthHeadersPanel);
        settingsPanel.add(Box.createVerticalStrut(10));
        settingsPanel.add(chunkedEndingsPanel);
        settingsPanel.add(Box.createVerticalStrut(10));
        settingsPanel.add(encodingPanel);
        settingsPanel.add(Box.createVerticalGlue());

        // Scripting (right) panel
        JPanel scriptingPanel = new JPanel();
        scriptingPanel.setBorder(BorderFactory.createTitledBorder("Advanced JavaScript Parser"));
        scriptingPanel.setLayout(new BorderLayout());

        useJsCheckbox = new JCheckBox("Use JS Parser");
        JPanel topBox = new JPanel();
        topBox.setLayout(new BoxLayout(topBox, BoxLayout.Y_AXIS));
        topBox.setOpaque(false);
        topBox.add(useJsCheckbox);
        topBox.add(Box.createVerticalStrut(8));

        jsEditorPanel = new JSCodeEditorPanel();
        jsEditorPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        jsEditorPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        scriptingPanel.add(topBox, BorderLayout.NORTH);
        scriptingPanel.add(jsEditorPanel, BorderLayout.CENTER);

        // Vertical separator
        JSeparator verticalSeparator = new JSeparator(SwingConstants.VERTICAL);
        verticalSeparator.setPreferredSize(new Dimension(5, 10));
        verticalSeparator.setMaximumSize(new Dimension(5, Integer.MAX_VALUE));
        verticalSeparator.setForeground(Color.DARK_GRAY);
        verticalSeparator.setBackground(Color.DARK_GRAY);

        JPanel combinedPanel = new JPanel();
        combinedPanel.setLayout(new BoxLayout(combinedPanel, BoxLayout.X_AXIS));
        combinedPanel.add(settingsPanel);
        combinedPanel.add(Box.createHorizontalStrut(10));
        combinedPanel.add(verticalSeparator);
        combinedPanel.add(Box.createHorizontalStrut(10));
        combinedPanel.add(scriptingPanel);

        add(combinedPanel, BorderLayout.CENTER);

        useJsCheckbox.addActionListener(e -> updateJsEnabled());
        updateJsEnabled();

        messageLengthHeadersPanel.setChunkedStateChangedListener(() -> {
            boolean show = messageLengthHeadersPanel.anyChunked();
            chunkedEndingsPanel.setVisible(show);
            revalidate();
            repaint();
        });

        chunkedEndingsPanel.setVisible(messageLengthHeadersPanel.anyChunked());
    }

    private void updateJsEnabled() {
        jsEditorPanel.setEnabled(useJsCheckbox.isSelected());
        jsEditorPanel.getTextPane().setEditable(useJsCheckbox.isSelected());
        jsEditorPanel.getTextPane().setBackground(useJsCheckbox.isSelected() ? Color.WHITE : new Color(245,245,245));
    }

    public BasicParserMessageLengthHeadersPanel getMessageLengthHeadersPanel() { return messageLengthHeadersPanel; }
    public BasicParserChunkedEndingsPanel getChunkedEndingsPanel() { return chunkedEndingsPanel; }

    public JTable getTable() { return messageLengthHeadersPanel.getTable(); }
    public List<HttpParserModel.BodyLenHeaderRule> getRules() { return messageLengthHeadersPanel.getRules(); }
    public void setRules(List<HttpParserModel.BodyLenHeaderRule> rules) { messageLengthHeadersPanel.setRules(rules); }
    public boolean anyChunked() { return messageLengthHeadersPanel.anyChunked(); }
    public void setChunkedStateChangedListener(Runnable r) { messageLengthHeadersPanel.setChunkedStateChangedListener(r); }

    public List<String> getChunkedEndings() { return chunkedEndingsPanel.getChunkedEndings(); }
    public void setChunkedEndings(List<String> endings) { chunkedEndingsPanel.setChunkedEndings(endings); }

    public boolean isUseJsParser() { return useJsCheckbox.isSelected(); }
    public void setUseJsParser(boolean b) {
        useJsCheckbox.setSelected(b);
        updateJsEnabled();
    }
    public String getJsScript() { return jsEditorPanel.getCode(); }
    public void setJsScript(String code) {
        jsEditorPanel.setCode(code != null ? code : "");
    }
    public HttpParserModel.MessageLenBodyEncoding getOutputEncoding() {
        return (HttpParserModel.MessageLenBodyEncoding) encodingCombo.getSelectedItem();
    }
    public void setOutputEncoding(HttpParserModel.MessageLenBodyEncoding encoding) {
        encodingCombo.setSelectedItem(encoding != null ? encoding : HttpParserModel.MessageLenBodyEncoding.DONT_MODIFY);
    }
}
