package mb.statix.p_raffrayi.impl;

import mb.statix.p_raffrayi.IScopeImpl;
import mb.statix.scopegraph.terms.Scope;

public class ScopeImpl implements IScopeImpl<Scope> {

    @Override public Scope make(String id, String name) {
        return Scope.of(id, name);
    }

    @Override public String id(Scope scope) {
        return scope.getResource();
    }

}