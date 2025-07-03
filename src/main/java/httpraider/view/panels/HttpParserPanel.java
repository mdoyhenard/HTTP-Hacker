package httpraider.view.panels;

import burp.api.montoya.ui.editor.HttpRequestEditor;
import burp.api.montoya.ui.editor.WebSocketMessageEditor;
import burp.api.montoya.ui.editor.EditorOptions;
import extension.HTTPRaiderExtension;

import javax.swing.*;
import java.awt.*;

public class HttpParserPanel extends JFrame {

    private final JTabbedPane tabbedPane;
    private final ParserCodePanel codePanel;
    private final ParserSettingsPanel settingsPanel;
    private final JButton testButton;
    private final JButton saveButton;
    private final HttpEditorPanel<HttpRequestEditor> reqEditorPanel;
    private final HttpEditorPanel<WebSocketMessageEditor> parsedEditorPanel;

    public HttpParserPanel() {
        super("HTTP Parser");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        setMinimumSize(new Dimension(1100, 800));
        setPreferredSize(new Dimension(1100, 850));
        setLayout(new BorderLayout());

        setJMenuBar(createMenuBar());

        tabbedPane = new JTabbedPane();
        codePanel = new ParserCodePanel();
        settingsPanel = new ParserSettingsPanel();

        tabbedPane.addTab("Settings", settingsPanel);
        tabbedPane.addTab("Code", codePanel);

        JPanel centerBar = new JPanel(new BorderLayout());
        centerBar.setBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, new Color(180,180,180)));

        testButton = new JButton("Test");
        testButton.setBackground(new Color(255, 137, 0));
        testButton.setForeground(Color.WHITE);
        testButton.setFocusPainted(false);
        testButton.setPreferredSize(new Dimension(75, 24));
        testButton.setFont(testButton.getFont().deriveFont(Font.BOLD));
        testButton.setFont(testButton.getFont().deriveFont(Font.BOLD, 13f));


        saveButton = new JButton("Save");
        saveButton.setForeground(Color.BLACK);
        saveButton.setFocusPainted(false);
        saveButton.setPreferredSize(new Dimension(75, 24));
        saveButton.setFont(saveButton.getFont().deriveFont(Font.BOLD));
        saveButton.setFont(saveButton.getFont().deriveFont(Font.BOLD, 13f));

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 27, 0));
        buttonsPanel.setOpaque(false);
        buttonsPanel.add(testButton);
        buttonsPanel.add(saveButton);

        centerBar.add(Box.createVerticalStrut(10), BorderLayout.NORTH);
        centerBar.add(Box.createVerticalStrut(1), BorderLayout.SOUTH);
        centerBar.add(Box.createHorizontalStrut(1), BorderLayout.EAST);
        centerBar.add(Box.createHorizontalStrut(1), BorderLayout.WEST);
        centerBar.add(buttonsPanel, BorderLayout.CENTER);



        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        topPanel.add(tabbedPane, BorderLayout.CENTER);
        topPanel.add(centerBar, BorderLayout.SOUTH);

        // HttpEditors
        reqEditorPanel = new HttpEditorPanel<>("Base Request",
                HTTPRaiderExtension.API.userInterface().createHttpRequestEditor());
        parsedEditorPanel = new HttpEditorPanel<>("Parsed Request",
                HTTPRaiderExtension.API.userInterface().createWebSocketMessageEditor(EditorOptions.READ_ONLY));

        // Editors horizontally in split pane
        JSplitPane editorsSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, reqEditorPanel, parsedEditorPanel);
        editorsSplit.setResizeWeight(0.5);
        editorsSplit.setBorder(null);

        // Split pane: top (everything except editors), bottom (editors)
        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topPanel, editorsSplit);
        mainSplit.setResizeWeight(0.65);
        mainSplit.setDividerSize(7);
        mainSplit.setBorder(null);

        add(mainSplit, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu file = new JMenu("File");
        JMenuItem save = new JMenuItem("Save");
        JMenuItem export = new JMenuItem("Export");
        JMenuItem load = new JMenuItem("Load");
        file.add(save);
        file.add(export);
        file.add(load);

        export.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.showSaveDialog(this);
        });
        load.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.showOpenDialog(this);
        });

        JMenu help = new JMenu("Help");
        JMenuItem helpSettings = new JMenuItem("How to use Settings mode");
        JMenuItem helpCode = new JMenuItem("How to use Code mode");
        help.add(helpSettings);
        help.add(helpCode);

        menuBar.add(file);
        menuBar.add(help);
        return menuBar;
    }

    public JTabbedPane getTabbedPane() {
        return tabbedPane;
    }

    public JButton getTestButton() { return testButton; }
    public JButton getSaveButton() { return saveButton; }
    public HttpEditorPanel<HttpRequestEditor> getReqEditorPanel() { return reqEditorPanel; }
    public HttpEditorPanel<WebSocketMessageEditor> getParsedEditorPanel() { return parsedEditorPanel; }
    public ParserCodePanel getCodePanel() { return codePanel; }
    public ParserSettingsPanel getSettingsPanel() { return settingsPanel; }
}
