package httpraider.controller.engines;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests demonstrating that the floating point issue has been fixed in JSEngine.
 * No Burp API dependencies.
 */
public class FloatingPointFixTest {

    @Test
    @DisplayName("Verify integer operations no longer produce decimal points")
    void testIntegerOperationsFixed() {
        // Basic arithmetic
        assertEquals("15", JSEngine.runTagEngine("output = 10 + 5;", ""));
        assertEquals("15", JSEngine.runTagEngine("output = 20 - 5;", ""));
        assertEquals("15", JSEngine.runTagEngine("output = 3 * 5;", ""));
        assertEquals("5", JSEngine.runTagEngine("output = 15 / 3;", ""));
        
        // All should NOT have decimal points
        assertFalse(JSEngine.runTagEngine("output = 10 + 5;", "").contains("."));
        assertFalse(JSEngine.runTagEngine("output = 20 - 5;", "").contains("."));
        assertFalse(JSEngine.runTagEngine("output = 3 * 5;", "").contains("."));
    }

    @Test
    @DisplayName("Verify Math functions return integers without decimals")
    void testMathFunctionsFixed() {
        // Math functions that should return integers
        assertEquals("10", JSEngine.runTagEngine("output = Math.floor(10.7);", ""));
        assertEquals("11", JSEngine.runTagEngine("output = Math.ceil(10.1);", ""));
        assertEquals("11", JSEngine.runTagEngine("output = Math.round(10.5);", ""));
        assertEquals("15", JSEngine.runTagEngine("output = Math.abs(-15);", ""));
        assertEquals("5", JSEngine.runTagEngine("output = Math.min(10, 5, 20);", ""));
        assertEquals("20", JSEngine.runTagEngine("output = Math.max(10, 5, 20);", ""));
        
        // Perfect square roots
        assertEquals("3", JSEngine.runTagEngine("output = Math.sqrt(9);", ""));
        assertEquals("5", JSEngine.runTagEngine("output = Math.sqrt(25);", ""));
        
        // Power operations with integer results
        assertEquals("8", JSEngine.runTagEngine("output = Math.pow(2, 3);", ""));
        assertEquals("100", JSEngine.runTagEngine("output = Math.pow(10, 2);", ""));
    }

    @Test
    @DisplayName("Verify parsing functions return integers correctly")
    void testParsingFunctionsFixed() {
        assertEquals("15", JSEngine.runTagEngine("output = parseInt('15');", ""));
        assertEquals("15", JSEngine.runTagEngine("output = parseInt('15.7');", ""));
        assertEquals("15", JSEngine.runTagEngine("output = parseFloat('15');", ""));
        
        // parseInt should never have decimals
        assertFalse(JSEngine.runTagEngine("output = parseInt('100');", "").contains("."));
        assertFalse(JSEngine.runTagEngine("output = parseInt(15.7);", "").contains("."));
    }

    @Test
    @DisplayName("Verify bitwise operations return integers")
    void testBitwiseOperationsFixed() {
        assertEquals("15", JSEngine.runTagEngine("output = 8 | 7;", ""));
        assertEquals("4", JSEngine.runTagEngine("output = 12 & 5;", ""));
        assertEquals("9", JSEngine.runTagEngine("output = 12 ^ 5;", ""));
        assertEquals("30", JSEngine.runTagEngine("output = 15 << 1;", ""));
        assertEquals("7", JSEngine.runTagEngine("output = 15 >> 1;", ""));
        assertEquals("-16", JSEngine.runTagEngine("output = ~15;", ""));
        
        // None should have decimals
        assertFalse(JSEngine.runTagEngine("output = 255 & 128;", "").contains("."));
        assertFalse(JSEngine.runTagEngine("output = 16 >> 2;", "").contains("."));
    }

    @Test
    @DisplayName("Verify floating point values still work correctly")
    void testFloatingPointStillWorks() {
        // Operations that should return floating point
        assertTrue(JSEngine.runTagEngine("output = 10 / 3;", "").contains("."));
        assertTrue(JSEngine.runTagEngine("output = Math.sqrt(10);", "").contains("."));
        assertTrue(JSEngine.runTagEngine("output = Math.PI;", "").contains("."));
        assertTrue(JSEngine.runTagEngine("output = parseFloat('15.7');", "").contains("."));
        
        // Verify specific values
        assertEquals("3.3333333333333335", JSEngine.runTagEngine("output = 10 / 3;", ""));
        assertEquals("15.7", JSEngine.runTagEngine("output = parseFloat('15.7');", ""));
    }

    @Test
    @DisplayName("Test real-world Content-Length calculation")
    void testContentLengthCalculation() {
        String script = 
            "var body = input;" +
            "var headers = 'Host: example.com\\r\\n'.length;" +
            "output = body.length + headers;";
        
        String result = JSEngine.runTagEngine(script, "Hello World");
        assertEquals("30", result); // 11 (Hello World) + 19 (Host: example.com\r\n)
        assertFalse(result.contains("."), "Content-Length should not have decimal");
    }

    @Test
    @DisplayName("Test chunk size calculations")
    void testChunkSizeCalculation() {
        String script = 
            "var totalSize = parseInt(input);" +
            "var chunkSize = 1024;" +
            "output = Math.ceil(totalSize / chunkSize);";
        
        assertEquals("3", JSEngine.runTagEngine(script, "2500"));
        assertEquals("10", JSEngine.runTagEngine(script, "10000"));
        assertEquals("1", JSEngine.runTagEngine(script, "1000"));
        
        // None should have decimals
        assertFalse(JSEngine.runTagEngine(script, "5000").contains("."));
    }

    @Test
    @DisplayName("Test array length operations")
    void testArrayOperations() {
        String script = "output = input.split(',').length;";
        
        assertEquals("5", JSEngine.runTagEngine(script, "a,b,c,d,e"));
        assertEquals("1", JSEngine.runTagEngine(script, "single"));
        assertEquals("1", JSEngine.runTagEngine(script, "")); // Empty string splits to [""]
        
        // Array length should never have decimals
        assertFalse(JSEngine.runTagEngine(script, "1,2,3,4,5,6,7,8,9,10").contains("."));
    }

    @Test
    @DisplayName("Test increment and decrement operations")
    void testIncrementDecrement() {
        assertEquals("16", JSEngine.runTagEngine("var x = 15; output = ++x;", ""));
        assertEquals("14", JSEngine.runTagEngine("var x = 15; output = --x;", ""));
        assertEquals("15", JSEngine.runTagEngine("var x = 15; output = x++;", ""));
        assertEquals("15", JSEngine.runTagEngine("var x = 15; output = x--;", ""));
        
        // None should have decimals
        assertFalse(JSEngine.runTagEngine("var x = 100; output = ++x;", "").contains("."));
    }

    @Test
    @DisplayName("Test modulo operations")
    void testModuloOperations() {
        assertEquals("2", JSEngine.runTagEngine("output = 17 % 5;", ""));
        assertEquals("0", JSEngine.runTagEngine("output = 20 % 5;", ""));
        assertEquals("2", JSEngine.runTagEngine("output = 100 % 7;", "")); // 100 % 7 = 2
        
        // Modulo should never have decimals
        assertFalse(JSEngine.runTagEngine("output = 1000 % 13;", "").contains("."));
    }

    @Test
    @DisplayName("Test edge cases for integer detection")
    void testEdgeCases() {
        // Very large integers
        assertEquals("1000000", JSEngine.runTagEngine("output = 1000000;", ""));
        assertEquals("999999", JSEngine.runTagEngine("output = 1000000 - 1;", ""));
        
        // Zero
        assertEquals("0", JSEngine.runTagEngine("output = 0;", ""));
        assertEquals("0", JSEngine.runTagEngine("output = 10 - 10;", ""));
        
        // Negative integers
        assertEquals("-15", JSEngine.runTagEngine("output = -15;", ""));
        assertEquals("-100", JSEngine.runTagEngine("output = 50 - 150;", ""));
        
        // None should have decimals
        assertFalse(JSEngine.runTagEngine("output = -1000;", "").contains("."));
        assertFalse(JSEngine.runTagEngine("output = 0;", "").contains("."));
    }

    @Test
    @DisplayName("Test string conversion methods still work")
    void testStringConversions() {
        // These always worked correctly
        assertEquals("15", JSEngine.runTagEngine("output = (10 + 5).toString();", ""));
        assertEquals("15", JSEngine.runTagEngine("output = String(10 + 5);", ""));
        assertEquals("15", JSEngine.runTagEngine("output = '' + (10 + 5);", ""));
        assertEquals("15", JSEngine.runTagEngine("output = `${10 + 5}`;", ""));
        
        // All should be decimal-free
        assertFalse(JSEngine.runTagEngine("output = (100).toString();", "").contains("."));
    }

    @Test
    @DisplayName("Demonstrate the fix works for all problematic operations")
    void testAllProblematicOperationsFixed() {
        // This test documents all operations that were returning decimals before the fix
        String[] operations = {
            "Math.floor(10.7)",
            "Math.ceil(10.1)", 
            "Math.round(10.5)",
            "Math.abs(-15)",
            "Math.min(5, 10)",
            "Math.max(5, 10)",
            "Math.sqrt(9)",
            "Math.pow(2, 3)",
            "parseInt('15')",
            "parseFloat('15')",
            "8 | 7",
            "15 & 7",
            "12 ^ 5",
            "15 << 1",
            "30 >> 1",
            "17 % 5",
            "++x // where x = 14",
            "--x // where x = 16"
        };
        
        // All these should now return integers without decimals
        for (String op : operations) {
            String script = op.contains("//") ? op.split("//")[0].trim() : "output = " + op + ";";
            if (op.contains("++x")) script = "var x = 14; output = ++x;";
            if (op.contains("--x")) script = "var x = 16; output = --x;";
            
            String result = JSEngine.runTagEngine(script, "");
            assertFalse(result.contains("."), 
                "Operation '" + op + "' should not return decimal, but got: " + result);
        }
    }
}