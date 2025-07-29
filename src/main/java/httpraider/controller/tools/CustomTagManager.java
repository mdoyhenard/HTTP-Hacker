// src/httpraider/controller/tools/CustomTagManager.java
package httpraider.controller.tools;

import httpraider.model.CustomTagModel;
import httpraider.model.PersistenceManager;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class CustomTagManager {

    private static final String PERSISTENCE_KEY = "HTTPRaider.customTags";
    private static final CustomTagManager INSTANCE = new CustomTagManager();

    private final List<CustomTagModel> tags = new ArrayList<>();
    private final List<CustomTagListener> listeners = new CopyOnWriteArrayList<>();

    private CustomTagManager() {
        Optional<CustomTagModel[]> opt =
                PersistenceManager.load(PERSISTENCE_KEY, CustomTagModel[].class);
        if (opt.isPresent()) {
            tags.addAll(Arrays.asList(opt.get()));
        }
    }

    public static CustomTagManager getInstance() {
        return INSTANCE;
    }

    public synchronized List<CustomTagModel> getTags() {
        return new ArrayList<>(tags);
    }

    public void addListener(CustomTagListener l) {
        listeners.add(l);
    }

    public void removeListener(CustomTagListener l) {
        listeners.remove(l);
    }

    private void notifyListeners() {
        List<CustomTagModel> snapshot = getTags();
        for (CustomTagListener l : listeners) {
            SwingUtilities.invokeLater(() -> l.tagsChanged(snapshot));
        }
    }

    public synchronized void addTag(CustomTagModel def) {
        tags.add(def);
        save();
        notifyListeners();
    }

    public synchronized void removeTag(CustomTagModel def) {
        tags.remove(def);
        save();
        notifyListeners();
    }

    public synchronized void updateTag(CustomTagModel def) {
        save();
        notifyListeners();
    }

    private void save() {
        PersistenceManager.save(PERSISTENCE_KEY, tags.toArray(new CustomTagModel[0]));
    }

    public interface CustomTagListener {
        void tagsChanged(List<CustomTagModel> newTags);
    }
}
