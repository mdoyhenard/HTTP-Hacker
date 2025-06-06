package httpraider.controller;

import javax.swing.SwingUtilities;

/**
 * Generic base for all controllers.
 *
 * @param <M> model  type
 * @param <V> view   type
 */
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
