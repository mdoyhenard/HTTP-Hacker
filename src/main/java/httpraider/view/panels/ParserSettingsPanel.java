package httpraider.view.panels;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

public class ParserSettingsPanel extends JPanel {
    private final ParserSettingsSubPanel headersEndPanel;
    private final ParserSettingsSubPanel headerSplittingPanel;
    private final ParserSettingsSubPanel messageLengthPanel;

    public ParserSettingsPanel() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

        Border panelBorder = new MatteBorder(1, 1, 1, 1, new Color(180,180,180));

        headersEndPanel = new ParserSettingsSubPanel(
                "Headers-End Sequence", false, 16, false, 220, panelBorder);
        headerSplittingPanel = new ParserSettingsSubPanel(
                "Header-Splitting Sequence", false, 16, false, 220, panelBorder);
        messageLengthPanel = new ParserSettingsSubPanel(
                "Message-Length Pattern", true, 30, true, 320, panelBorder);

        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;

        // Panel 1: leftmost, 10px right gap
        c.gridx = 0;
        c.weightx = 0.8;
        c.insets = new Insets(0, 0, 0, 10);
        add(headersEndPanel, c);

        // Panel 2: 10px right gap
        c.gridx = 1;
        c.weightx = 0.8;
        c.insets = new Insets(0, 0, 0, 10);
        add(headerSplittingPanel, c);

        // Panel 3: rightmost, no gap after
        c.gridx = 2;
        c.weightx = 1.2;
        c.insets = new Insets(0, 0, 0, 0);
        add(messageLengthPanel, c);

        // Add 10px vertical space before test/save bar
        GridBagConstraints sep = new GridBagConstraints();
        sep.gridy = 1;
        sep.gridx = 0;
        sep.gridwidth = 3;
        sep.fill = GridBagConstraints.HORIZONTAL;
        sep.weighty = 0;
        add(Box.createRigidArea(new Dimension(0, 10)), sep);
    }

    public ParserSettingsSubPanel getHeadersEndPanel() { return headersEndPanel; }
    public ParserSettingsSubPanel getHeaderSplittingPanel() { return headerSplittingPanel; }
    public ParserSettingsSubPanel getMessageLengthPanel() { return messageLengthPanel; }


}
