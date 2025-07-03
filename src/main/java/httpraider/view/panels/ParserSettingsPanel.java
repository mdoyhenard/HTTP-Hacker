package httpraider.view.panels;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.List;

public class ParserSettingsPanel extends JPanel {
    private final ParserSettingsSubPanel headersEndPanel;
    private final ParserSettingsSubPanel headerSplittingPanel;
    private final ParserSettingsSubPanel messageLengthPanel;
    private final ParserSettingsSubPanel chunkLineEndPanel;

    public ParserSettingsPanel() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

        Border panelBorder = new MatteBorder(1, 1, 1, 1, new Color(180,180,180));

        headersEndPanel = new ParserSettingsSubPanel(
                "Header-Body Delimiter", false, 10, false, 165, panelBorder, false);

        headerSplittingPanel = new ParserSettingsSubPanel(
                "Header Line Delimiter", false, 10, false, 165, panelBorder, false);

        messageLengthPanel = new ParserSettingsSubPanel(
                "Body Length Header", true, 28, true, 275, panelBorder, true);

        chunkLineEndPanel = new ParserSettingsSubPanel(
                "Chunk Line End", false, 10, false, 165, panelBorder, false);

        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;

        c.gridx = 0;
        c.weightx = 0.6;
        c.insets = new Insets(0, 0, 0, 7);
        add(headersEndPanel, c);

        c.gridx = 1;
        c.weightx = 0.6;
        c.insets = new Insets(0, 0, 0, 7);
        add(headerSplittingPanel, c);

        c.gridx = 2;
        c.weightx = 1.2;
        c.insets = new Insets(0, 0, 0, 7);
        add(messageLengthPanel, c);

        c.gridx = 3;
        c.weightx = 0.6;
        c.insets = new Insets(0, 0, 0, 0);
        add(chunkLineEndPanel, c);

        chunkLineEndPanel.setVisible(false);

        messageLengthPanel.setChunkPanelVisibilityHandler(() -> updateChunkPanelVisibility());

        GridBagConstraints sep = new GridBagConstraints();
        sep.gridy = 1;
        sep.gridx = 0;
        sep.gridwidth = 4;
        sep.fill = GridBagConstraints.HORIZONTAL;
        sep.weighty = 0;
        add(Box.createRigidArea(new Dimension(0, 10)), sep);
    }

    public void updateChunkPanelVisibility() {
        boolean anyChunked = false;
        List<ParserSettingsRowPanel> rows = messageLengthPanel.getRows();
        for (ParserSettingsRowPanel row : rows) {
            JCheckBox cb = row.getCheckBox();
            if (cb != null && cb.isSelected()) {
                anyChunked = true;
                break;
            }
        }
        if (anyChunked && !chunkLineEndPanel.isVisible()) {
            chunkLineEndPanel.setVisible(true);
            revalidate();
            repaint();
        } else if (!anyChunked && chunkLineEndPanel.isVisible()) {
            chunkLineEndPanel.setVisible(false);
            revalidate();
            repaint();
        }
    }

    public ParserSettingsSubPanel getHeadersEndPanel() { return headersEndPanel; }
    public ParserSettingsSubPanel getHeaderSplittingPanel() { return headerSplittingPanel; }
    public ParserSettingsSubPanel getMessageLengthPanel() { return messageLengthPanel; }
    public ParserSettingsSubPanel getChunkLineEndPanel() { return chunkLineEndPanel; }
}
