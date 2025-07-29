package proxyFinder.engine;

public class ResponseNormalizer {
    public static String normalizeBody(String body) {
        if (body == null) return "";
        return body.trim().replaceAll("\\s+", " ").toLowerCase();
    }
}
