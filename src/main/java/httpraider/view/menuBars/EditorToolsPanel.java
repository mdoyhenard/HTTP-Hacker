package httpraider.view.menuBars;

import httpraider.view.filters.DigitDocumentFilter;
import httpraider.view.filters.HexDocumentFilter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import java.awt.*;
import java.awt.event.ActionListener;

public class EditorToolsPanel extends JPanel {

    /* ── ui components ────────────────────────────────────────────────────── */
    private final JLabel     selectedLabel   = new JLabel("Selected: 0 (0x0)",  SwingConstants.CENTER);

    private final JTextField hexField        = new JTextField(12);
    private final JTextField asciiField      = new JTextField(8);
    private final JTextField repeatField     = new JTextField(4);
    private final JButton    insertBtn       = new JButton("Insert");
    private final JButton    insertHeaderBtn = new JButton("Insert");
    private final JCheckBox  enableTags      = new JCheckBox("Enable Tags");
    private final JButton    helpBtn         = new JButton("?");

    private final JComboBox<String> headerCombo = new JComboBox<>(REQUEST_HEADERS);

    private static final String[] REQUEST_HEADERS = {
            "GET",
            "Host",
            "X-Forwarded-Host",
            "X-Forwarded-For",
            "Content-Length",
            "Content-Type - Form",
            "Content-Type - Json",
            "Content-Type - Multi",
            "User-Agent",
            "Connection",
            "Range",
            "Expect",
            "Max-Forwards",
            "Upgrade",
            "TE",
            "Cookie",
            "Accept",
            "Accept-Encoding",
            "Authorization",
            "Cache-Control",
            "Origin",
            "Referer"
    };

    private static final String TAGS_INFO_TEXT =
            """
<!DOCTYPE html>
<html>
<head>
<meta charset='utf-8'>
<style>
/* ── Base ─────────────────────────────────────────────────────────── */
body{
  margin:0;
  font:15px/1.55 -apple-system,BlinkMacSystemFont,"Segoe UI","Roboto",sans-serif;
  color:#212529;
}
.container{
  max-width:760px;
  padding:28px 34px;
  margin:0 auto;
}
/* ── Headings ─────────────────────────────────────────────────────── */
h1{
  text-align:center;
  font-size:23px;
  margin:0 0 18px;
  font-weight:600;
}
h2{
  font-size:18px;
  margin:26px 0 12px;
  font-weight:600;
}
/* ── Subtitle ─────────────────────────────────────────────────────── */
.subtitle{
  margin:0 0 26px;
  font-size:15px;
  color:#555;
}
/* ── Table ────────────────────────────────────────────────────────── */
table{
  width:100%;
  border-collapse:collapse;
  font-size:14px;
  box-shadow:0 0 0 1px #dcdfe3;
}
thead th{
  text-align:left;
  background:#f3f4f6;
  padding:10px 12px;
  font-weight:600;
  border-bottom:1px solid #dcdfe3;
}
tbody tr:nth-child(odd){ background:#ffffff; }
tbody tr:nth-child(even){ background:#f9f9fa; }
td{
  padding:10px 12px;
  border-bottom:1px solid #e8eaed;
}
code{
  font:13px "JetBrains Mono","Consolas",monospace;
  background:#f5f6f8;
  padding:2px 6px;
  border-radius:4px;
}
/* ── Example blocks ──────────────────────────────────────────────── */
pre{
  background:#f5f6f8;
  border:1px solid #dcdfe3;
  border-radius:6px;
  padding:12px 16px;
  font:13px/1.45 "JetBrains Mono","Consolas",monospace;
  overflow-x:auto;
  margin:6px 0 22px;
  color:#212529;
}
.label{ font-weight:600; margin:10px 0 4px; }
/* ── List ─────────────────────────────────────────────────────────── */
ul{ margin:0 0 0 22px; padding:0; font-size:14px; }
li{ margin:5px 0; }
</style>
</head>
<body>
  <div class="container">

    <h1>Dynamic&nbsp;Tags&nbsp;Help</h1>

    <p class="subtitle">
      Insert the tags below directly in your document. When you <b>send</b> the request,
      the editor replaces each tag with the correct value&mdash;
      byte counts or repeated patterns&mdash;calculated on&nbsp;the&nbsp;fly.
    </p>

    <table>
      <thead>
        <tr><th style="width:38%">Tag</th><th>Description</th></tr>
      </thead>
      <tbody>
        <tr>
          <td><code>&lt;start_<i>ID</i>&gt; … &lt;end_<i>ID</i>&gt;</code></td>
          <td>Marks a block; its <b>byte length</b> is measured.</td>
        </tr>
        <tr>
          <td><code>&lt;int_<i>ID</i>&gt;</code></td>
          <td>Decimal length of that block.</td>
        </tr>
        <tr>
          <td><code>&lt;hex_<i>ID</i>&gt;</code></td>
          <td>The same length in hexadecimal.</td>
        </tr>
        <tr>
          <td><code>&lt;repeat(<i>ID</i>, "txt")&gt;</code></td>
          <td>Repeats <code>txt</code> exactly <i>ID-length</i> times
              (must be <i>outside</i> its own block).</td>
        </tr>
      </tbody>
    </table>

    <h2>Nested Example</h2>

    <div class="label">Input</div>
    <pre>&lt;repeat(1,"*")&gt;
&lt;start_1&gt;Hi &lt;start_2&gt;abc&lt;end_2&gt; &lt;int_2&gt;&lt;end_1&gt; &lt;int_1&gt;</pre>

    <div class="label">Steps</div>
    <pre>block 2 = 3 bytes  → &lt;int_2&gt; = 3
block 1 = 13 bytes → &lt;int_1&gt; = 13
repeat   = 13 × "*"</pre>

    <div class="label">Result</div>
    <pre>************* Hi abc3 13</pre>

    <h2>Quick Rules</h2>
    <ul>
      <li>IDs are numeric and <b>unique</b> (same ID feeds <code>int</code> &amp; <code>hex</code>).</li>
      <li>Evaluation runs from <b>innermost → outermost</b>.</li>
      <li>Spaces and line breaks inside a block are counted.</li>
    </ul>

  </div>
</body>
</html>
""";

    private boolean internalUpdate = false;

    public EditorToolsPanel() {
        super(new GridBagLayout());
        setBorder(new EmptyBorder(8, 10, 8, 10));

        ((AbstractDocument) hexField.getDocument()).setDocumentFilter(new HexDocumentFilter());
        ((AbstractDocument) repeatField.getDocument()).setDocumentFilter(new DigitDocumentFilter());

        hexField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { syncFromHex(); }
            @Override public void removeUpdate(DocumentEvent e) { syncFromHex(); }
            @Override public void changedUpdate(DocumentEvent e) { }
        });

        asciiField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { syncFromAscii(); }
            @Override public void removeUpdate(DocumentEvent e) { syncFromAscii(); }
            @Override public void changedUpdate(DocumentEvent e) { }
        });
        repeatField.setText("1");

        headerCombo.setPreferredSize(asciiField.getPreferredSize());
        headerCombo.setEditable(false);
        headerCombo.setSelectedIndex(0);

        enableTags.setHorizontalAlignment(SwingConstants.CENTER);
        enableTags.setFont(new Font("Arial", Font.BOLD, 14));
        selectedLabel.setFont(new Font("Arial", Font.PLAIN, 14));

        helpBtn.setPreferredSize(new Dimension(18,18));
        helpBtn.setMargin(new Insets(0,0,0,0));

        JEditorPane view = new JEditorPane("text/html", TAGS_INFO_TEXT);
        view.setEditable(false);
        view.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);

        JScrollPane scroller = new JScrollPane(view, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.setBorder(new EmptyBorder(0,0,0,0));
        scroller.setPreferredSize(new Dimension(640, 520));
        scroller.getVerticalScrollBar().setUnitIncrement(16);

        JDialog dlg = new JDialog(
                SwingUtilities.getWindowAncestor(this),
                "Dynamic Placeholders – Help",
                Dialog.ModalityType.APPLICATION_MODAL
        );
        dlg.getContentPane().add(scroller);
        dlg.pack();
        dlg.setLocationRelativeTo(this);

        helpBtn.addActionListener(e -> dlg.setVisible(true));
        buildLayout();
    }

    public int getSelectedHeader(){
        return headerCombo.getSelectedIndex();
    }

    public void setEnableTagsListener(ActionListener l){
        enableTags.addActionListener(l);
    }

    public boolean tagsEnabled(){
        return enableTags.isSelected();
    }

    public String getAsciiText() {
        return asciiField.getText();
    }

    public void setInsertStringActionListener(ActionListener l){
        insertBtn.addActionListener(l);
    }

    public void setInsertHeaderActionListener(ActionListener l){
        insertHeaderBtn.addActionListener(l);
    }

    public void setSelectedBytes(int count) {
        SwingUtilities.invokeLater(() ->
                selectedLabel.setText("Selected: " + count + " (0x"
                        + Integer.toHexString(count) + ')'));
    }

    public Integer getHexValue() {
        String txt = hexField.getText().trim().replace(" ", "");
        return txt.isEmpty() ? null : Integer.parseUnsignedInt(txt, 16);
    }

    public int getRepeatCount() {
        String txt = repeatField.getText().trim();
        return txt.isEmpty() ? 1 : Integer.parseInt(txt);
    }

    /* ── helpers ─────────────────────────────────────────────────────────── */

    private void syncFromAscii() {
        if (internalUpdate) return;
        internalUpdate = true;
        String txt = asciiField.getText();
        StringBuilder hex = new StringBuilder();
        for (char c : txt.toCharArray())
            hex.append(String.format("%02X", (int) c));
        hexField.setText(insertSpaces(hex.toString()));
        internalUpdate = false;
    }

    private void syncFromHex() {
        if (internalUpdate) return;
        String raw = hexField.getText().replaceAll("\\s+", "");
        String spaced = insertSpaces(raw);

        if (!spaced.equals(hexField.getText())) {
            SwingUtilities.invokeLater(() -> {
                internalUpdate = true;
                hexField.setText(spaced);
                internalUpdate = false;
            });
        }

        if (raw.length() % 2 != 0) return;

        StringBuilder ascii = new StringBuilder();
        try {
            for (int i = 0; i < raw.length(); i += 2)
                ascii.append((char) Integer.parseInt(raw.substring(i, i + 2), 16));

            internalUpdate = true;
            asciiField.setText(ascii.toString());
            internalUpdate = false;
        } catch (NumberFormatException ignored) { }
    }

    private static String insertSpaces(String s) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (i > 0 && i % 2 == 0) out.append(' ');
            out.append(Character.toUpperCase(s.charAt(i)));
        }
        return out.toString();
    }

    private void buildLayout() {
        GridBagConstraints c = new GridBagConstraints();
        c.insets  = new Insets(4, 2, 4, 2);
        c.anchor  = GridBagConstraints.CENTER;
        c.fill    = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        int row = 0;

        c.gridy = row++; c.gridwidth = 2; c.fill = GridBagConstraints.NONE;
        add(Box.createVerticalStrut(3), c); c.gridwidth = 1; c.fill = GridBagConstraints.HORIZONTAL;

        c.gridy = row++; c.gridwidth = 2;
        add(selectedLabel, c);

        c.gridy = row++; c.gridwidth = 2; c.fill = GridBagConstraints.NONE;
        add(Box.createVerticalStrut(2), c); c.gridwidth = 1; c.fill = GridBagConstraints.HORIZONTAL;

        c.gridy = row++; c.gridwidth = 2; c.insets = new Insets(8, 0, 8, 0);
        add(new JSeparator(), c); c.insets = new Insets(4, 2, 4, 2);

        c.gridy = row++; c.weightx = 0;
        JLabel insertCharLabel = new JLabel("Insert Character", SwingConstants.CENTER);
        insertCharLabel.setFont(insertCharLabel.getFont().deriveFont(Font.BOLD));
        add(insertCharLabel, c);

        c.gridy = row++; c.gridwidth = 2; c.fill = GridBagConstraints.NONE;
        add(Box.createVerticalStrut(4), c); c.gridwidth = 1; c.fill = GridBagConstraints.HORIZONTAL;

        c.gridy = row++; add(new JLabel("ASCII:"), c); c.gridx = 1; c.weightx = 1; add(asciiField, c); c.gridx = 0;

        c.gridy = row++; add(new JLabel("Hex:"), c); c.gridx = 1; add(hexField, c); c.gridx = 0;

        c.gridy = row++; add(new JLabel("Repeat:"), c); c.gridx = 1; add(repeatField, c); c.gridx = 0;

        c.gridy = row++; c.gridwidth = 2;
        add(insertBtn, c);

        c.gridy = row++; c.gridwidth = 2; c.fill = GridBagConstraints.NONE;
        add(Box.createVerticalStrut(2), c); c.gridwidth = 1; c.fill = GridBagConstraints.HORIZONTAL;

        c.gridy = row++; c.gridwidth = 2; c.insets = new Insets(8, 0, 8, 0);
        add(new JSeparator(), c); c.insets = new Insets(4, 2, 4, 2);

        c.gridy = row++; c.weightx = 0;
        JLabel headerLbl = new JLabel("Insert Header line", SwingConstants.CENTER);
        headerLbl.setFont(headerLbl.getFont().deriveFont(Font.BOLD));
        add(headerLbl, c);

        c.gridy = row++; c.gridwidth = 2; c.fill = GridBagConstraints.NONE;
        add(Box.createVerticalStrut(4), c);

        JPanel headerLine = new JPanel(new BorderLayout(4, 0));
        headerLine.add(headerCombo, BorderLayout.CENTER);
        headerLine.add(insertHeaderBtn, BorderLayout.EAST);

        c.gridy = row++; c.gridwidth = 2; c.fill = GridBagConstraints.HORIZONTAL;
        add(headerLine, c);

        c.gridy = row++; c.gridwidth = 2; c.fill = GridBagConstraints.NONE;
        add(Box.createVerticalStrut(5), c); c.gridwidth = 1; c.fill = GridBagConstraints.HORIZONTAL;

        c.gridy = row++; c.gridwidth = 2; c.insets = new Insets(8, 0, 8, 0);
        add(new JSeparator(), c); c.insets = new Insets(4, 2, 4, 2);

        c.gridy = row++; c.gridwidth = 2; c.fill = GridBagConstraints.NONE;
        add(Box.createVerticalStrut(5), c); c.gridwidth = 1; c.fill = GridBagConstraints.HORIZONTAL;

        JPanel tagLine = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        tagLine.add(enableTags); tagLine.add(helpBtn);

        c.gridy = row; c.gridwidth = 0;
        add(tagLine, c);
    }
}
