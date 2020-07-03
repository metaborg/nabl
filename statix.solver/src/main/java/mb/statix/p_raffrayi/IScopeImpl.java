package mb.statix.p_raffrayi;

public interface IScopeImpl<S> {

    S make(String id, String name);

    String id(S scope);

}