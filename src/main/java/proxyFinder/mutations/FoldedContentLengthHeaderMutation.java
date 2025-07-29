package proxyFinder.mutations;

import burp.api.montoya.http.message.requests.HttpRequest;

public class FoldedContentLengthHeaderMutation implements RequestMutationStrategy {
    public String getName() { return "FOLDED_CONTENT_LENGTH"; }
    public MutationTarget getTarget() { return MutationTarget.HEADER_FULL; }
    public HttpRequest mutate(HttpRequest base) {
        // Inject folded line after Content-Length
        String raw = base.toString();
        String toInject = "Content-Length: 5\r\n "
                + "0\r\n"; // obs-fold: space starts next line, gets merged by RFC folding
        int idx = raw.indexOf("\r\n");
        if (idx < 0) return base;
        String folded = raw.substring(0, idx)
                + "\r\n" + toInject
                + raw.substring(idx);
        return HttpRequest.httpRequest(folded).withService(base.httpService());
    }
}
