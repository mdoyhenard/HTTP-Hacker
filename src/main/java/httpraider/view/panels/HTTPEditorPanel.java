package httpraider.view.panels;

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.ui.editor.Editor;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import burp.api.montoya.ui.editor.WebSocketMessageEditor;
import httpraider.view.components.SwitchButton;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

import static javax.swing.SwingUtilities.invokeLater;

public class HTTPEditorPanel<T extends Editor> extends JPanel {

    private final JPanel topPanel;
    private final JLabel name;
    private final T editor;
    private SwitchButton switchButton;

    public HTTPEditorPanel(String text, T editor){
        super(new BorderLayout());
        topPanel = new JPanel(new BorderLayout());
        this.editor = editor;
        switchButton = null;
        name = new JLabel(text);
        name.setFont(name.getFont().deriveFont(Font.BOLD, 13f));
        topPanel.setBorder(BorderFactory.createEmptyBorder(7, 12, 5, 0));
        topPanel.add(name, BorderLayout.WEST);
        add(topPanel, BorderLayout.NORTH);
        add(this.editor.uiComponent(), BorderLayout.CENTER);
    }

    public void setSwitch(String text, ActionListener l){
        if (switchButton != null) topPanel.remove(switchButton);
        switchButton =  new SwitchButton(text);
        switchButton.addActionListener(l);
        topPanel.add(switchButton, BorderLayout.EAST);
    }

    public void removeSwitch(){
        if (switchButton != null) topPanel.remove(switchButton);
        switchButton = null;
    }

    public void setName(String text){ name.setText(text); }

    public java.util.Optional<burp.api.montoya.ui.Selection> getSelection(){
        return editor.selection();
    }

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

    public void setSearchExpression(String expression){
        editor.setSearchExpression(expression);
    }

    public int getCaretPosition(){
        return editor.caretPosition();
    }

    public void setCaretToLastPosition(){
        setCaretPosition(getBytes().length+1);
    }

    public void setCaretPosition(int pos) {
        SwingUtilities.invokeLater(() ->
                SwingUtilities.invokeLater(() -> {
                    Component ui = editor.uiComponent();
                    Method setSel = null;
                    for (Method m : ui.getClass().getMethods()) {
                        if (m.getParameterCount() == 2 &&
                                m.getParameterTypes()[0] == int.class &&
                                m.getParameterTypes()[1] == int.class &&
                                m.getReturnType() == void.class) {
                            setSel = m;
                            break;
                        }
                    }
                    if (setSel == null) {
                        return;
                    }


                    int len = pos;
                    try {
                        Method getSelection = ui.getClass().getMethod("getSelection");
                        int[] sel = (int[]) getSelection.invoke(ui);
                        len = Math.max(len, sel[1]);               // last known end
                    } catch (Exception ignored) { }

                    int clamp = Math.max(0, Math.min(pos, len));

                    try {
                        setSel.invoke(ui, clamp, clamp);
                        ui.requestFocusInWindow();
                    } catch (Exception ignored) {}
                })
        );
    }

    // In your HTTPEditorPanel<T extends Editor> class:
    public void addEditorFocusListener(java.awt.event.FocusListener listener) {
        editor.uiComponent().addFocusListener(listener);
    }


}
