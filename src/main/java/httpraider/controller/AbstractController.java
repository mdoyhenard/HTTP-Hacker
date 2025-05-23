package httpraider.controller;

import javax.swing.SwingUtilities;

/**
 * Generic base for all HTTPRaider controllers.
 *
 * @param <M> model  (plain POJO, serialisable)
 * @param <V> view   (Swing component tree)
 */
public abstract class AbstractController<M, V> {

    protected final M model;
    protected final V view;

    protected AbstractController(M model, V view) {
        this.model = model;
        this.view  = view;
        wireView();
    }

    protected abstract void wireView();

    public void dispose() { /* default: nothing */ }

    protected static void ui(Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) r.run();
        else SwingUtilities.invokeLater(r);
    }

}
