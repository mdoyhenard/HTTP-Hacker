package httpraider.view.panels.parser;

import httpraider.view.panels.JSCodeEditorPanel;

import javax.swing.*;
import java.awt.*;

public class HeaderLinesParserPanel extends JPanel {
    private final BasicParserHeaderLineEndingsPanel headerLineEndingsPanel;
    private final BasicParserHeaderFoldingPanel headerFoldingPanel;
    private final BasicParserHeaderDeletePanel headerDeletePanel;
    private final BasicParserHeaderAddPanel headerAddPanel;

    private final JCheckBox useJsCheckbox;
    private final JSCodeEditorPanel jsEditorPanel;

    public static final int SETTINGS_PANEL_WIDTH = 250;

    public HeaderLinesParserPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JPanel settingsPanel = new JPanel();
        settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
        settingsPanel.setPreferredSize(new Dimension(SETTINGS_PANEL_WIDTH, 8));
        settingsPanel.setMaximumSize(new Dimension(SETTINGS_PANEL_WIDTH, Integer.MAX_VALUE));

        headerLineEndingsPanel = new BasicParserHeaderLineEndingsPanel(3); // 3 rows for short height
        headerFoldingPanel = new BasicParserHeaderFoldingPanel();
        headerDeletePanel = new BasicParserHeaderDeletePanel();
        headerAddPanel = new BasicParserHeaderAddPanel();

        settingsPanel.add(headerLineEndingsPanel);
        settingsPanel.add(Box.createVerticalStrut(10));
        settingsPanel.add(headerFoldingPanel);
        settingsPanel.add(Box.createVerticalStrut(10));
        settingsPanel.add(headerDeletePanel);
        settingsPanel.add(Box.createVerticalStrut(10));
        settingsPanel.add(headerAddPanel);
        settingsPanel.add(Box.createVerticalGlue());

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
        jsEditorPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        jsEditorPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        scriptingPanel.add(topBox, BorderLayout.NORTH);
        scriptingPanel.add(jsEditorPanel, BorderLayout.CENTER);

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
    }

    private void updateJsEnabled() {
        jsEditorPanel.setEnabled(useJsCheckbox.isSelected());
        jsEditorPanel.getTextPane().setEditable(useJsCheckbox.isSelected());
        jsEditorPanel.getTextPane().setBackground(useJsCheckbox.isSelected() ? Color.WHITE : new Color(245,245,245));
    }

    public BasicParserHeaderLineEndingsPanel getHeaderLineEndingsPanel() { return headerLineEndingsPanel; }
    public BasicParserHeaderFoldingPanel getHeaderFoldingPanel() { return headerFoldingPanel; }
    public BasicParserHeaderDeletePanel getHeaderDeletePanel() { return headerDeletePanel; }
    public BasicParserHeaderAddPanel getHeaderAddPanel() { return headerAddPanel; }

    public boolean isUseJsParser() { return useJsCheckbox.isSelected(); }
    public void setUseJsParser(boolean b) {
        useJsCheckbox.setSelected(b);
        updateJsEnabled();
    }
    public String getJsScript() { return jsEditorPanel.getCode(); }
    public void setJsScript(String code) {
        jsEditorPanel.setCode(code != null ? code : "");
    }
}
