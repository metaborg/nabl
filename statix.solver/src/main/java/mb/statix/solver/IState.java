package mb.statix.solver;

import java.util.Map;
import java.util.Set;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.stratego.TermIndex;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.util.Tuple2;
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
    
    Map<Tuple2<TermIndex, ITerm>, ITerm> termProperties();

    interface Immutable extends IState {

        Tuple2<ITermVar, IState.Immutable> freshVar(String base);

        Tuple2<Scope, IState.Immutable> freshScope(String base);

        @Override IUnifier.Immutable unifier();

        @Override IScopeGraph.Immutable<Scope, ITerm, ITerm> scopeGraph();

    }

    interface Transient extends IState {

        ITermVar freshVar(String base);

        Scope freshScope(String base);

        @Override IUnifier.Transient unifier();

        @Override IScopeGraph.Transient<Scope, ITerm, ITerm> scopeGraph();

    }

}