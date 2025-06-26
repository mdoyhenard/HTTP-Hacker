package httpraider.view.panels;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class ParserCodePanel extends JPanel {
    private final JSCodeEditorPanel editor1;
    private final JSCodeEditorPanel editor2;
    private final JSCodeEditorPanel editor3;

    public ParserCodePanel() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

        JPanel[] panels = new JPanel[3];
        String[] titles = {"Headers-End", "Header-Splitting", "Message-Length"};
        JSCodeEditorPanel[] editors = new JSCodeEditorPanel[3];

        for (int i = 0; i < 3; i++) {
            panels[i] = new JPanel(new BorderLayout());
            panels[i].setBorder(BorderFactory.createTitledBorder(
                    new MatteBorder(0, i==0?0:1, 0, 0, new Color(180,180,180)), titles[i], TitledBorder.CENTER, TitledBorder.TOP,
                    panels[i].getFont().deriveFont(Font.BOLD, 15f)));
            panels[i].setPreferredSize(new Dimension(340, 240));
            panels[i].setMinimumSize(new Dimension(250, 170));
            editors[i] = new JSCodeEditorPanel();
            editors[i].setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            panels[i].add(editors[i], BorderLayout.CENTER);
        }

        editor1 = editors[0];
        editor2 = editors[1];
        editor3 = editors[2];

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

    public String getCode1() { return editor1.getCode(); }
    public String getCode2() { return editor2.getCode(); }
    public String getCode3() { return editor3.getCode(); }

    public void setCode1(String code) { editor1.setCode(code != null ? code : ""); }
    public void setCode2(String code) { editor2.setCode(code != null ? code : ""); }
    public void setCode3(String code) { editor3.setCode(code != null ? code : ""); }
}
