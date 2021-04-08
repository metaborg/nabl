package mb.statix.concurrent.p_raffrayi.impl;

import java.util.Collection;
import java.util.Map;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.Transform.T;
import mb.statix.concurrent.p_raffrayi.IScopeImpl;
import mb.statix.scopegraph.terms.Scope;

public class ScopeImpl implements IScopeImpl<Scope, ITerm> {

    @Override public Scope make(String id, String name) {
        return Scope.of(id, name);
    }

    @Override public String id(Scope scope) {
        return scope.getResource();
    }

    @Override public String name(Scope scope) {
        return scope.getName();
    }

    @Override public ITerm embed(Scope scope) {
        return scope;
    }

    @Override public Collection<Scope> getAllScopes(ITerm datum) {
        return T.collecttd(Scope.matcher()::match).apply(datum);
    }

    @Override public ITerm subtituteScopes(ITerm datum, Map<Scope, Scope> substitution) {
        return T.sometd(Scope.matcher().map(s -> (ITerm) substitution.getOrDefault(s, s))::match).apply(datum);
    }

}