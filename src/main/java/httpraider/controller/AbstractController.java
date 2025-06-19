package httpraider.controller;

public abstract class AbstractController<M, V> {

    protected M model;
    protected V view;

    protected AbstractController(M model, V view) {
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
