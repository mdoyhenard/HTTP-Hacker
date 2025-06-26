package httpraider.view.panels;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

public class ParserCodePanel extends JPanel {
    public ParserCodePanel() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

        JPanel[] panels = new JPanel[3];
        String[] titles = {"Headers-End", "Header-Splitting", "Message-Length"};
        for (int i = 0; i < 3; i++) {
            panels[i] = new JPanel(new BorderLayout());
            panels[i].setBorder(BorderFactory.createTitledBorder(
                    new MatteBorder(0, i==0?0:1, 0, 0, new Color(180,180,180)), titles[i], TitledBorder.CENTER, TitledBorder.TOP,
                    panels[i].getFont().deriveFont(Font.BOLD, 15f)));
            panels[i].setPreferredSize(new Dimension(340, 240));
            panels[i].setMinimumSize(new Dimension(250, 170));
            JSCodeEditorPanel editor = new JSCodeEditorPanel();
            editor.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            panels[i].add(editor, BorderLayout.CENTER);
        }

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridy = 0;
        c.insets = new Insets(0, 0, 0, 0);
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.weightx = 1;
        add(panels[0], c);
        c.gridx++;
        add(panels[1], c);
        c.gridx++;
        add(panels[2], c);
    }
}
