package httpraider.view.panels;

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.ui.editor.Editor;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import burp.api.montoya.ui.editor.WebSocketMessageEditor;

import javax.swing.*;
import java.awt.*;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;

import static javax.swing.SwingUtilities.invokeLater;

public class HTTPEditorPanel<T extends Editor> extends JPanel {

    private final JLabel name;
    private final T editor;

    public HTTPEditorPanel(String text, T editor){
        super(new BorderLayout());
        this.editor = editor;
        name = new JLabel(text);
        name.setFont(name.getFont().deriveFont(Font.BOLD, 13f));
        name.setBorder(BorderFactory.createEmptyBorder(8, 12, 2, 0));
        add(name, BorderLayout.NORTH);
        add(this.editor.uiComponent(), BorderLayout.CENTER);
    }

    private T getEditor(){ return editor; }

    private Component getUiComponent(){ return editor.uiComponent(); }

    public void setName(String text){ name.setText(text); }

    public byte[] getBytes(){
        return (editor instanceof HttpRequestEditor) ? ((HttpRequestEditor)editor).getRequest().toByteArray().getBytes() : ((WebSocketMessageEditor)editor).getContents().getBytes();
    }

    public void setBytes(byte[] data){
        invokeLater(() -> {
            if (editor instanceof HttpRequestEditor)
                ((HttpRequestEditor)editor).setRequest(HttpRequest.httpRequest(((HttpRequestEditor) editor).getRequest().httpService(), ByteArray.byteArray(data)));
            else
                ((WebSocketMessageEditor)editor).setContents(ByteArray.byteArray(data));
        });
    }

    public void addBytes(byte[] data){
        byte[] concat;
        if (editor instanceof HttpRequestEditor)
            concat = ByteBuffer.allocate(((HttpRequestEditor)editor).getRequest().toByteArray().getBytes().length + data.length).put(((HttpRequestEditor)editor).getRequest().toByteArray().getBytes()).put(data).array();
        else
            concat = ByteBuffer.allocate(((WebSocketMessageEditor)editor).getContents().getBytes().length + data.length).put(((WebSocketMessageEditor)editor).getContents().getBytes()).put(data).array();

        setBytes(concat);
    }

    public void clear(){
        setBytes(new byte[0]);
    }

}
