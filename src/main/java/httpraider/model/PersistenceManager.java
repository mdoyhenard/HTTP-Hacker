package httpraider.model;

import extension.HTTPRaiderExtension;
import burp.api.montoya.core.ByteArray;

import java.io.*;
import java.util.Optional;

public final class PersistenceManager {

    private PersistenceManager() { }

    public static <T extends Serializable> void save(String key, T value) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos))
        {
            oos.writeObject(value);
            oos.flush();
            HTTPRaiderExtension.API.persistence().extensionData().setByteArray(key, ByteArray.byteArray(bos.toByteArray()));

        } catch (IOException e) {
            HTTPRaiderExtension.API.logging().logToError("There was an error at persisting data: " + e.getMessage());
        }
    }

    public static <T extends Serializable> Optional<T> load(String key, Class<T> type) {
        ByteArray rawData = HTTPRaiderExtension.API.persistence().extensionData().getByteArray(key);
        if (rawData == null) return Optional.empty();
        byte[] data =  rawData.getBytes();

        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))) {
            Object obj = ois.readObject();
            return type.isInstance(obj) ? Optional.of(type.cast(obj)) : Optional.empty();
        } catch (ClassNotFoundException | IOException e) {
            return Optional.empty();
        }
    }
}
