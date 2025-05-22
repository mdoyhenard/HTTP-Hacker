package Utils;

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.requests.HttpRequest;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.nio.charset.StandardCharsets;

import static java.text.MessageFormat.format;

public class JSEngine {

    private static final String REQUEST_BUILDER = "// HttpRequest class definition\n" +
            "class HttpRequest {\n" +
            "  /**\n" +
            "   * Construct an HttpRequest instance.\n" +
            "   * @param {string} firstLine - The request line (e.g. \"GET /path?query=123 HTTP/1.1\")\n" +
            "   * @param {string} headersStr - The block of headers (with \"\\r\\n\" line endings, ending with an extra \"\\r\\n\")\n" +
            "   * @param {string} bodyStr - The body of the request\n" +
            "   */\n" +
            "  constructor(firstLine, headersStr, bodyStr) {\n" +
            "    // Parse first line into method, URL, and version.\n" +
            "    const parts = firstLine.split(\" \");\n" +
            "    if (parts.length < 3) {\n" +
            "      throw new Error(\"Invalid request line. Format should be: METHOD <URL> HTTP/<version>\");\n" +
            "    }\n" +
            "    this.method = parts[0];\n" +
            "    this.url = parts[1];\n" +
            "    this.version = parts[2];\n" +
            "\n" +
            "    // Store the body.\n" +
            "    this.body = bodyStr;\n" +
            "\n" +
            "    // Parse headers (assumes header lines separated by CRLF).\n" +
            "    this.headers = {};\n" +
            "    const headerLines = headersStr.split(/\\r\\n/).filter(line => line.trim().length > 0);\n" +
            "    for (let line of headerLines) {\n" +
            "      const index = line.indexOf(':');\n" +
            "      if (index > -1) {\n" +
            "        const key = line.substring(0, index).trim();\n" +
            "        const value = line.substring(index + 1).trim();\n" +
            "        this.headers[key] = value;\n" +
            "      }\n" +
            "    }\n" +
            "    // Save the original raw headers.\n" +
            "    this.rawHeaders = headersStr;\n" +
            "\n" +
            "    // Use the Host header as the domain if it exists.\n" +
            "    this.domain = this.headers[\"Host\"] || \"\";\n" +
            "  }\n" +
            "\n" +
            "  // -------------------------------\n" +
            "  // Header Methods\n" +
            "  // -------------------------------\n" +
            "  /** Returns a shallow copy of the headers dictionary. */\n" +
            "  getHeaders() {\n" +
            "    return Object.assign({}, this.headers);\n" +
            "  }\n" +
            "\n" +
            "  /** Adds a new header (or overwrites an existing one). */\n" +
            "  addHeader(key, value) {\n" +
            "    this.headers[key] = value;\n" +
            "    this._updateRawHeaders();\n" +
            "  }\n" +
            "\n" +
            "  /** Modifies the header value (alias to addHeader). */\n" +
            "  modifyHeader(key, value) {\n" +
            "    this.headers[key] = value;\n" +
            "    this._updateRawHeaders();\n" +
            "  }\n" +
            "\n" +
            "  /** Removes a header by key. */\n" +
            "  removeHeader(key) {\n" +
            "    delete this.headers[key];\n" +
            "    this._updateRawHeaders();\n" +
            "  }\n" +
            "\n" +
            "  // Private helper to rebuild the raw headers string based on current headers.\n" +
            "  _updateRawHeaders() {\n" +
            "    let headersStr = \"\";\n" +
            "    for (let key in this.headers) {\n" +
            "      headersStr += `${key}: ${this.headers[key]}\\r\\n`;\n" +
            "    }\n" +
            "    headersStr += \"\\r\\n\"; // terminating CRLF sequence for header block\n" +
            "    this.rawHeaders = headersStr;\n" +
            "  }\n" +
            "\n" +
            "  // -------------------------------\n" +
            "  // URL and Query String Methods\n" +
            "  // -------------------------------\n" +
            "  /** Returns the current URL (the part after the domain). */\n" +
            "  getURL() {\n" +
            "    return this.url;\n" +
            "  }\n" +
            "\n" +
            "  /** Sets the URL string. */\n" +
            "  setURL(newUrl) {\n" +
            "    this.url = newUrl;\n" +
            "  }\n" +
            "\n" +
            "  /** Returns the query string part of the URL (if present). */\n" +
            "  getQueryString() {\n" +
            "    const queryIndex = this.url.indexOf(\"?\");\n" +
            "    return queryIndex >= 0 ? this.url.substring(queryIndex + 1) : \"\";\n" +
            "  }\n" +
            "\n" +
            "  /** Replaces the query string while preserving the base URL. */\n" +
            "  setQueryString(newQuery) {\n" +
            "    const basePath = this.url.split('?')[0];\n" +
            "    this.url = newQuery && newQuery.length > 0 ? `${basePath}?${newQuery}` : basePath;\n" +
            "  }\n" +
            "\n" +
            "  /** Parses and returns the query parameters as an object. */\n" +
            "  getQueryParams() {\n" +
            "    return this._parseQueryParams();\n" +
            "  }\n" +
            "\n" +
            "  /** Adds or modifies a query parameter and updates the URL accordingly. */\n" +
            "  addQueryParam(key, value) {\n" +
            "    const params = this._parseQueryParams();\n" +
            "    params[key] = value;\n" +
            "    const newQuery = this._buildQueryString(params);\n" +
            "    this.setQueryString(newQuery);\n" +
            "  }\n" +
            "\n" +
            "  /** Alias for addQueryParam; modifies a parameter value. */\n" +
            "  modifyQueryParam(key, value) {\n" +
            "    this.addQueryParam(key, value);\n" +
            "  }\n" +
            "\n" +
            "  /** Removes a query parameter from the URL. */\n" +
            "  removeQueryParam(key) {\n" +
            "    const params = this._parseQueryParams();\n" +
            "    delete params[key];\n" +
            "    const newQuery = this._buildQueryString(params);\n" +
            "    this.setQueryString(newQuery);\n" +
            "  }\n" +
            "\n" +
            "  // Private helper: parses the query string into an object.\n" +
            "  _parseQueryParams() {\n" +
            "    const query = this.getQueryString();\n" +
            "    const params = {};\n" +
            "    if (query) {\n" +
            "      const pairs = query.split(\"&\");\n" +
            "      for (let pair of pairs) {\n" +
            "        if (!pair) continue;\n" +
            "        const parts = pair.split(\"=\");\n" +
            "        const key = decodeURIComponent(parts[0]);\n" +
            "        const value = parts[1] ? decodeURIComponent(parts[1]) : \"\";\n" +
            "        params[key] = value;\n" +
            "      }\n" +
            "    }\n" +
            "    return params;\n" +
            "  }\n" +
            "\n" +
            "  // Private helper: builds a query string from an object.\n" +
            "  _buildQueryString(params) {\n" +
            "    const pairs = [];\n" +
            "    for (let key in params) {\n" +
            "      pairs.push(`${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`);\n" +
            "    }\n" +
            "    return pairs.join(\"&\");\n" +
            "  }\n" +
            "\n" +
            "  // -------------------------------\n" +
            "  // Version, Method, and Body Methods\n" +
            "  // -------------------------------\n" +
            "  /** Returns the HTTP version. */\n" +
            "  getVersion() {\n" +
            "    return this.version;\n" +
            "  }\n" +
            "\n" +
            "  /** Sets the HTTP method (e.g., GET, POST). */\n" +
            "  setMethod(newMethod) {\n" +
            "    this.method = newMethod;\n" +
            "  }\n" +
            "\n" +
            "  /** Returns the current HTTP method. */\n" +
            "  getMethod() {\n" +
            "    return this.method;\n" +
            "  }\n" +
            "\n" +
            "  /** Returns the body of the request. */\n" +
            "  getBody() {\n" +
            "    return this.body;\n" +
            "  }\n" +
            "\n" +
            "  /** Sets/updates the body of the request. */\n" +
            "  setBody(newBody) {\n" +
            "    this.body = newBody;\n" +
            "  }\n" +
            "\n" +
            "  // -------------------------------\n" +
            "  // Conversion Methods: Content-Length <-> Chunked\n" +
            "  // -------------------------------\n" +
            "  /**\n" +
            "   * Converts the request body to chunked encoding.\n" +
            "   * Removes any existing Content-Length header, sets Transfer-Encoding to \"chunked\",\n" +
            "   * and transforms the body into a single chunk followed by the terminating empty chunk.\n" +
            "   */\n" +
            "  convertToChunked() {\n" +
            "    // Remove Content-Length header if it exists.\n" +
            "    if (this.headers[\"Content-Length\"]) {\n" +
            "      delete this.headers[\"Content-Length\"];\n" +
            "    }\n" +
            "    // Set Transfer-Encoding to \"chunked\".\n" +
            "    this.headers[\"Transfer-Encoding\"] = \"chunked\";\n" +
            "\n" +
            "    // Convert the entire body as one chunk.\n" +
            "    const chunkLengthHex = this.body.length.toString(16);\n" +
            "    const chunkedBody = `${chunkLengthHex}\\r\\n${this.body}\\r\\n0\\r\\n\\r\\n`;\n" +
            "    this.body = chunkedBody;\n" +
            "    this._updateRawHeaders();\n" +
            "  }\n" +
            "\n" +
            "  /**\n" +
            "   * Converts a chunked-encoded body back to a normal body with Content-Length.\n" +
            "   * Removes the Transfer-Encoding header and adds a Content-Length header with the decoded length.\n" +
            "   */\n" +
            "  convertToContentLength() {\n" +
            "    // Remove Transfer-Encoding if present.\n" +
            "    if (this.headers[\"Transfer-Encoding\"]) {\n" +
            "      delete this.headers[\"Transfer-Encoding\"];\n" +
            "    }\n" +
            "    // Decode the chunked body.\n" +
            "    let body = this.body;\n" +
            "    let unchunkedBody = \"\";\n" +
            "    let pos = 0;\n" +
            "    while (pos < body.length) {\n" +
            "      const nextCrLf = body.indexOf(\"\\r\\n\", pos);\n" +
            "      if (nextCrLf === -1) break;\n" +
            "      const chunkSizeHex = body.substring(pos, nextCrLf);\n" +
            "      const chunkSize = parseInt(chunkSizeHex, 16);\n" +
            "      if (isNaN(chunkSize) || chunkSize < 0) break;\n" +
            "      pos = nextCrLf + 2; // Move after CRLF.\n" +
            "      if (chunkSize === 0) break; // End of chunks.\n" +
            "      const chunkData = body.substring(pos, pos + chunkSize);\n" +
            "      unchunkedBody += chunkData;\n" +
            "      pos += chunkSize + 2; // Skip chunk data and following CRLF.\n" +
            "    }\n" +
            "    this.body = unchunkedBody;\n" +
            "    // Add Content-Length header.\n" +
            "    this.headers[\"Content-Length\"] = unchunkedBody.length.toString();\n" +
            "    this._updateRawHeaders();\n" +
            "  }\n" +
            "\n" +
            "  // -------------------------------\n" +
            "  // Utility: Rebuild the Raw Request String\n" +
            "  // -------------------------------\n" +
            "  /**\n" +
            "   * Rebuilds the complete HTTP request as a string.\n" +
            "   * Combines the request line, headers, and body.\n" +
            "   */\n" +
            "  toString() {\n" +
            "    const requestLine = `${this.method} ${this.url} ${this.version}`;\n" +
            "    return requestLine + \"\\r\\n\" + this.rawHeaders + this.body;\n" +
            "  }\n" +
            "}\n" +
            "\n" +
            "const httpRequest = new HttpRequest('{0}', '{1}', '{2}');\n" +
            "{3};\n" +
            "httpRequest.request;";

    private final ScriptEngine engine;

    public JSEngine() {
        // Create a ScriptEngineManager and get the JavaScript engine
        ScriptEngineManager manager = new ScriptEngineManager();
        engine = manager.getEngineByName("JavaScript");
        if (engine == null) {
            throw new RuntimeException("JavaScript engine not found. Please ensure your JDK supports it or consider using an alternative.");
        }
    }

    public HttpRequest execute(HttpParser parser, HttpRequest request) {
        try {
            String result = engine.eval(format(REQUEST_BUILDER, request.toString().split("\r\n")[0].replace("'", "\\'"), request.headers().toString().replace("'", "\\'"), request.bodyToString().replace("'", "\\'"), parser.getParserCode())).toString();
            return HttpRequest.httpRequest(ByteArray.byteArray(result.getBytes(StandardCharsets.UTF_8))).withService(request.httpService());
        } catch (Exception e){
            return null;
        }
    }

}
