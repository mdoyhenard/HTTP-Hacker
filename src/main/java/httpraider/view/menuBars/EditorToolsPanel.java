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
    private final JLabel     selectedLabel   = new JLabel("Selected: 0 (0x0)");
    private final JButton    generateClBtn   = new JButton("Generate CL");
    private final JButton    generateTeBtn   = new JButton("Generate TE");
    private final JTextField hexField        = new JTextField(12);
    private final JTextField asciiField      = new JTextField(8);
    private final JTextField repeatField     = new JTextField(4);
    private final JButton    insertBtn       = new JButton("Insert");

    private boolean internalUpdate = false;

    public EditorToolsPanel() {
        super(new GridBagLayout());
        setBorder(new EmptyBorder(8, 10, 8, 10));

        ((AbstractDocument) hexField.getDocument()).setDocumentFilter(new HexDocumentFilter());
        ((AbstractDocument) repeatField.getDocument()).setDocumentFilter(new DigitDocumentFilter());


        /* keep internal conversions */
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

        buildLayout();
    }

    public String getAsciiText() {
        return asciiField.getText();
    }

    public void setInsertStringActionListener(ActionListener l){
        insertBtn.addActionListener(l);
    }

    public void setGenerateCLActionListener(ActionListener l){
        generateClBtn.addActionListener(l);
    }

    public void setGenerateTEActionListener(ActionListener l){
        generateTeBtn.addActionListener(l);
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
        String spaced = insertSpaces(raw);                 // always re-space

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

        c.gridy = row++; c.gridwidth = 2;
        add(selectedLabel, c);

        c.gridy = row++; c.gridwidth = 2;
        add(generateClBtn, c);

        c.gridy = row++; c.gridwidth = 2;
        add(generateTeBtn, c);

        c.gridy = row++; c.gridwidth = 2;
        c.insets = new Insets(8, 0, 8, 0);
        add(new JSeparator(), c);
        c.insets = new Insets(4, 2, 4, 2);

        c.gridy = row++; c.gridwidth = 1; c.weightx = 0;
        add(new JLabel("ASCII:"), c);
        c.gridx = 1; c.weightx = 1;
        add(asciiField, c);
        c.gridx = 0;

        c.gridy = row++; c.weightx = 0;
        add(new JLabel("Hex:"), c);
        c.gridx = 1; c.weightx = 1;
        add(hexField, c);
        c.gridx = 0;

        c.gridy = row++; c.weightx = 0;
        add(new JLabel("Repeat:"), c);
        c.gridx = 1; c.weightx = 1;
        add(repeatField, c);
        c.gridx = 0;

        c.gridy   = row; c.gridwidth = 2;
        add(insertBtn, c);
    }
}
