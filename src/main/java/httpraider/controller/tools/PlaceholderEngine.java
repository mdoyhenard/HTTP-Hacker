package httpraider.controller.tools;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlaceholderEngine {

    public static final int CORRECT = 0;

    private static final Pattern START = Pattern.compile("<start_([0-9]+)>");
    private static final Pattern END = Pattern.compile("<end_([0-9]+)>");
    private static final Pattern SIMPLE = Pattern.compile("<(int|hex)_([0-9]+)>");
    private static final Pattern REPEAT = Pattern.compile("<repeat\\(([0-9]+),\"([^\"]*)\"\\)>");
    private static final Pattern TOKEN = Pattern.compile(
            "<start_([0-9]+)>|<end_([0-9]+)>|<repeat\\(([0-9]+),\"[^\"]*\"\\)>");

    private record Tag(String id, int pos) {}

    public static int validate(byte[] data) {
        String src = new String(data, StandardCharsets.ISO_8859_1);
        Deque<Tag> stack = new ArrayDeque<>();
        Set<String> closed = new HashSet<>();
        Set<String> allIds = new HashSet<>();
        Map<String, Integer> repeatFirstPos = new HashMap<>();

        Matcher t = TOKEN.matcher(src);
        while (t.find()) {
            int pos = t.start();

            if (t.group(1) != null) {
                String id = t.group(1);
                if (closed.contains(id)) return pos;
                allIds.add(id);
                stack.push(new Tag(id, pos));
            } else if (t.group(2) != null) {
                String id = t.group(2);
                if (stack.isEmpty()) return pos;
                Tag open = stack.pop();
                if (!open.id.equals(id)) return pos;
                closed.add(id);
            } else {
                String id = t.group(3);
                for (Tag open : stack) if (open.id.equals(id)) return pos;
                repeatFirstPos.putIfAbsent(id, pos);
            }
        }
        if (!stack.isEmpty()) return stack.peek().pos;
        for (Map.Entry<String, Integer> e : repeatFirstPos.entrySet())
            if (!allIds.contains(e.getKey())) return e.getValue();
        return CORRECT;
    }

    public static byte[] resolve(byte[] data) {
        String src = new String(data, StandardCharsets.ISO_8859_1);
        if (validate(data) != CORRECT) return data;
        Map<String, Integer> values = new HashMap<>();
        String noBlocks = resolveBlocks(src, values);
        String out = replaceAll(noBlocks, values);
        return out.getBytes(StandardCharsets.ISO_8859_1);
    }

    private static String resolveBlocks(String text, Map<String, Integer> val) {
        StringBuilder out = new StringBuilder();
        int cursor = 0;
        int len = text.length();

        while (true) {
            Matcher mStart = START.matcher(text).region(cursor, len);
            if (!mStart.find()) {
                out.append(text.substring(cursor));
                break;
            }
            out.append(text, cursor, mStart.start());
            String id = mStart.group(1);
            int depth = 1;
            int scan = mStart.end();

            while (depth > 0) {
                Matcher nextS = START.matcher(text).region(scan, len);
                Matcher nextE = END.matcher(text).region(scan, len);
                boolean sFound = nextS.find();
                nextE.find();
                int sPos = sFound ? nextS.start() : Integer.MAX_VALUE;
                int ePos = nextE.start();

                if (sPos < ePos) {
                    depth++;
                    scan = nextS.end();
                } else {
                    depth--;
                    if (depth == 0) {
                        String inner = text.substring(mStart.end(), ePos);
                        inner = resolveBlocks(inner, val);
                        inner = replaceReadyRepeats(inner, val);
                        val.put(id, inner.length());
                        out.append(inner);
                        cursor = nextE.end();
                    } else {
                        scan = nextE.end();
                    }
                }
            }
        }
        return out.toString();
    }

    private static String replaceReadyRepeats(String txt, Map<String, Integer> val) {
        Matcher m = REPEAT.matcher(txt);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String id = m.group(1);
            if (!val.containsKey(id)) continue;
            String rep = m.group(2).repeat(val.get(id));
            m.appendReplacement(sb, Matcher.quoteReplacement(rep));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String replaceAll(String txt, Map<String, Integer> val) {
        txt = REPEAT.matcher(txt).replaceAll(m -> m.group(2).repeat(val.get(m.group(1))));
        txt = SIMPLE.matcher(txt).replaceAll(m -> {
            int v = val.get(m.group(2));
            return m.group(1).equals("hex") ? Integer.toHexString(v) : Integer.toString(v);
        });
        return txt;
    }
}
