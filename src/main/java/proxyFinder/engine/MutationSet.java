package proxyFinder.engine;

import burp.api.montoya.http.message.requests.HttpRequest;
import proxyFinder.mutations.RequestMutationStrategy;
import java.util.List;

public class MutationSet {
    private final List<RequestMutationStrategy> mutations;

    public MutationSet(List<RequestMutationStrategy> mutations) {
        this.mutations = mutations;
    }

    public HttpRequest apply(HttpRequest base) {
        HttpRequest req = base;
        for (RequestMutationStrategy mutation : mutations) {
            req = mutation.mutate(req);
        }
        return req;
    }

    public List<RequestMutationStrategy> getMutations() {
        return mutations;
    }

    public String describe() {
        StringBuilder sb = new StringBuilder();
        for (RequestMutationStrategy m : mutations) {
            sb.append(m.getName()).append(' ');
        }
        return sb.toString().trim();
    }

    @Override
    public String toString() {
        return describe();
    }
}
