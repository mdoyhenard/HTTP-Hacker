package httpraider.controller.engines;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.util.HashMap;
import java.util.Map;

public final class JSEngine {

    private JSEngine() {}

    private static void run(String script, Map<String, Object> inputs, Map<String, Object> outputs) {
        try (Context ctx = Context.newBuilder("js").allowAllAccess(true).build()) {
            Value bindings = ctx.getBindings("js");
            for (Map.Entry<String, Object> e : inputs.entrySet()) {
                bindings.putMember(e.getKey(), e.getValue());
            }
            ctx.eval("js", script);
            for (String key : outputs.keySet()) {
                Value v = bindings.getMember(key);
                if (v != null && !v.isNull()) {
                    if (v.isBoolean()) {
                        outputs.put(key, v.asBoolean());
                    } else if (v.isNumber()) {
                        outputs.put(key, v.as(Number.class));
                    } else if (v.isString()) {
                        outputs.put(key, v.asString());
                    } else {
                        outputs.put(key, v.toString());
                    }
                } else {
                    outputs.put(key, null);
                }
            }
        } catch (Exception ignored) {}
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
