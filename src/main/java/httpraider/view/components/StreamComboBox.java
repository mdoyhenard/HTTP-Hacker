package httpraider.view.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class StreamComboBox<T> extends JPanel implements ActionComponent {

    private final JLabel label;
    private final JComboBox<ComboItem<T>> comboBox;

    public StreamComboBox(String labelText) {
        super(new BorderLayout(5, 0));
        this.label = new JLabel(labelText);
        this.comboBox = new JComboBox<>();

        add(label, BorderLayout.WEST);
        add(comboBox, BorderLayout.CENTER);

        JPanel rightSpace = new JPanel();
        rightSpace.setPreferredSize(new Dimension(8, 1));
        rightSpace.setOpaque(false);
        add(rightSpace, BorderLayout.EAST);
    }

    public StreamComboBox(String labelText, ComboItem<T>[] items) {
        super(new BorderLayout(5, 0));
        this.label = new JLabel(labelText);
        this.comboBox = new JComboBox<>(items);

        add(label, BorderLayout.WEST);
        add(comboBox, BorderLayout.CENTER);

        JPanel rightSpace = new JPanel();
        rightSpace.setPreferredSize(new Dimension(8, 1));
        rightSpace.setOpaque(false);
        add(rightSpace, BorderLayout.EAST);
    }

    public void addItem(ComboItem<T> item) {
        comboBox.addItem(item);
    }

    public ComboItem<T> getSelectedItem() {
        return (ComboItem<T>) comboBox.getSelectedItem();
    }

    public T getSelectedValue() {
        ComboItem<T> item = (ComboItem<T>) comboBox.getSelectedItem();
        return item != null ? item.getValue() : null;
    }

    public T getValueAt(int index){
        return comboBox.getItemAt(index).getValue();
    }

    public void setSelectedIndex(int index) {
        comboBox.setSelectedIndex(index);
    }

    public int getSelectedIndex() {
        return comboBox.getSelectedIndex();
    }

    public void setEnabled(boolean enabled) {
        comboBox.setEnabled(enabled);
    }

    public JComboBox<ComboItem<T>> getComboBox() {
        return comboBox;
    }

    @Override
    public void addActionListener(ActionListener listener) {
        comboBox.addActionListener(listener);
    }
}
