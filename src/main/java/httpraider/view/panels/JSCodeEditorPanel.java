package httpraider.view.panels;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.text.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;

public class JSCodeEditorPanel extends JPanel {

    private static final Pattern SINGLE_QUOTE        = Pattern.compile("'[^']*'");
    private static final Pattern DOUBLE_QUOTE        = Pattern.compile("\"[^\"]*\"");
    private static final Pattern TEMPLATE_LITERAL    = Pattern.compile("`[^`]*`");
    private static final Pattern SINGLE_LINE_COMMENT = Pattern.compile("//.*");
    private static final Pattern MULTI_LINE_COMMENT  = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern NUMBER_PATTERN      = Pattern.compile("\\b\\d+(?:\\.\\d+)?\\b");
    private static final Pattern WORD_PATTERN        = Pattern.compile("\\b[\\w.]+\\b");

    private static final Set<String> JS_KEYWORDS = new HashSet<>(Arrays.asList(
            "abstract","arguments","await","boolean","break","byte","case","catch",
            "char","class","const","continue","debugger","default","delete","do",
            "double","else","enum","eval","export","extends","false","final",
            "finally","float","for","function","goto","if","implements","import",
            "in","instanceof","int","interface","let","long","native","new",
            "null","package","private","protected","public","return","short",
            "static","super","switch","synchronized","this","throw","throws",
            "transient","true","try","typeof","var","void","volatile","while",
            "with","yield","async","of"
    ));

    private static final Set<String> JS_OBJECTS = new HashSet<>(Arrays.asList(
            "Array","Object","String","Number","Boolean","Date","Math","JSON",
            "console","window","document","localStorage","sessionStorage","Promise",
            "Error","TypeError","ReferenceError","SyntaxError","RegExp","Map","Set"
    ));

    private static final Set<String> JS_METHODS = new HashSet<>(Arrays.asList(
            "length",
            "push","pop","shift","unshift","slice","splice","indexOf","lastIndexOf",
            "forEach","map","filter","reduce","find","findIndex","includes","join",
            "reverse","sort","concat","every","some",
            "charAt","charCodeAt","concat","endsWith","startsWith","substring","substr",
            "toLowerCase","toUpperCase","trim","replace","split","match","search",
            "padStart","padEnd","repeat",
            "hasOwnProperty","toString","valueOf",
            "log","error","warn","info","debug","trace",
            "abs","ceil","floor","round","max","min","pow","sqrt","random",
            "sin","cos","tan","asin","acos","atan",
            "getTime","getFullYear","getMonth","getDate","getHours","getMinutes","getSeconds",
            "setFullYear","setMonth","setDate","setHours","setMinutes","setSeconds",
            "parseInt","parseFloat","isNaN","isFinite","encodeURIComponent","decodeURIComponent",
            "setTimeout","setInterval","clearTimeout","clearInterval"
    ));

    private static final Set<String> ALL_SUGGESTIONS = new HashSet<>();
    static {
        ALL_SUGGESTIONS.addAll(JS_KEYWORDS);
        ALL_SUGGESTIONS.addAll(JS_OBJECTS);
        ALL_SUGGESTIONS.addAll(JS_METHODS);
    }

    private final JTextPane textPane;
    private final JScrollPane scrollPane;
    private final StyledDocument doc;
    private final JPopupMenu suggestionPopup;
    private final JList<String> suggestionList;
    private final Timer highlightTimer;

    private final Style keywordStyle;
    private final Style stringStyle;
    private final Style commentStyle;
    private final Style numberStyle;
    private final Style objectStyle;
    private final Style defaultStyle;

    public JSCodeEditorPanel() { this(""); }
    public JSCodeEditorPanel(String initialCode) {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(800, 600));

        textPane = new JTextPane();
        doc = textPane.getStyledDocument();
        textPane.setText(initialCode);
        textPane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        textPane.setBackground(Color.WHITE);
        textPane.setForeground(Color.BLACK);
        textPane.setCaretColor(Color.BLACK);
        textPane.setSelectionColor(new Color(173, 214, 255));

        scrollPane = new JScrollPane(textPane);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        add(scrollPane, BorderLayout.CENTER);

        keywordStyle = textPane.addStyle("keyword", null);
        StyleConstants.setForeground(keywordStyle, new Color(86, 156, 214));
        StyleConstants.setBold(keywordStyle, true);

        stringStyle = textPane.addStyle("string", null);
        StyleConstants.setForeground(stringStyle, new Color(206, 145, 120));

        commentStyle = textPane.addStyle("comment", null);
        StyleConstants.setForeground(commentStyle, new Color(106, 153, 85));
        StyleConstants.setItalic(commentStyle, true);

        numberStyle = textPane.addStyle("number", null);
        StyleConstants.setForeground(numberStyle, new Color(181, 206, 168));

        objectStyle = textPane.addStyle("object", null);
        StyleConstants.setForeground(objectStyle, new Color(78, 201, 176));

        defaultStyle = textPane.addStyle("default", null);
        StyleConstants.setForeground(defaultStyle, Color.BLACK);

        installLineNumbers();

        highlightTimer = new Timer(300, e -> highlightSyntax());
        highlightTimer.setRepeats(false);

        suggestionList = new JList<>(new DefaultListModel<>());
        suggestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        suggestionList.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        suggestionList.setFocusable(false);

        JScrollPane suggestionScroll = new JScrollPane(suggestionList);
        suggestionScroll.setFocusable(false);
        suggestionScroll.setPreferredSize(new Dimension(200, 150));

        suggestionPopup = new JPopupMenu();
        suggestionPopup.add(suggestionScroll);

        installDocumentListener();
        installKeyHandlers();

        SwingUtilities.invokeLater(this::highlightSyntax);
    }

    private void installLineNumbers() {
        JTextArea lineNumbers = new JTextArea();
        lineNumbers.setFont(textPane.getFont());
        lineNumbers.setBackground(new Color(240, 240, 240));
        lineNumbers.setForeground(Color.GRAY);
        lineNumbers.setEditable(false);
        lineNumbers.setFocusable(false);
        lineNumbers.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        scrollPane.setRowHeaderView(lineNumbers);

        doc.addDocumentListener(new DocumentListener() {
            private void update() {
                SwingUtilities.invokeLater(() -> {
                    int lines = textPane.getText().split("\n", -1).length;
                    StringBuilder sb = new StringBuilder();
                    for (int i = 1; i <= lines; i++) sb.append(String.format("%3d\n", i));
                    lineNumbers.setText(sb.toString());
                });
            }
            public void insertUpdate(DocumentEvent e) { update(); highlightTimer.restart(); }
            public void removeUpdate(DocumentEvent e) { update(); highlightTimer.restart(); }
            public void changedUpdate(DocumentEvent e) {}
        });
    }

    private void installDocumentListener() {
        doc.addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { highlightTimer.restart(); }
            public void removeUpdate(DocumentEvent e) { highlightTimer.restart(); }
            public void changedUpdate(DocumentEvent e) {}
        });
    }

    private void installKeyHandlers() {
        suggestionList.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_ENTER: insertSelectedSuggestion(); break;
                    case KeyEvent.VK_ESCAPE: suggestionPopup.setVisible(false); break;
                }
            }
        });

        textPane.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (suggestionPopup.isVisible()) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_DOWN:
                            navigateSuggestion(1);
                            e.consume();
                            return;
                        case KeyEvent.VK_UP:
                            navigateSuggestion(-1);
                            e.consume();
                            return;
                        case KeyEvent.VK_ENTER:
                            insertSelectedSuggestion();
                            e.consume();
                            return;
                        case KeyEvent.VK_ESCAPE:
                            suggestionPopup.setVisible(false);
                            return;
                    }
                }
                if (e.getKeyCode() == KeyEvent.VK_SPACE && e.isControlDown()) {
                    showAutoComplete();
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (!suggestionPopup.isVisible()) handleAutoIndent();
                }
            }

            @Override public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                if (Character.isLetterOrDigit(c) || c == '.') {
                    showAutoCompleteIfNeeded();
                } else {
                    suggestionPopup.setVisible(false);
                }
            }
        });
    }

    private void navigateSuggestion(int delta) {
        int idx = suggestionList.getSelectedIndex();
        int size = suggestionList.getModel().getSize();
        int next = Math.max(0, Math.min(size - 1, idx + delta));
        suggestionList.setSelectedIndex(next);
        suggestionList.ensureIndexIsVisible(next);
    }

    private void highlightSyntax() {
        String text = textPane.getText();
        new SwingWorker<List<StyleRange>, Void>() {
            @Override protected List<StyleRange> doInBackground() {
                List<StyleRange> ranges = new ArrayList<>();
                ranges.add(new StyleRange(0, text.length(), defaultStyle));
                Matcher m;
                m = SINGLE_QUOTE.matcher(text);          while (m.find()) ranges.add(new StyleRange(m.start(), m.end()-m.start(), stringStyle));
                m = DOUBLE_QUOTE.matcher(text);          while (m.find()) ranges.add(new StyleRange(m.start(), m.end()-m.start(), stringStyle));
                m = TEMPLATE_LITERAL.matcher(text);      while (m.find()) ranges.add(new StyleRange(m.start(), m.end()-m.start(), stringStyle));
                m = SINGLE_LINE_COMMENT.matcher(text);   while (m.find()) ranges.add(new StyleRange(m.start(), m.end()-m.start(), commentStyle));
                m = MULTI_LINE_COMMENT.matcher(text);    while (m.find()) ranges.add(new StyleRange(m.start(), m.end()-m.start(), commentStyle));
                m = NUMBER_PATTERN.matcher(text);        while (m.find()) ranges.add(new StyleRange(m.start(), m.end()-m.start(), numberStyle));
                m = WORD_PATTERN.matcher(text);          while (m.find()) {
                    String w = m.group();
                    if (JS_KEYWORDS.contains(w)) ranges.add(new StyleRange(m.start(), m.end()-m.start(), keywordStyle));
                    else if (JS_OBJECTS.contains(w)) ranges.add(new StyleRange(m.start(), m.end()-m.start(), objectStyle));
                }
                return ranges;
            }
            @Override protected void done() {
                try {
                    List<StyleRange> ranges = get();
                    doc.setCharacterAttributes(0, text.length(), defaultStyle, true);
                    for (StyleRange r : ranges) {
                        doc.setCharacterAttributes(r.offset, r.length, r.style, false);
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private static class StyleRange {
        final int offset, length;
        final AttributeSet style;
        StyleRange(int o, int l, AttributeSet s) { offset = o; length = l; style = s; }
    }

    private String getCurrentWord() {
        try {
            int pos = textPane.getCaretPosition();
            String t = textPane.getText();
            int start = pos;
            while (start > 0 && (Character.isLetterOrDigit(t.charAt(start-1)) || t.charAt(start-1) == '.')) {
                start--;
            }
            return t.substring(start, pos);
        } catch (Exception e) {
            return "";
        }
    }

    private List<String> getSuggestions(String prefix) {
        String prop = prefix.contains(".") ? prefix.substring(prefix.lastIndexOf('.') + 1) : prefix;
        List<String> list = new ArrayList<>();
        String low = prop.toLowerCase();
        for (String cand : ALL_SUGGESTIONS) {
            if (cand.toLowerCase().startsWith(low)) {
                list.add(cand);
            }
        }
        Collections.sort(list);
        return list.size() > 10 ? list.subList(0, 10) : list;
    }

    private void showAutoComplete() {
        List<String> matches = getSuggestions(getCurrentWord());
        if (matches.isEmpty()) return;
        suggestionList.setListData(matches.toArray(new String[0]));
        suggestionList.setSelectedIndex(0);
        try {
            Rectangle r = textPane.modelToView2D(textPane.getCaretPosition()).getBounds();
            suggestionPopup.show(textPane, r.x, r.y + r.height);
            textPane.requestFocusInWindow();
        } catch (BadLocationException ignored) {}
    }

    private void showAutoCompleteIfNeeded() {
        String w = getCurrentWord();
        if (w.length() >= 1) showAutoComplete();
        else suggestionPopup.setVisible(false);
    }

    private void insertSelectedSuggestion() {
        String sel = suggestionList.getSelectedValue();
        if (sel == null) return;
        try {
            int pos = textPane.getCaretPosition();
            String t = textPane.getText();
            int start = pos;
            while (start > 0 && (Character.isLetterOrDigit(t.charAt(start-1)) || t.charAt(start-1) == '.')) {
                start--;
            }
            String full = t.substring(start, pos);
            int replacePos = start;
            int replaceLen = full.length();
            if (full.contains(".")) {
                int idx = full.lastIndexOf('.');
                replacePos = start + idx + 1;
                replaceLen = full.length() - idx - 1;
            }
            doc.remove(replacePos, replaceLen);
            doc.insertString(replacePos, sel, null);
            suggestionPopup.setVisible(false);
            textPane.requestFocusInWindow();
        } catch (BadLocationException ignored) {}
    }

    private void handleAutoIndent() {
        SwingUtilities.invokeLater(() -> {
            try {
                int pos = textPane.getCaretPosition();
                String t = textPane.getText();
                int ls = pos - 1;
                while (ls > 0 && t.charAt(ls - 1) != '\n') ls--;
                String line = t.substring(ls, pos - 1);
                int indent = 0;
                for (char c : line.toCharArray()) {
                    if (c == ' ') indent++;
                    else if (c == '\t') indent += 4;
                    else break;
                }
                if (line.trim().endsWith("{")) indent += 4;
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < indent; i++) sb.append(' ');
                doc.insertString(pos, sb.toString(), null);
            } catch (BadLocationException ignored) {}
        });
    }

    public String getCode() { return textPane.getText(); }
    public void setCode(String code) { textPane.setText(code); SwingUtilities.invokeLater(this::highlightSyntax); }
    public JTextPane getTextPane() { return textPane; }
    public void requestEditorFocus() { SwingUtilities.invokeLater(() -> textPane.requestFocusInWindow()); }
}
