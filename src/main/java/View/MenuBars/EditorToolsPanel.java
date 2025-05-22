package View.MenuBars;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * The “Editor Tools” palette that lives inside {@link view.menubars.InspectorBar}.
 *
 * <pre>
 * ┌───────────────────────────────────────┐
 * │ Selected: 10 (0xa)                   │
 * │┌────────────┐                        │
 * ││Generate CL │                        │
 * │└────────────┘                        │
 * │┌────────────┐                        │
 * ││Generate TE │                        │
 * │└────────────┘                        │
 * │──────────────────────────────────────│
 * │ Hex:    [______]                     │
 * │ Repeat: [__]                         │
 * │┌────────┐                            │
 * ││ Insert │                            │
 * │└────────┘                            │
 * └───────────────────────────────────────┘
 * </pre>
 *
 *  🔸  Controller code *never* reaches into Swing fields directly –
 *      it talks through the public API below.
 */
public class EditorToolsPanel extends JPanel {

    /* ------------------------------------------------------------------
     *  UI widgets
     * ---------------------------------------------------------------- */
    private final JLabel     selectedLabel   = new JLabel("Selected: 0 (0x0)");
    private final JButton    generateClBtn   = new JButton("Generate CL");
    private final JButton    generateTeBtn   = new JButton("Generate TE");
    private final JTextField hexField        = new JTextField(8);        // up to 32-bit
    private final JTextField repeatField     = new JTextField(4);
    private final JButton    insertBtn       = new JButton("Insert");

    /* ------------------------------------------------------------------
     *  ctor
     * ---------------------------------------------------------------- */
    public EditorToolsPanel() {
        super(new GridBagLayout());
        setBorder(new EmptyBorder(8, 10, 8, 10));   // breathing room

        // keep the hex field hexadecimal
        ((AbstractDocument) hexField.getDocument()).setDocumentFilter(new HexDocumentFilter());

        // simple numeric doc-filter for repeat
        ((AbstractDocument) repeatField.getDocument()).setDocumentFilter(new DigitDocumentFilter());

        buildLayout();
    }

    /* ------------------------------------------------------------------
     *  Public API (view setters / getters / listener hooks)
     * ---------------------------------------------------------------- */

    /** Updates the "Selected: n (0x...)" label. */
    public void setSelectedBytes(int count) {
        SwingUtilities.invokeLater(() ->
                selectedLabel.setText("Selected: " + count + " (0x"
                        + Integer.toHexString(count) + ')'));
    }

    /** Returns the current hex value, or <code>null</code> if the field is blank. */
    public Integer getHexValue() {
        String txt = hexField.getText().trim();
        return txt.isEmpty() ? null : Integer.parseUnsignedInt(txt, 16);
    }

    /** Returns the repeat count (defaults to 1 if empty). */
    public int getRepeatCount() {
        String txt = repeatField.getText().trim();
        return txt.isEmpty() ? 1 : Integer.parseInt(txt);
    }

    // ----- listener registration -------------------------------------------------

    public void addGenerateClListener(ActionListener l) { generateClBtn.addActionListener(l); }
    public void addGenerateTeListener(ActionListener l) { generateTeBtn.addActionListener(l); }
    public void addInsertListener     (ActionListener l) { insertBtn    .addActionListener(l); }

    /* ------------------------------------------------------------------
     *  private helpers
     * ---------------------------------------------------------------- */

    private void buildLayout() {
        GridBagConstraints c = new GridBagConstraints();
        c.insets    = new Insets(4, 2, 4, 2);
        c.anchor    = GridBagConstraints.CENTER;
        c.fill      = GridBagConstraints.HORIZONTAL;
        c.weightx   = 1;

        int row = 0;

        // row 0: selected label
        c.gridy = row++; c.gridwidth = 2;
        add(selectedLabel, c);

        // row 1: Generate CL
        c.gridy = row++; c.gridwidth = 2;
        add(generateClBtn, c);

        // row 2: Generate TE
        c.gridy = row++; c.gridwidth = 2;
        add(generateTeBtn, c);

        // separator
        c.gridy = row++; c.gridwidth = 2;
        c.insets = new Insets(8, 0, 8, 0);
        add(new JSeparator(), c);
        c.insets = new Insets(4, 2, 4, 2);

        // row 4: Hex label + field
        c.gridy = row++; c.gridwidth = 1;
        c.weightx = 0;
        add(new JLabel("Hex:"), c);
        c.gridx = 1; c.weightx = 1;
        add(hexField, c);
        c.gridx = 0;

        // row 5: Repeat label + field
        c.gridy = row++; c.weightx = 0;
        add(new JLabel("Repeat:"), c);
        c.gridx = 1; c.weightx = 1;
        add(repeatField, c);
        c.gridx = 0;

        // row 6: Insert button
        c.gridy   = row; c.gridwidth = 2;
        c.weightx = 1;
        add(insertBtn, c);
    }

    /* ------------------------------------------------------------------
     *  tiny document filters
     * ---------------------------------------------------------------- */

    /** Accepts 0-9 & A-F only (case-insensitive). */
    private static class HexDocumentFilter extends DocumentFilter {
        @Override
        public void insertString(FilterBypass fb, int offs, String str, AttributeSet a)
                throws BadLocationException {
            if (str != null && str.matches("[0-9a-fA-F]+"))
                super.insertString(fb, offs, str.toUpperCase(), a);
        }
        @Override
        public void replace(FilterBypass fb, int offs, int len, String str, AttributeSet a)
                throws BadLocationException {
            if (str != null && str.matches("[0-9a-fA-F]+"))
                super.replace(fb, offs, len, str.toUpperCase(), a);
        }
    }

    /** Accepts decimal digits only. */
    private static class DigitDocumentFilter extends DocumentFilter {
        @Override
        public void insertString(FilterBypass fb, int offs, String str, AttributeSet a)
                throws BadLocationException {
            if (str != null && str.matches("\\d+"))
                super.insertString(fb, offs, str, a);
        }
        @Override
        public void replace(FilterBypass fb, int offs, int len, String str, AttributeSet a)
                throws BadLocationException {
            if (str != null && str.matches("\\d+"))
                super.replace(fb, offs, len, str, a);
        }
    }
}
