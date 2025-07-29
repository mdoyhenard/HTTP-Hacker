package httpraider.controller.engines;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test to identify all cases where JavaScript operations
 * produce unexpected floating point representations for integer values.
 */
public class ComprehensiveFloatingPointTest {

    private static class TestCase {
        String description;
        String script;
        String expectedResult;
        boolean shouldHaveDecimal;

        TestCase(String description, String script, String expectedResult, boolean shouldHaveDecimal) {
            this.description = description;
            this.script = script;
            this.expectedResult = expectedResult;
            this.shouldHaveDecimal = shouldHaveDecimal;
        }
    }

    @Test
    @DisplayName("Comprehensive test of JavaScript number operations")
    void testAllNumberOperations() {
        List<TestCase> testCases = new ArrayList<>();
        
        // Basic arithmetic operations
        testCases.add(new TestCase("Integer addition", "output = 10 + 5;", "15", false));
        testCases.add(new TestCase("Integer subtraction", "output = 20 - 5;", "15", false));
        testCases.add(new TestCase("Integer multiplication", "output = 3 * 5;", "15", false));
        testCases.add(new TestCase("Integer division (exact)", "output = 15 / 3;", "5", false));
        testCases.add(new TestCase("Integer division (inexact)", "output = 10 / 3;", "3.3333333333333335", true));
        
        // Math functions that should return integers
        testCases.add(new TestCase("Math.floor with decimal", "output = Math.floor(10.7);", "10", false));
        testCases.add(new TestCase("Math.ceil with decimal", "output = Math.ceil(10.1);", "11", false));
        testCases.add(new TestCase("Math.round with decimal", "output = Math.round(10.5);", "11", false));
        testCases.add(new TestCase("Math.abs with integer", "output = Math.abs(-15);", "15", false));
        testCases.add(new TestCase("Math.min with integers", "output = Math.min(10, 15, 5);", "5", false));
        testCases.add(new TestCase("Math.max with integers", "output = Math.max(10, 15, 5);", "15", false));
        
        // Math functions that naturally return floats
        testCases.add(new TestCase("Math.sqrt (perfect square)", "output = Math.sqrt(9);", "3", false));
        testCases.add(new TestCase("Math.sqrt (non-perfect)", "output = Math.sqrt(10);", "3.1622776601683795", true));
        testCases.add(new TestCase("Math.pow integer result", "output = Math.pow(2, 3);", "8", false));
        testCases.add(new TestCase("Math.pow float result", "output = Math.pow(2, 0.5);", "1.4142135623730951", true));
        
        // Parsing functions
        testCases.add(new TestCase("parseInt from string", "output = parseInt('15');", "15", false));
        testCases.add(new TestCase("parseInt from float string", "output = parseInt('15.7');", "15", false));
        testCases.add(new TestCase("parseFloat from int string", "output = parseFloat('15');", "15", false));
        testCases.add(new TestCase("parseFloat from float string", "output = parseFloat('15.7');", "15.7", true));
        
        // Bitwise operations (always return integers)
        testCases.add(new TestCase("Bitwise OR", "output = 8 | 7;", "15", false));
        testCases.add(new TestCase("Bitwise AND", "output = 15 & 7;", "7", false));
        testCases.add(new TestCase("Bitwise XOR", "output = 12 ^ 7;", "11", false));
        testCases.add(new TestCase("Bitwise NOT", "output = ~-16;", "15", false));
        testCases.add(new TestCase("Left shift", "output = 15 << 1;", "30", false));
        testCases.add(new TestCase("Right shift", "output = 30 >> 1;", "15", false));
        
        // Special cases
        testCases.add(new TestCase("Modulo operation", "output = 17 % 5;", "2", false));
        testCases.add(new TestCase("Increment", "var x = 14; output = ++x;", "15", false));
        testCases.add(new TestCase("Decrement", "var x = 16; output = --x;", "15", false));
        
        // String conversion methods
        testCases.add(new TestCase("Explicit toString", "output = (10 + 5).toString();", "15", false));
        testCases.add(new TestCase("String concatenation", "output = '' + (10 + 5);", "15", false));
        testCases.add(new TestCase("Template literal", "output = `${10 + 5}`;", "15", false));
        
        // Run all test cases
        List<String> failures = new ArrayList<>();
        
        for (TestCase tc : testCases) {
            String result = JSEngine.runTagEngine(tc.script, "");
            boolean hasDecimal = result.contains(".");
            
            System.out.printf("%-35s: %-25s (decimal: %s)%n", 
                tc.description, result, hasDecimal ? "YES" : "NO");
            
            // Check if the result has unexpected decimal
            if (!tc.shouldHaveDecimal && hasDecimal) {
                failures.add(String.format("%s returned '%s' with unexpected decimal", 
                    tc.description, result));
            }
        }
        
        // Report failures
        if (!failures.isEmpty()) {
            System.out.println("\n=== UNEXPECTED DECIMAL POINTS FOUND ===");
            for (String failure : failures) {
                System.out.println("- " + failure);
            }
        }
        
        // Assert specific problematic cases
        assertEquals("10", JSEngine.runTagEngine("output = Math.floor(10.7);", ""), 
            "Math.floor returns decimal for integer result");
        assertEquals("11", JSEngine.runTagEngine("output = Math.ceil(10.1);", ""), 
            "Math.ceil returns decimal for integer result");
        assertEquals("11", JSEngine.runTagEngine("output = Math.round(10.5);", ""), 
            "Math.round returns decimal for integer result");
    }

    @Test
    @DisplayName("Test fix suggestions for floating point issue")
    void testFixSuggestions() {
        // The issue has been fixed! Math functions now return proper integers
        String problemScript = "output = Math.floor(10.7);";
        String problemResult = JSEngine.runTagEngine(problemScript, "");
        assertEquals("10", problemResult);
        assertFalse(problemResult.contains("."), "Fix is working - no decimal point");
        
        // These workarounds still work but are no longer necessary
        String fix1Script = "output = Math.floor(10.7) | 0;";
        String fix1Result = JSEngine.runTagEngine(fix1Script, "");
        assertEquals("10", fix1Result);
        assertFalse(fix1Result.contains("."));
        
        // Fix 2: Use parseInt
        String fix2Script = "output = parseInt(Math.floor(10.7));";
        String fix2Result = JSEngine.runTagEngine(fix2Script, "");
        assertEquals("10", fix2Result);
        assertFalse(fix2Result.contains("."));
        
        // Fix 3: Custom formatting function
        String fix3Script = 
            "function formatInt(n) { " +
            "  var i = parseInt(n); " +
            "  return (i == n) ? i.toString() : n.toString(); " +
            "} " +
            "output = formatInt(Math.floor(10.7));";
        String fix3Result = JSEngine.runTagEngine(fix3Script, "");
        assertEquals("10", fix3Result);
        assertFalse(fix3Result.contains("."));
        
        System.out.println("=== ISSUE FIXED ===");
        System.out.println("Math.floor now correctly returns: '" + problemResult + "' (no decimal!)");
        System.out.println("Workaround 1 (bitwise OR): " + fix1Script + " returns '" + fix1Result + "'");
        System.out.println("Workaround 2 (parseInt): " + fix2Script + " returns '" + fix2Result + "'");
        System.out.println("Workaround 3 (custom function): returns '" + fix3Result + "'");
    }

    @Test
    @DisplayName("Test real-world scenarios affected by floating point issue")
    void testRealWorldScenarios() {
        // Scenario 1: Content-Length calculation
        String contentLengthScript = 
            "var body = input;" +
            "var extraHeaders = 20;" +
            "output = body.length + extraHeaders;";
        String result1 = JSEngine.runTagEngine(contentLengthScript, "Hello World");
        assertEquals("31", result1);
        assertFalse(result1.contains("."), "Content-Length should be integer");
        
        // Scenario 2: Chunk size calculation with Math.ceil
        String chunkSizeScript = 
            "var dataSize = parseFloat(input);" +
            "var chunkSize = 1024;" +
            "output = Math.ceil(dataSize / chunkSize);";
        String result2 = JSEngine.runTagEngine(chunkSizeScript, "2500");
        assertEquals("3", result2); // Fixed! No more decimal
        
        // Scenario 2 Fixed: Chunk size calculation with proper integer conversion
        String chunkSizeFixedScript = 
            "var dataSize = parseFloat(input);" +
            "var chunkSize = 1024;" +
            "output = Math.ceil(dataSize / chunkSize) | 0;";
        String result2Fixed = JSEngine.runTagEngine(chunkSizeFixedScript, "2500");
        assertEquals("3", result2Fixed);
        
        // Scenario 3: Array length operations
        String arrayLengthScript = 
            "var items = input.split(',');" +
            "output = items.length;";
        String result3 = JSEngine.runTagEngine(arrayLengthScript, "a,b,c,d,e");
        assertEquals("5", result3);
        assertFalse(result3.contains("."), "Array length should be integer");
        
        System.out.println("=== REAL-WORLD SCENARIOS ===");
        System.out.println("Content-Length calculation: " + result1 + " (OK)");
        System.out.println("Chunk count: " + result2 + " (OK - Fix applied!)");
        System.out.println("Chunk count (with workaround): " + result2Fixed + " (OK)");
        System.out.println("Array length: " + result3 + " (OK)");
    }

    @Test
    @DisplayName("Document best practices for avoiding floating point issues")
    void documentBestPractices() {
        // Best practice examples that work correctly
        String[] bestPractices = {
            // Use bitwise operations to ensure integers
            "output = (Math.floor(10.7) | 0).toString();",
            "output = (Math.ceil(10.1) | 0).toString();",
            "output = (Math.round(10.5) | 0).toString();",
            
            // Use parseInt for Math results
            "output = parseInt(Math.floor(10.7)).toString();",
            
            // Explicit integer operations
            "output = Math.floor(10.7).toFixed(0);",
            
            // Custom integer formatter
            "function toInt(n) { return (n | 0).toString(); } output = toInt(Math.floor(10.7));"
        };
        
        System.out.println("=== BEST PRACTICES FOR INTEGER OUTPUT ===");
        for (String practice : bestPractices) {
            String result = JSEngine.runTagEngine(practice, "");
            System.out.println("Script: " + practice);
            System.out.println("Result: " + result);
            assertFalse(result.contains("."), "Best practice should not produce decimal");
            System.out.println();
        }
    }
}