package mb.statix.solver.concurrent;

public interface ScopeImpl<S> {

    S make(String resource, String name);

    String resource(S scope);

}