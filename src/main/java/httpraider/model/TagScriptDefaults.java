package httpraider.model;

public class TagScriptDefaults {
    
    public static String getDefaultCustomTagScript() {
        return """
// Input: input (string) - the content between <start_N> and <end_N>
// Output: output (string) - the processed result

// ===== BASE FUNCTIONS =====

function toHex(str) {
    var result = '';
    for (var i = 0; i < str.length; i++) {
        var hex = str.charCodeAt(i).toString(16);
        result += (hex.length === 1 ? '0' : '') + hex;
    }
    return result;
}

function fromHex(hex) {
    var result = '';
    for (var i = 0; i < hex.length; i += 2) {
        result += String.fromCharCode(parseInt(hex.substr(i, 2), 16));
    }
    return result;
}

function toBase64(str) {
    var chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';
    var result = '';
    var i = 0;
    
    while (i < str.length) {
        var a = str.charCodeAt(i++);
        var b = i < str.length ? str.charCodeAt(i++) : 0;
        var c = i < str.length ? str.charCodeAt(i++) : 0;
        
        var bitmap = (a << 16) | (b << 8) | c;
        result += chars.charAt((bitmap >> 18) & 63);
        result += chars.charAt((bitmap >> 12) & 63);
        result += i - 2 < str.length ? chars.charAt((bitmap >> 6) & 63) : '=';
        result += i - 1 < str.length ? chars.charAt(bitmap & 63) : '=';
    }
    
    return result;
}

function toUrlEncode(str) {
    return str.replace(/[^a-zA-Z0-9-_.~]/g, function(match) {
        return '%' + match.charCodeAt(0).toString(16).toUpperCase();
    });
}

function xor(str, key) {
    var result = '';
    for (var i = 0; i < str.length; i++) {
        result += String.fromCharCode(str.charCodeAt(i) ^ key.charCodeAt(i % key.length));
    }
    return result;
}

function reverse(str) {
    return str.split('').reverse().join('');
}

function pad(str, length, padChar) {
    padChar = padChar || ' ';
    while (str.length < length) {
        str += padChar;
    }
    return str;
}

function padLeft(str, length, padChar) {
    padChar = padChar || ' ';
    while (str.length < length) {
        str = padChar + str;
    }
    return str;
}

// ===== TAG PROCESSING =====

// Example: Convert input to hex
output = toHex(input);

// Other examples:
// output = toBase64(input);
// output = toUrlEncode(input);
// output = reverse(input);
// output = xor(input, 'key');
// output = pad(input, 20, '0');
""";
    }
    
    public static String getBaseJsFunctions() {
        return """
// ===== UTILITY FUNCTIONS =====

function findHeader(lines, name) {
    var lowerName = name.toLowerCase();
    for (var i = 0; i < lines.length; i++) {
        if (lines[i].toLowerCase().indexOf(lowerName + ':') === 0) {
            return i;
        }
    }
    return -1;
}

function getHeaderValue(lines, name) {
    var index = findHeader(lines, name);
    if (index === -1) return null;
    var colonPos = lines[index].indexOf(':');
    return lines[index].substring(colonPos + 1).trim();
}

function setHeader(lines, name, value) {
    var index = findHeader(lines, name);
    if (index !== -1) {
        lines[index] = name + ': ' + value;
    } else {
        lines.push(name + ': ' + value);
    }
}

function removeHeader(lines, name) {
    var index = findHeader(lines, name);
    if (index !== -1) {
        lines.splice(index, 1);
    }
}

function toHex(str) {
    var result = '';
    for (var i = 0; i < str.length; i++) {
        var hex = str.charCodeAt(i).toString(16);
        result += (hex.length === 1 ? '0' : '') + hex;
    }
    return result;
}

function fromHex(hex) {
    var result = '';
    for (var i = 0; i < hex.length; i += 2) {
        result += String.fromCharCode(parseInt(hex.substr(i, 2), 16));
    }
    return result;
}

function toBase64(str) {
    var chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';
    var result = '';
    var i = 0;
    
    while (i < str.length) {
        var a = str.charCodeAt(i++);
        var b = i < str.length ? str.charCodeAt(i++) : 0;
        var c = i < str.length ? str.charCodeAt(i++) : 0;
        
        var bitmap = (a << 16) | (b << 8) | c;
        result += chars.charAt((bitmap >> 18) & 63);
        result += chars.charAt((bitmap >> 12) & 63);
        result += i - 2 < str.length ? chars.charAt((bitmap >> 6) & 63) : '=';
        result += i - 1 < str.length ? chars.charAt(bitmap & 63) : '=';
    }
    
    return result;
}
""";
    }
}