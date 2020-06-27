package mb.statix.solver.concurrent2.impl;

import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.concurrent2.IScopeImpl;

public class ScopeImpl implements IScopeImpl<Scope> {

    @Override public Scope make(String id, String name) {
        return Scope.of(id, name);
    }

    @Override public String id(Scope scope) {
        return scope.getResource();
    }

}