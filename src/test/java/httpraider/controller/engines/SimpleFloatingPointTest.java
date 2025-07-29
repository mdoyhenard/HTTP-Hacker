package httpraider.controller.engines;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple test to demonstrate the floating point issue without requiring
 * the full application context or Burp API.
 */
public class SimpleFloatingPointTest {

    @Test
    @DisplayName("Demonstrate JavaScript Number to String conversion issue")
    void demonstrateJavaScriptNumberToString() {
        // Test how Rhino JavaScript engine converts numbers to strings
        
        Context ctx = Context.enter();
        try {
            Scriptable scope = ctx.initStandardObjects();
            
            // Test 1: Integer addition
            Object result1 = ctx.evaluateString(scope, "10 + 5", "test", 1, null);
            System.out.println("10 + 5 = " + result1 + " (type: " + result1.getClass().getName() + ")");
            System.out.println("toString(): " + result1.toString());
            
            // Test 2: Division (should produce float)
            Object result2 = ctx.evaluateString(scope, "10 / 3", "test", 1, null);
            System.out.println("10 / 3 = " + result2 + " (type: " + result2.getClass().getName() + ")");
            System.out.println("toString(): " + result2.toString());
            
            // Test 3: Using explicit toString in JavaScript
            Object result3 = ctx.evaluateString(scope, "(10 + 5).toString()", "test", 1, null);
            System.out.println("(10 + 5).toString() = " + result3 + " (type: " + result3.getClass().getName() + ")");
            
            // Test 4: Number variable
            ctx.evaluateString(scope, "var num = 15;", "test", 1, null);
            Object num = scope.get("num", scope);
            System.out.println("var num = 15; num = " + num + " (type: " + num.getClass().getName() + ")");
            
            // Check if integer addition produces decimal
            if (result1 instanceof Number) {
                String stringResult = result1.toString();
                boolean hasDecimal = stringResult.contains(".");
                System.out.println("\nINTEGER ADDITION RESULT: " + stringResult);
                System.out.println("Contains decimal point: " + hasDecimal);
                
                if (hasDecimal) {
                    System.out.println("ISSUE CONFIRMED: Integer addition produces floating point string!");
                }
            }
            
        } finally {
            Context.exit();
        }
    }

    @Test
    @DisplayName("Test JSEngine.runTagEngine number handling")
    void testJSEngineRunTagEngineNumbers() {
        // Test various number operations through JSEngine
        
        // Test 1: Direct integer
        String result1 = JSEngine.runTagEngine("output = 15;", "");
        System.out.println("Direct integer (15): '" + result1 + "'");
        assertNotNull(result1);
        
        // Test 2: Integer addition
        String result2 = JSEngine.runTagEngine("output = 10 + 5;", "");
        System.out.println("Integer addition (10 + 5): '" + result2 + "'");
        assertNotNull(result2);
        
        // Test 3: Integer multiplication
        String result3 = JSEngine.runTagEngine("output = 3 * 4;", "");
        System.out.println("Integer multiplication (3 * 4): '" + result3 + "'");
        assertNotNull(result3);
        
        // Test 4: Division (should have decimal)
        String result4 = JSEngine.runTagEngine("output = 10 / 3;", "");
        System.out.println("Division (10 / 3): '" + result4 + "'");
        assertNotNull(result4);
        
        // Test 5: With explicit toString()
        String result5 = JSEngine.runTagEngine("output = (10 + 5).toString();", "");
        System.out.println("With toString() (10 + 5).toString(): '" + result5 + "'");
        assertEquals("15", result5);
        
        // Test 6: Math.floor
        String result6 = JSEngine.runTagEngine("output = Math.floor(10.7);", "");
        System.out.println("Math.floor(10.7): '" + result6 + "'");
        assertNotNull(result6);
        
        // Analyze results
        System.out.println("\n=== ANALYSIS ===");
        System.out.println("Result 1 (direct 15) has decimal: " + result1.contains("."));
        System.out.println("Result 2 (10 + 5) has decimal: " + result2.contains("."));
        System.out.println("Result 3 (3 * 4) has decimal: " + result3.contains("."));
        System.out.println("Result 6 (Math.floor) has decimal: " + result6.contains("."));
    }

    @Test
    @DisplayName("Test workaround: ensuring integer string output")
    void testIntegerWorkarounds() {
        // Test different approaches to ensure integer output
        
        // Approach 1: Use toString() explicitly
        String approach1 = JSEngine.runTagEngine(
            "var sum = 10 + 20 + 30;" +
            "output = sum.toString();", 
            ""
        );
        assertEquals("60", approach1);
        assertFalse(approach1.contains("."), "toString() should not add decimal");
        
        // Approach 2: Use Math.floor for safety
        String approach2 = JSEngine.runTagEngine(
            "var sum = 10 + 20 + 30;" +
            "output = Math.floor(sum).toString();", 
            ""
        );
        assertEquals("60", approach2);
        assertFalse(approach2.contains("."), "Math.floor should ensure integer");
        
        // Approach 3: Use parseInt on the result
        String approach3 = JSEngine.runTagEngine(
            "var sum = 10 + 20 + 30;" +
            "output = parseInt(sum).toString();", 
            ""
        );
        assertEquals("60", approach3);
        assertFalse(approach3.contains("."), "parseInt should ensure integer");
        
        // Approach 4: String concatenation trick
        String approach4 = JSEngine.runTagEngine(
            "var sum = 10 + 20 + 30;" +
            "output = '' + sum;", 
            ""
        );
        System.out.println("String concatenation result: '" + approach4 + "'");
    }

    @Test
    @DisplayName("Test the actual issue in JSEngine implementation")
    void testJSEngineImplementation() {
        // Looking at JSEngine.runTagEngine, line 54:
        // return result == null ? "" : result.toString();
        
        // The issue might be that when 'output' is a JavaScript Number,
        // it's being converted to Java Double, and Double.toString()
        // can produce "15.0" instead of "15"
        
        // Let's trace through what happens
        Context ctx = Context.enter();
        try {
            Scriptable scope = ctx.initStandardObjects();
            
            // Simulate what happens in JSEngine
            ctx.evaluateString(scope, "output = 10 + 5;", "test", 1, null);
            Object output = scope.get("output", scope);
            
            System.out.println("JavaScript output type: " + output.getClass().getName());
            System.out.println("JavaScript output value: " + output);
            System.out.println("toString() result: " + output.toString());
            
            if (output instanceof Number) {
                Number num = (Number) output;
                System.out.println("Number value: " + num);
                System.out.println("intValue(): " + num.intValue());
                System.out.println("doubleValue(): " + num.doubleValue());
                
                // Check if it's actually an integer value
                if (num.doubleValue() == num.intValue()) {
                    System.out.println("This is an integer value stored as Double!");
                    System.out.println("Should return: " + num.intValue());
                    System.out.println("But returns: " + num.toString());
                }
            }
            
        } finally {
            Context.exit();
        }
    }

    @Test
    @DisplayName("Proposed fix for JSEngine")
    void testProposedFix() {
        // Test a proposed fix that would check if a Number is actually an integer
        
        String script = "output = 10 + 5;";
        String input = "";
        
        // Current behavior
        String currentResult = JSEngine.runTagEngine(script, input);
        System.out.println("Current result: '" + currentResult + "'");
        
        // Proposed fix would be to modify JSEngine.runTagEngine to check
        // if the Number result is actually an integer and format accordingly
        String proposedResult = runTagEngineFixed(script, input);
        System.out.println("Proposed result: '" + proposedResult + "'");
        
        assertEquals("15", proposedResult);
        assertFalse(proposedResult.contains("."));
    }
    
    // Proposed fix implementation
    private String runTagEngineFixed(String script, String input) {
        Context ctx = Context.enter();
        try {
            Scriptable scope = ctx.initStandardObjects();
            scope.put("input", scope, input);
            scope.put("output", scope, null);
            
            ctx.evaluateString(scope, script, "script", 1, null);
            Object result = scope.get("output", scope);
            
            if (result == null || result == Scriptable.NOT_FOUND) {
                return "";
            }
            
            // FIX: Check if Number is actually an integer
            if (result instanceof Number) {
                Number num = (Number) result;
                double d = num.doubleValue();
                if (d == Math.floor(d) && !Double.isInfinite(d)) {
                    // It's an integer value
                    return String.valueOf(num.intValue());
                }
            }
            
            return result.toString();
            
        } finally {
            Context.exit();
        }
    }
}