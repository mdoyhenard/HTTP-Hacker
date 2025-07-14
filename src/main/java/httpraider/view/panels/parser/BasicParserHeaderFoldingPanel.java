package httpraider.view.panels.parser;

import javax.swing.*;
import java.awt.*;

public class BasicParserHeaderFoldingPanel extends JPanel {
    private final JCheckBox foldingCheckbox;

    public BasicParserHeaderFoldingPanel() {
        super();
        setLayout(new FlowLayout(FlowLayout.LEFT));
        setBorder(BorderFactory.createTitledBorder("Header Folding"));

        foldingCheckbox = new JCheckBox("Fold headers");
        foldingCheckbox.setToolTipText("Allows parsing of multi-line headers (lines starting with whitespace are joined to previous header). Rare in modern HTTP.");

        JLabel helpLabel = new JLabel("RFC 7230 §3.2.4");
        helpLabel.setFont(helpLabel.getFont().deriveFont(Font.ITALIC, 11f));
        helpLabel.setForeground(new Color(80, 80, 120));

        add(foldingCheckbox);
        //add(Box.createHorizontalStrut(8));
        //add(helpLabel);
    }

    public boolean isHeaderFoldingEnabled() {
        return foldingCheckbox.isSelected();
    }

    public void setHeaderFoldingEnabled(boolean enabled) {
        foldingCheckbox.setSelected(enabled);
    }
}
