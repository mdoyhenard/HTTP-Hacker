package httpraider.controller.engines;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import java.util.HashMap;
import java.util.Map;

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
                } else {
                    outputs.put(key, value.toString());
                }
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        } finally {
            Context.exit();
        }
    }


    public static String runTagEngine(String script, String input) {
        Map<String, Object> in = new HashMap<>();
        in.put("input", input);
        Map<String, Object> out = new HashMap<>();
        out.put("output", null);
        run(script, in, out);
        Object result = out.get("output");
        return result == null ? "" : result.toString();
    }
}
