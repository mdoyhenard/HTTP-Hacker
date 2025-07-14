package httpraider.controller.engines;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import java.util.*;

public final class JSEngine {

    private JSEngine() {}

    private static void run(String script, Map<String, Object> inputs, Map<String, Object> outputs) {
        Context ctx = Context.enter();
        try {
            Scriptable scope = ctx.initStandardObjects();
            for (Map.Entry<String, Object> e : inputs.entrySet()) {
                Object jsObj = Context.javaToJS(e.getValue(), scope);
                org.mozilla.javascript.ScriptableObject.putProperty(scope, e.getKey(), jsObj);
            }
            ctx.evaluateString(scope, script, "script", 1, null);
            for (String key : outputs.keySet()) {
                Object value = org.mozilla.javascript.ScriptableObject.getProperty(scope, key);
                if (value == Scriptable.NOT_FOUND) {
                    outputs.put(key, null);
                } else if (value instanceof Boolean) {
                    outputs.put(key, (Boolean) value);
                } else if (value instanceof Number) {
                    outputs.put(key, (Number) value);
                } else if (value instanceof String) {
                    outputs.put(key, (String) value);
                } else if (value instanceof org.mozilla.javascript.NativeArray) {
                    outputs.put(key, value);
                } else if (value instanceof org.mozilla.javascript.NativeJavaObject) {
                    outputs.put(key, ((org.mozilla.javascript.NativeJavaObject) value).unwrap());
                } else {
                    outputs.put(key, value.toString());
                }
            }
        } catch (Exception ex) {
            System.out.println(ex);
        } finally {
            Context.exit();
        }
    }

    public static String runTagEngine(String script, String input) {
        Map<String, Object> in = new HashMap<>();
        in.put("input", input);
        Map<String, Object> out = new HashMap<>();
        out.put("output", null);
        if (script == null || script.trim().isEmpty()) return "";
        run(script, in, out);
        Object result = out.get("output");
        return result == null ? "" : result.toString();
    }

    public static byte[][] runEndHeaderScript(byte[] headers, byte[] buffer, String script) {
        try {
            Map<String, Object> in = new HashMap<>();
            in.put("headers", new String(headers, "ISO-8859-1"));
            in.put("buffer", new String(buffer, "ISO-8859-1"));
            Map<String, Object> out = new HashMap<>();
            out.put("outHeaders", null);
            out.put("outBuffer", null);

            if (script == null || script.trim().isEmpty()) throw new IllegalArgumentException("The script is empty or null");
            run(script, in, out);

            byte[] outHeaders = out.get("outHeaders") == null ? "".getBytes() : out.get("outHeaders").toString().getBytes();
            byte[] outPayload = out.get("outBuffer") == null ? "".getBytes() : out.get("outBuffer").toString().getBytes();
            return new byte[][] { outHeaders, outPayload };
        } catch (Exception e) {
            return new byte[][] { headers, buffer };
        }
    }

    public static List<String> runSplitHeaderScript(List<String> headerLines, byte[] headers, String script) {
        try {
            Map<String, Object> in = new HashMap<>();
            in.put("headerLines", headerLines.toArray(new String[0]));
            in.put("headerBlock", new String(headers, "ISO-8859-1"));
            Map<String, Object> out = new HashMap<>();
            out.put("outHeaderLines", null);

            if (script == null || script.trim().isEmpty()) throw new IllegalArgumentException("The script is empty or null");
            run(script, in, out);

            List<String> outHeaderList = new ArrayList<>();
            org.mozilla.javascript.NativeArray nativeArray = (org.mozilla.javascript.NativeArray) out.get("outHeaderLines");
            for (int i = 0; i < nativeArray.getLength(); i++) {
                Object val = nativeArray.get(i, nativeArray);
                if (val instanceof org.mozilla.javascript.NativeJavaObject) {
                    Object realObj = ((org.mozilla.javascript.NativeJavaObject) val).unwrap();
                    outHeaderList.add(realObj != null ? realObj.toString() : null);
                } else if (val != null) {
                    outHeaderList.add(val.toString());
                } else {
                    outHeaderList.add(null);
                }
            }
            return outHeaderList;
        } catch (Exception e) {
            return headerLines;
        }
    }

    public static byte[][] runBodyLenScript(List<String> headerLines, byte[] body, byte[] buffer, String script) {
        try {
            Map<String, Object> in = new HashMap<>();
            in.put("headerLines", headerLines.toArray(new String[0]));
            in.put("body", new String(body, "ISO-8859-1"));
            in.put("buffer", new String(buffer, "ISO-8859-1"));
            Map<String, Object> out = new HashMap<>();
            out.put("outBody", null);
            out.put("outBuffer", null);

            if (script == null || script.trim().isEmpty()) throw new IllegalArgumentException("The script is empty or null");
            run(script, in, out);

            byte[] outBody = out.get("outBody") == null ? "".getBytes() : out.get("outBody").toString().getBytes();
            byte[] outBuffer = out.get("outBuffer") == null ? "".getBytes() : out.get("outBuffer").toString().getBytes();
            return new byte[][] { outBody, outBuffer };
        } catch (Exception e) {
            return new byte[][] { body, buffer };
        }
    }

    public static Object runJsBooleanRule(String js, String headers, String body) {
        // Provide input variables 'headers' and 'body', expect 'result' or 'output' boolean as output
        org.mozilla.javascript.Context ctx = org.mozilla.javascript.Context.enter();
        try {
            org.mozilla.javascript.Scriptable scope = ctx.initStandardObjects();
            scope.put("headers", scope, headers);
            scope.put("body", scope, body);
            ctx.evaluateString(scope, js, "rule", 1, null);
            Object result = org.mozilla.javascript.ScriptableObject.getProperty(scope, "result");
            if (result == org.mozilla.javascript.Scriptable.NOT_FOUND) {
                result = org.mozilla.javascript.ScriptableObject.getProperty(scope, "output");
            }
            if (result instanceof Boolean) return (Boolean) result;
            if (result instanceof String) return Boolean.parseBoolean((String) result);
            return false;
        } finally {
            org.mozilla.javascript.Context.exit();
        }
    }

}
