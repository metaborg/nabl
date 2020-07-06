package mb.statix.concurrent.p_raffrayi;

public interface IScopeImpl<S> {

    S make(String id, String name);

    String id(S scope);

}