package Utils;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.HttpMode;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.RequestOptions;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;

import java.nio.charset.StandardCharsets;

public class Utils {



    public static HttpRequestResponse sendHTTPRequest(MontoyaApi api, HttpRequest req, int version){
        HttpRequestResponse reqResp;
        String from = version == 1 ? "RawRepeater/HTTP/2" : "RawRepeater/HTTP/1.1";
        String to = version == 1 ? "RawRepeater/HTTP/1.1" : "RawRepeater/HTTP/2";
        HttpMode mode = version == 1 ? HttpMode.HTTP_1 : HttpMode.HTTP_2;

        byte[] requestBytes = req.toByteArray().getBytes();
        String requestString = new String(requestBytes, StandardCharsets.UTF_8);
        requestString = requestString.replace(from, to);
        byte[] modifiedRequestBytes = requestString.getBytes(StandardCharsets.UTF_8);
        HttpRequest modifiedRequest = HttpRequest.httpRequest(ByteArray.byteArray(modifiedRequestBytes));

        reqResp = api.http().sendRequest(modifiedRequest.withService(req.httpService()), RequestOptions.requestOptions().withHttpMode(mode));
        if (reqResp.hasResponse() && reqResp.response().statusCode()>=100) return reqResp;
        else return null;
    }

    public static HttpRequest to_HTTP_1_Request(HttpRequest req){
        String requestString = req.toString().replaceFirst("HTTP/2\r\n", "HTTP/1.1\r\n");
        return HttpRequest.httpRequest(requestString).withService(HttpService.httpService(req.httpService().host(),req.httpService().port(), req.httpService().secure()));
    }


    /**
     * Returns the longest common directory among multiple paths.
     * It computes the common prefix among the given paths and then trims
     * it back to the last '/' to ensure it represents a complete directory.
     *
     * @param paths An array of path strings.
     * @return The common directory ending with '/', or an empty string if none is found.
     */
    public static String getCommonDirectory(String... paths) {
        if (paths == null || paths.length == 0) {
            return "";
        }

        // Use the first path as the initial common prefix.
        String commonPrefix = paths[0];

        // Iterate through the rest of the paths to update the common prefix.
        for (int i = 1; i < paths.length; i++) {
            commonPrefix = getCommonPrefix(commonPrefix, paths[i]);
            if (commonPrefix.isEmpty()) {
                return "";
            }
        }

        // Find the last '/' in the common prefix to ensure we do not cut a directory name in half.
        int lastSlashIndex = commonPrefix.lastIndexOf('/');
        if (lastSlashIndex != -1) {
            return commonPrefix.substring(0, lastSlashIndex + 1);
        }
        return "";
    }

    /**
     * Returns the common prefix between two strings.
     *
     * @param s1 The first string.
     * @param s2 The second string.
     * @return The common prefix of s1 and s2.
     */
    private static String getCommonPrefix(String s1, String s2) {
        int minLength = Math.min(s1.length(), s2.length());
        int i = 0;
        while (i < minLength && s1.charAt(i) == s2.charAt(i)) {
            i++;
        }
        return s1.substring(0, i);
    }

}
