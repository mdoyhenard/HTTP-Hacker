package proxyFinder.mutations;

import burp.api.montoya.http.message.requests.HttpRequest;

public class InvalidVersionMutation implements RequestMutationStrategy {
    public String getName() { return "INVALID_VERSION"; }
    public MutationTarget getTarget() { return MutationTarget.VERSION; }
    public HttpRequest mutate(HttpRequest base) {
        String requestStr = base.toString();
        int idx = requestStr.indexOf("\r\n");
        if (idx < 0) return base;
        String firstLine = requestStr.substring(0, idx)
                .replaceFirst("HTTP/1\\.1", "HTTP/1.9")
                .replaceFirst("HTTP/1\\.0", "HTTP/1.9");
        String mutated = firstLine + requestStr.substring(idx);
        return HttpRequest.httpRequest(mutated).withService(base.httpService());
    }
}
