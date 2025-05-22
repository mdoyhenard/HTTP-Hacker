package Utils.Panels;

import org.fife.ui.autocomplete.*;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JSEditorPanel extends JPanel {

    private RSyntaxTextArea textArea;
    private ContextSensitiveCompletionProvider provider;
    private AutoCompletion autoCompletion;

    public JSEditorPanel() {
        super(new BorderLayout());
        initializeComponents();
    }

    public JSEditorPanel(String code) {
        super(new BorderLayout());
        initializeComponents();
        this.textArea.setText(code != null ? code : "");
    }

    private void initializeComponents() {
        // Create RSyntaxTextArea and set its preferred size
        textArea = new RSyntaxTextArea(20, 60);
        // Configure the editor for JavaScript syntax highlighting
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
        textArea.setCodeFoldingEnabled(true);

        // Wrap the RSyntaxTextArea in a scroll pane and add it to this panel
        RTextScrollPane sp = new RTextScrollPane(textArea);
        add(sp, BorderLayout.CENTER);

        // Create a custom context-sensitive completion provider.
        provider = new ContextSensitiveCompletionProvider();
        addJavaScriptKeywords();
        addJavaScriptBuiltInObjects();
        addHttpRequestClassKeywords(); // Add HTTP request class related completions

        // Install auto-completion on the text area
        autoCompletion = new AutoCompletion(provider);
        autoCompletion.install(textArea);
    }

    private void addJavaScriptKeywords() {
        // Add as many JavaScript language keywords as you need for autocomplete.
        String[] keywords = {
                "abstract", "arguments", "await", "boolean", "break", "byte", "case",
                "catch", "char", "class", "const", "continue", "debugger", "default", "delete",
                "do", "double", "else", "enum", "eval", "export", "extends", "false", "final",
                "finally", "float", "for", "function", "goto", "if", "implements", "import", "in",
                "instanceof", "int", "interface", "let", "long", "native", "new", "null", "package",
                "private", "protected", "public", "return", "short", "static", "super", "switch",
                "synchronized", "this", "throw", "throws", "transient", "true", "try", "typeof", "var",
                "void", "volatile", "while", "with", "yield"
        };
        for (String keyword : keywords) {
            provider.addCompletion(new BasicCompletion(provider, keyword));
        }
    }

    private void addJavaScriptBuiltInObjects() {
        // Add JavaScript built-in objects, global functions, and optionally shorthand completions.
        String[] builtins = {
                "Array", "Date", "eval", "function", "hasOwnProperty", "Infinity", "isFinite",
                "isNaN", "isPrototypeOf", "Math", "NaN", "Number", "Object", "prototype", "String",
                "toString", "undefined", "valueOf", "console", "window", "document", "JSON", "RegExp",
                "Error", "Set", "Map", "WeakSet", "WeakMap", "Promise"
        };
        for (String obj : builtins) {
            provider.addCompletion(new BasicCompletion(provider, obj));
        }
    }

    private void addHttpRequestClassKeywords() {
        // Add HTTP request class related keywords as HttpRequestCompletion instances.
        String[] httpKeywords = {
                "HttpRequest",          // The class name
                "getHeaders",           // Returns a shallow copy of the headers
                "addHeader",            // Adds or overwrites a header
                "modifyHeader",         // Alias for addHeader (updates header value)
                "removeHeader",         // Removes a header
                "rawHeaders",           // Raw header block as a string
                "getURL",               // Returns the URL part of the request
                "setURL",               // Sets the URL
                "getQueryString",       // Returns the query string from the URL
                "setQueryString",       // Updates the query string while preserving the base URL
                "getQueryParams",       // Parses and returns the query parameters as an object
                "addQueryParam",        // Adds a new query parameter
                "modifyQueryParam",     // Modifies an existing query parameter
                "removeQueryParam",     // Removes a query parameter
                "getVersion",           // Returns the HTTP version
                "setMethod",            // Sets the HTTP method (e.g., GET, POST)
                "getMethod",            // Gets the current HTTP method
                "getBody",              // Returns the request body
                "setBody",              // Sets or updates the request body
                "convertToChunked",     // Converts the body to chunked encoding
                "convertToContentLength", // Converts a chunked body back to Content-Length encoding
                "toString"              // Rebuilds the complete raw HTTP request as a string
        };
        for (String keyword : httpKeywords) {
            provider.addCompletion(new HttpRequestCompletion(provider, keyword));
        }
    }

    public void setCode(String code) {
        textArea.setText(code);
    }

    public String getCode() {
        return textArea.getText();
    }

    /**
     * A custom completion class to mark completions related to the HttpRequest class.
     */
    public static class HttpRequestCompletion extends BasicCompletion {

        public HttpRequestCompletion(CompletionProvider provider, String replacementText) {
            super(provider, replacementText);
        }
    }

    /**
     * A custom provider that shows HttpRequest completions only when the context matches.
     * If the text right before the caret ends with "req." (i.e. the variable name),
     * then only HTTP request class completions are shown. Otherwise, these completions are hidden.
     */
    public static class ContextSensitiveCompletionProvider extends DefaultCompletionProvider {

        // Pattern to detect an identifier followed by a dot at the caret position.
        private static final Pattern DOT_PATTERN = Pattern.compile("([a-zA-Z0-9_$]+)\\.$");

        @Override
        public List<Completion> getCompletions(JTextComponent comp) {

            // Get the default completions from the super provider.
            List<Completion> allCompletions = super.getCompletions(comp);

            // Find the text before the caret to check context.
            int pos = comp.getCaretPosition();
            String text = "";
            try {
                text = comp.getText(0, pos);
            } catch (Exception e) {
                e.printStackTrace();
            }

            boolean isHttpRequestContext = false;
            Matcher m = DOT_PATTERN.matcher(text);
            if (m.find()) {
                String objectName = m.group(1);
                // Here, we assume the HttpRequest instance is referenced by "req".
                if ("httpRequest".equals(objectName)) {
                    isHttpRequestContext = true;
                }
            }

            // Now, filter completions:
            // - If the context is HttpRequest (e.g., "req."), include only HttpRequest completions.
            // - Otherwise, filter out HttpRequest completions.
            List<Completion> filtered = new ArrayList<>();
            for (Completion c : allCompletions) {
                if (c instanceof HttpRequestCompletion) {
                    if (isHttpRequestContext) {
                        filtered.add(c);
                    }
                } else {
                    filtered.add(c);
                }
            }
            return filtered;
        }
    }
}
