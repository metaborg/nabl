package mb.statix.solver.concurrent2;

public interface IScopeImpl<S> {

    S make(String id, String name);

    String id(S scope);

}