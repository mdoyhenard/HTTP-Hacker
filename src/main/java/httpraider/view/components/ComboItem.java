package httpraider.view.components;

public class ComboItem<T> {

    private String key;
    private T value;

    public ComboItem(String key, T value) {
        this.key = key;
        this.value = value;
    }

    public String toString() { return key; }

    public T getValue() { return value; }
}