package httpraider.controller;

public abstract class AbstractUIController<M, V> {

    protected M model;
    protected V view;

    protected AbstractUIController(M model, V view) {
        this.model = model;
        this.view  = view;
    }

    public M getModel() {
        return model;
    }

    public V getView() {
        return view;
    }
}
