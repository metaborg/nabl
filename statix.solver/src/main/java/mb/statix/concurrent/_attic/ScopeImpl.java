package mb.statix.concurrent._attic;

public interface ScopeImpl<S> {

    S make(String resource, String name);

    String resource(S scope);

}