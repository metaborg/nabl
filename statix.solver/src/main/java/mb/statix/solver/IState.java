package mb.statix.solver;

import java.util.Set;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.scopegraph.IScopeGraph;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.spec.Spec;

public interface IState {

    Spec spec();

    String resource();

    Set<ITermVar> vars();

    Set<Scope> scopes();

    IUnifier unifier();

    IScopeGraph<Scope, ITerm, ITerm> scopeGraph();

}