package httpraider.controller.engines;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.util.List;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class JSEngineTest {

    @Test
    @DisplayName("Test runTagEngine with simple string transformation")
    void testRunTagEngineSimpleTransform() {
        String script = "output = input.toUpperCase();";
        String input = "hello world";
        String result = JSEngine.runTagEngine(script, input);
        assertEquals("HELLO WORLD", result);
    }

    @Test
    @DisplayName("Test runTagEngine with number operations - integer result")
    void testRunTagEngineIntegerResult() {
        String script = "output = (parseInt(input) * 2).toString();";
        String input = "5";
        String result = JSEngine.runTagEngine(script, input);
        assertEquals("10", result);
    }

    @Test
    @DisplayName("Test runTagEngine with addition - verify no floating point")
    void testRunTagEngineAdditionNoFloat() {
        String script = 
            "var nums = input.split(',');" +
            "var sum = 0;" +
            "for (var i = 0; i < nums.length; i++) {" +
            "  sum += parseInt(nums[i]);" +
            "}" +
            "output = sum.toString();";
        String input = "10,20,30";
        String result = JSEngine.runTagEngine(script, input);
        assertEquals("60", result);
        assertFalse(result.contains("."), "Integer addition should not produce decimal");
    }

    @Test
    @DisplayName("Test runTagEngine with division - floating point result")
    void testRunTagEngineDivisionFloat() {
        String script = "output = (parseFloat(input) / 3).toString();";
        String input = "10";
        String result = JSEngine.runTagEngine(script, input);
        assertTrue(result.startsWith("3.333"), "Division should produce floating point");
    }

    @Test
    @DisplayName("Test runTagEngine with null output")
    void testRunTagEngineNullOutput() {
        String script = "// No output set";
        String input = "test";
        String result = JSEngine.runTagEngine(script, input);
        assertEquals("", result);
    }

    @Test
    @DisplayName("Test runTagEngine with empty script")
    void testRunTagEngineEmptyScript() {
        String script = "";
        String input = "test";
        String result = JSEngine.runTagEngine(script, input);
        assertEquals("", result);
    }

    @Test
    @DisplayName("Test runTagEngine with null script")
    void testRunTagEngineNullScript() {
        String script = null;
        String input = "test";
        String result = JSEngine.runTagEngine(script, input);
        assertEquals("", result);
    }

    @Test
    @DisplayName("Test runTagEngine with script error")
    void testRunTagEngineScriptError() {
        String script = "output = nonExistentFunction();";
        String input = "test";
        String result = JSEngine.runTagEngine(script, input);
        assertEquals("", result); // Should return empty on error
    }

    @Test
    @DisplayName("Test runEndHeaderScript")
    void testRunEndHeaderScript() {
        String script = 
            "outHeaders = headers.toUpperCase();" +
            "outBuffer = buffer + ' MODIFIED';";
        byte[] headers = "content-type: text/plain".getBytes();
        byte[] buffer = "body content".getBytes();
        
        byte[][] result = JSEngine.runEndHeaderScript(headers, buffer, script);
        
        assertEquals("CONTENT-TYPE: TEXT/PLAIN", new String(result[0]));
        assertEquals("body content MODIFIED", new String(result[1]));
    }

    @Test
    @DisplayName("Test runEndHeaderScript with empty script")
    void testRunEndHeaderScriptEmptyScript() {
        byte[] headers = "test".getBytes();
        byte[] buffer = "body".getBytes();
        
        byte[][] result = JSEngine.runEndHeaderScript(headers, buffer, "");
        
        assertArrayEquals(headers, result[0]);
        assertArrayEquals(buffer, result[1]);
    }

    @Test
    @DisplayName("Test runSplitHeaderScript")
    void testRunSplitHeaderScript() {
        String script = 
            "outHeaderLines = [];" +
            "for (var i = 0; i < headerLines.length; i++) {" +
            "  outHeaderLines.push(headerLines[i].toUpperCase());" +
            "}";
        List<String> headerLines = Arrays.asList("Host: example.com", "User-Agent: test");
        byte[] headers = "Host: example.com\r\nUser-Agent: test".getBytes();
        
        List<String> result = JSEngine.runSplitHeaderScript(headerLines, headers, script);
        
        assertEquals(2, result.size());
        assertEquals("HOST: EXAMPLE.COM", result.get(0));
        assertEquals("USER-AGENT: TEST", result.get(1));
    }

    @Test
    @DisplayName("Test runSplitHeaderScript with script error")
    void testRunSplitHeaderScriptError() {
        List<String> headerLines = Arrays.asList("test1", "test2");
        byte[] headers = "test".getBytes();
        
        List<String> result = JSEngine.runSplitHeaderScript(headerLines, headers, "invalid script");
        
        assertEquals(headerLines, result); // Should return original on error
    }

    @Test
    @DisplayName("Test runBodyLenScript")
    void testRunBodyLenScript() {
        String script = 
            "outBody = body + ' MODIFIED';" +
            "outBuffer = buffer + ' BUFFERED';";
        List<String> headerLines = Arrays.asList("Content-Length: 10");
        byte[] body = "test body".getBytes();
        byte[] buffer = "buffer".getBytes();
        
        byte[][] result = JSEngine.runBodyLenScript(headerLines, body, buffer, script);
        
        assertEquals("test body MODIFIED", new String(result[0]));
        assertEquals("buffer BUFFERED", new String(result[1]));
    }

    @Test
    @DisplayName("Test runJsBooleanRule")
    void testRunJsBooleanRule() {
        String script = "result = headers.indexOf('Host') > -1;";
        String headers = "Host: example.com\r\nUser-Agent: test";
        String body = "body content";
        
        Object result = JSEngine.runJsBooleanRule(script, headers, body);
        
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("Test runJsBooleanRule with output variable")
    void testRunJsBooleanRuleOutput() {
        String script = "output = body.length > 10;";
        String headers = "Host: example.com";
        String body = "short";
        
        Object result = JSEngine.runJsBooleanRule(script, headers, body);
        
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("Test runFirewallRule - true result")
    void testRunFirewallRuleTrue() {
        String script = "return input.indexOf('malicious') > -1;";
        String input = "This is a malicious request";
        
        boolean result = JSEngine.runFirewallRule(script, input);
        
        assertTrue(result);
    }

    @Test
    @DisplayName("Test runFirewallRule - false result")
    void testRunFirewallRuleFalse() {
        String script = "return input.indexOf('malicious') > -1;";
        String input = "This is a normal request";
        
        boolean result = JSEngine.runFirewallRule(script, input);
        
        assertFalse(result);
    }

    @Test
    @DisplayName("Test runFirewallRule with script error")
    void testRunFirewallRuleError() {
        String script = "throw new Error('test error');";
        String input = "test";
        
        boolean result = JSEngine.runFirewallRule(script, input);
        
        assertFalse(result); // Should return false on error
    }

    @Test
    @DisplayName("Test runFirewallRuleArray")
    void testRunFirewallRuleArray() {
        String script = 
            "for (var i = 0; i < headers.length; i++) {" +
            "  if (headers[i].indexOf('Evil-Header') > -1) return true;" +
            "}" +
            "return false;";
        String[] headers = {"Host: example.com", "Evil-Header: bad", "User-Agent: test"};
        
        boolean result = JSEngine.runFirewallRuleArray(script, headers);
        
        assertTrue(result);
    }

    @Test
    @DisplayName("Test Math operations return integers when appropriate")
    void testMathOperationsReturnIntegers() {
        // Test Math.floor
        String script1 = "output = Math.floor(10.7).toString();";
        assertEquals("10", JSEngine.runTagEngine(script1, ""));
        
        // Test Math.ceil
        String script2 = "output = Math.ceil(10.1).toString();";
        assertEquals("11", JSEngine.runTagEngine(script2, ""));
        
        // Test Math.round
        String script3 = "output = Math.round(10.5).toString();";
        assertEquals("11", JSEngine.runTagEngine(script3, ""));
    }

    @Test
    @DisplayName("Test number type coercion")
    void testNumberTypeCoercion() {
        // Test that JavaScript Number type is properly handled
        String script = 
            "var a = 5;" +
            "var b = 10;" +
            "var sum = a + b;" +
            "output = sum.toString();";
        String result = JSEngine.runTagEngine(script, "");
        assertEquals("15", result);
        assertFalse(result.contains("."), "Integer operations should not add decimals");
    }
}