# JavaScript Number to String Conversion Issue Report

## Summary

The Tag processor in HTTP-Raider returns floating-point representations (e.g., `15.0`) for integer values in many cases when JavaScript code is executed through the JSEngine. This occurs because JavaScript's Number type is always a double-precision floating-point, and when these values are converted to strings in Java, they retain the decimal notation.

## Test Results

### Operations That Unexpectedly Return Decimals

The following JavaScript operations return decimal notation even for integer results:

1. **Math Functions**
   - `Math.floor(10.7)` → `10.0` (expected: `10`)
   - `Math.ceil(10.1)` → `11.0` (expected: `11`)
   - `Math.round(10.5)` → `11.0` (expected: `11`)
   - `Math.abs(-15)` → `15.0` (expected: `15`)
   - `Math.min(10, 15, 5)` → `5.0` (expected: `5`)
   - `Math.max(10, 15, 5)` → `15.0` (expected: `15`)
   - `Math.sqrt(9)` → `3.0` (expected: `3`)
   - `Math.pow(2, 3)` → `8.0` (expected: `8`)

2. **Parsing Functions**
   - `parseInt('15')` → `15.0` (expected: `15`)
   - `parseFloat('15')` → `15.0` (expected: `15`)

3. **Bitwise Operations** (surprisingly!)
   - `8 | 7` → `15.0` (expected: `15`)
   - `15 & 7` → `7.0` (expected: `7`)
   - `12 ^ 7` → `11.0` (expected: `11`)
   - `15 << 1` → `30.0` (expected: `30`)
   - `30 >> 1` → `15.0` (expected: `15`)

4. **Other Operations**
   - `17 % 5` → `2.0` (expected: `2`)
   - `++x` (where x=14) → `15.0` (expected: `15`)
   - `--x` (where x=16) → `15.0` (expected: `15`)

### Operations That Work Correctly

The following operations correctly return integers without decimals:

1. **Basic Arithmetic** (when result is assigned directly)
   - `10 + 5` → `15`
   - `20 - 5` → `15`
   - `3 * 5` → `15`
   - `15 / 3` → `5`

2. **String Conversion Methods**
   - `(10 + 5).toString()` → `15`
   - `'' + (10 + 5)` → `15`
   - Template literals → `15`

3. **Special Case**
   - `~-16` → `15` (bitwise NOT seems to work correctly)

## Root Cause

The issue occurs in `JSEngine.runTagEngine()` at line 54:
```java
return result == null ? "" : result.toString();
```

When the JavaScript engine returns a Number, it's stored as a Java Double. The `toString()` method on Double always includes decimal notation, even for whole numbers.

## Solutions

### 1. Fix in JSEngine (Recommended)

Modify `JSEngine.runTagEngine()` to check if a Number is actually an integer value:

```java
public static String runTagEngine(String script, String input) {
    // ... existing code ...
    
    Object result = out.get("output");
    if (result == null) return "";
    
    // Check if Number is actually an integer
    if (result instanceof Number) {
        Number num = (Number) result;
        double d = num.doubleValue();
        if (d == Math.floor(d) && !Double.isInfinite(d)) {
            return String.valueOf(num.longValue());
        }
    }
    
    return result.toString();
}
```

### 2. JavaScript Workarounds

Until the engine is fixed, use these patterns in custom tags:

```javascript
// Method 1: Explicit toString()
output = (Math.floor(10.7)).toString();

// Method 2: Bitwise OR to force integer (then toString)
output = (Math.floor(10.7) | 0).toString();

// Method 3: Use parseInt
output = parseInt(Math.floor(10.7)).toString();

// Method 4: Use toFixed(0)
output = Math.floor(10.7).toFixed(0);

// Method 5: Custom formatter function
function formatInt(n) {
    return (n % 1 === 0) ? n.toFixed(0) : n.toString();
}
output = formatInt(Math.floor(10.7));
```

### 3. Best Practices for Tag Scripts

1. **For Content-Length calculations:**
   ```javascript
   output = input.length.toString(); // Works correctly
   ```

2. **For Math operations that should return integers:**
   ```javascript
   // Instead of:
   output = Math.ceil(data.length / 1024);
   
   // Use:
   output = (Math.ceil(data.length / 1024) | 0).toString();
   ```

3. **For array operations:**
   ```javascript
   var items = input.split(',');
   output = items.length.toString(); // Already works correctly
   ```

## Impact

This issue affects:
- HTTP Content-Length headers when calculated with Math functions
- Chunk size calculations in chunked transfer encoding
- Any custom tag that performs mathematical operations
- Parser rules that expect integer values

## Testing

Run the comprehensive test suite with:
```bash
./gradlew test --tests ComprehensiveFloatingPointTest
```

This will show all affected operations and verify any fixes.