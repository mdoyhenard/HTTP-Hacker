package httpraider.controller.engines;

import httpraider.controller.tools.CustomTagManager;
import httpraider.model.CustomTagModel;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TagEngine {

    public static final int CORRECT = 0;

    private static final Pattern START  = Pattern.compile("<start_([0-9]+)>");
    private static final Pattern END    = Pattern.compile("<end_([0-9]+)>");
    private static final Pattern SIMPLE = Pattern.compile("<(int|hex)_([0-9]+)>");
    private static final Pattern REPEAT_SEGMENT = Pattern.compile("<repeat_([0-9]+)\\(\"([^\"]*)\"\\)>");
    private static final Pattern REPEAT_FIXED = Pattern.compile("<repeat\\(\"([^\"]*)\",\s*([0-9]+)\\)>");

    private static final Pattern TOKEN  = Pattern.compile(
            "<start_([0-9]+)>|<end_([0-9]+)>|<repeat_([0-9]+)\\(\"[^\"]*\"\\)>|<repeat\\(\"[^\"]*\",\s*([0-9]+)\\)>"
    );

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
            } else if (t.group(3) != null) {
                // <repeat_N("string")>
                String id = t.group(3);
                for (Tag open : stack) if (open.id.equals(id)) return pos;
                repeatFirstPos.putIfAbsent(id, pos);
            } else if (t.group(4) != null) {
                // <repeat("string", amount)> - no validation needed
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

        Map<String, Integer> values   = new HashMap<>();
        Map<String, String>  contents = new HashMap<>();

        String noBlocks = resolveBlocks(src, values, contents);
        String out      = replaceAll(noBlocks, values);

        // ── NEW: custom-tag replacement pass ───────────────────────────
        for (CustomTagModel def : CustomTagManager.getInstance().getTags()) {
            String raw = def.getName().trim();
            // strip angle-brackets if the user included them
            if (raw.startsWith("<")  && raw.endsWith(">")) {
                raw = raw.substring(1, raw.length()-1).trim();
            }
            if (raw.isEmpty()) continue;

            Pattern p = Pattern.compile("<" + Pattern.quote(raw) + "_([0-9]+)>");
            Matcher m = p.matcher(out);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                String id    = m.group(1);
                String block = contents.getOrDefault(id, "");
                String repl  = JSEngine.runTagEngine(def.getScript(), block);
                m.appendReplacement(sb, Matcher.quoteReplacement(repl));
            }
            m.appendTail(sb);
            out = sb.toString();
        }

        return out.getBytes(StandardCharsets.ISO_8859_1);
    }

    private static String resolveBlocks(String text,
                                        Map<String, Integer> val,
                                        Map<String, String>  contents) {
        StringBuilder out = new StringBuilder();
        int cursor = 0, len = text.length();

        while (true) {
            Matcher mStart = START.matcher(text).region(cursor, len);
            if (!mStart.find()) {
                out.append(text.substring(cursor));
                break;
            }
            out.append(text, cursor, mStart.start());
            String id = mStart.group(1);
            int depth = 1, scan = mStart.end();

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
                        inner = resolveBlocks(inner, val, contents);
                        inner = replaceReadyRepeats(inner, val);
                        val.put(id, inner.length());
                        contents.put(id, inner);                // <─ RECORD raw text
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
        // Handle <repeat_N("string")>
        Matcher m1 = REPEAT_SEGMENT.matcher(txt);
        StringBuilder sb1 = new StringBuilder();
        while (m1.find()) {
            String id = m1.group(1);
            if (!val.containsKey(id)) continue;
            String rep = m1.group(2).repeat(val.get(id));
            m1.appendReplacement(sb1, Matcher.quoteReplacement(rep));
        }
        m1.appendTail(sb1);
        
        // Handle <repeat("string", amount)>
        String result = sb1.toString();
        Matcher m2 = REPEAT_FIXED.matcher(result);
        StringBuilder sb2 = new StringBuilder();
        while (m2.find()) {
            String str = m2.group(1);
            int amount = Integer.parseInt(m2.group(2));
            String rep = str.repeat(amount);
            m2.appendReplacement(sb2, Matcher.quoteReplacement(rep));
        }
        m2.appendTail(sb2);
        return sb2.toString();
    }

    private static String replaceAll(String txt, Map<String, Integer> val) {
        // Handle <repeat_N("string")>
        txt = REPEAT_SEGMENT.matcher(txt).replaceAll(m -> {
            String id = m.group(1);
            Integer count = val.get(id);
            return count != null ? m.group(2).repeat(count) : m.group(0);
        });
        
        // Handle <repeat("string", amount)>
        txt = REPEAT_FIXED.matcher(txt).replaceAll(m -> m.group(1).repeat(Integer.parseInt(m.group(2))));
        
        // Handle <int_N> and <hex_N>
        txt = SIMPLE.matcher(txt).replaceAll(m -> {
            int v = val.get(m.group(2));
            return m.group(1).equals("hex") ? Integer.toHexString(v) : Integer.toString(v);
        });
        return txt;
    }
}
