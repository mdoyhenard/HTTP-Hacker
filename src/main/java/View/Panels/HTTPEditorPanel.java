package View.Panels;

import burp.api.montoya.ui.editor.Editor;
import burp.api.montoya.ui.editor.HttpRequestEditor;

import javax.swing.*;
import java.awt.*;

public class HTTPEditorPanel<T extends Editor> extends JPanel {

    private JLabel name;
    private T editor;

    public HTTPEditorPanel(String text, T editor){
        super(new BorderLayout());
        this.editor = editor;
        name = new JLabel(text);
        //name.setHorizontalAlignment(SwingConstants.CENTER);
        name.setFont(name.getFont().deriveFont(Font.BOLD, 13f));
        name.setBorder(BorderFactory.createEmptyBorder(8, 12, 2, 0));
        add(name, BorderLayout.NORTH);
        add(this.editor.uiComponent(), BorderLayout.CENTER);
    }

    public T getEditor(){
        return editor;
    }

    public Component getUiComponent(){
        return editor.uiComponent();
    }

    public void setName(String text){
        name.setText(text);
    }
}
