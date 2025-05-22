package Utils;

import RawRepeater.MsgLenHeader;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpParser {

    private String headersEnd;
    private List<MsgLenHeader> messageLengths;
    private String parserCode;
    private String forwardingCode;

    public HttpParser(){
        this.headersEnd = "";
        this.messageLengths = new ArrayList<>();
    }

    public HttpParser(HttpParser parser){
        this.headersEnd = parser.headersEnd;
        this.messageLengths = new ArrayList<>(parser.messageLengths);
        this.parserCode = parser.parserCode;
        this.forwardingCode = parser.forwardingCode;
    }

    public void clone(HttpParser parser){
        this.headersEnd = parser.headersEnd;
        this.messageLengths = new ArrayList<>(parser.messageLengths);
        this.parserCode = parser.parserCode;
        this.forwardingCode = parser.forwardingCode;
    }

    public String getHeadersEnd() {
        return headersEnd;
    }

    public void setHeadersEnd(String headersEnd) {
        this.headersEnd = headersEnd;
    }

    public List<MsgLenHeader> getMessageLengths() {
        return messageLengths;
    }

    public void setMessageLengths(List<MsgLenHeader> messageLengths) {
        this.messageLengths = messageLengths;
    }

    public void addMessageLengths(MsgLenHeader messageLengths) {
        this.messageLengths.add(messageLengths);
    }

    public String getParserCode() {
        return parserCode;
    }

    public void setParserCode(String parserCode) {
        this.parserCode = parserCode;
    }

    public String getForwardingCode() {
        return forwardingCode;
    }

    public void setForwardingCode(String forwardingCode) {
        this.forwardingCode = forwardingCode;
    }

    public static int parseTEbody(String body){

        int pos = 0;
        int len = body.length();

        while (true) {
            // Buscar el CRLF que termina la línea del tamaño del chunk.
            int crlfIndex = body.indexOf("\r\n", pos);
            if (crlfIndex == -1) {
                // No se encontró CRLF: error en el parseo.
                return -1;
            }

            // Extraer la línea del chunk-size (puede incluir extensiones).
            String chunkSizeLine = body.substring(pos, crlfIndex);

            // Si existe ';' se ignoran las extensiones: se toma solo la parte anterior.
            int semicolonIndex = chunkSizeLine.indexOf(';');
            String chunkSizeStr = (semicolonIndex != -1)
                    ? chunkSizeLine.substring(0, semicolonIndex)
                    : chunkSizeLine;
            chunkSizeStr = chunkSizeStr.trim();

            int chunkSize;
            try {
                // Convertir el tamaño (en hexadecimal) a entero.
                chunkSize = Integer.parseInt(chunkSizeStr, 16);
            } catch (NumberFormatException e) {
                return -1;
            }

            // Avanzar la posición después de la línea del chunk-size (saltamos el CRLF).
            pos = crlfIndex + 2;

            if (chunkSize < 0) {
                return -1;
            }

            if (chunkSize > 0) {
                // Para un chunk con tamaño > 0, se debe tener al menos 'chunkSize' caracteres
                // correspondientes a los datos del chunk, y luego un CRLF.
                if (pos + chunkSize > len) {
                    return -1;
                }
                pos += chunkSize;
                // Verificar la existencia del CRLF que sigue a los datos.
                if (pos + 1 >= len || !body.substring(pos, pos + 2).equals("\r\n")) {
                    return -1;
                }
                pos += 2; // Saltamos el CRLF tras el chunk.
            } else {
                // Cuando chunkSize es 0: es el último chunk.
                // Después se pueden incluir líneas de trailer que finalizan al encontrar una línea vacía (CRLF).
                while (true) {
                    int trailerCRLF = body.indexOf("\r\n", pos);
                    if (trailerCRLF == -1) {
                        return -1;
                    }
                    // Línea vacía indica el final del trailer.
                    if (trailerCRLF == pos) {
                        pos += 2; // Saltamos la línea vacía.
                        break;
                    }
                    // Saltamos la línea del header del trailer.
                    pos = trailerCRLF + 2;
                }
                // Retornamos la posición en la que finaliza el body.
                return pos;
            }
        }
    }

    public static int parseCLheader(String input, MsgLenHeader clHeader) {
        // Definimos el literal placeholder que indica dónde está el entero.
        final String placeholder = clHeader.getPlaceHolder();
        String searchPattern = clHeader.getEscapedPattern();
        int posPlaceholder = searchPattern.indexOf(placeholder);
        if (posPlaceholder == -1) {
            // No se encontró el placeholder, se asume error.
            return -1;
        }
        // Separa en prefijo y sufijo
        String prefix = searchPattern.substring(0, posPlaceholder);
        String suffix = searchPattern.substring(posPlaceholder + placeholder.length());

        // Construimos la expresión regular:
        // 1. Escapamos el prefijo y sufijo para que se traten como literales.
        // 2. Insertamos un grupo de captura para uno o más dígitos en lugar del placeholder.
        String regex = Pattern.quote(prefix) + "(\\d+)" + Pattern.quote(suffix);
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        int foundNumber = -1;
        boolean found = false;
        // Recorremos todas las ocurrencias
        while (matcher.find()) {
            // El grupo 1 corresponde a la secuencia de dígitos
            String numStr = matcher.group(1);
            try {
                int number = Integer.parseInt(numStr);
                if (!clHeader.isUseLast()) {
                    return number;
                } else {
                    foundNumber = number;
                    found = true;
                }
            } catch (NumberFormatException e) {
            }
        }
        return found ? foundNumber : -1;
    }
}
