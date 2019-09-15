package mb.statix.solver;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.stratego.TermIndex;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.util.Tuple2;
import mb.nabl2.util.collections.IRelation3;
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

    IRelation3<TermIndex, ITerm, ITerm> termProperties();

    interface Immutable extends IState {

        IState.Immutable withResource(String resource);

        Tuple2<ITermVar, IState.Immutable> freshVar(String base);

        Tuple2<Scope, IState.Immutable> freshScope(String base);

        IState.Immutable add(IState.Immutable other);

        @Override Set.Immutable<ITermVar> vars();

        @Override Set.Immutable<Scope> scopes();

        @Override IUnifier.Immutable unifier();

        IState.Immutable withUnifier(IUnifier.Immutable unifier);

        @Override IScopeGraph.Immutable<Scope, ITerm, ITerm> scopeGraph();

        IState.Immutable withScopeGraph(IScopeGraph.Immutable<Scope, ITerm, ITerm> scopeGraph);

        @Override IRelation3.Immutable<TermIndex, ITerm, ITerm> termProperties();

        IState.Immutable withTermProperties(IRelation3.Immutable<TermIndex, ITerm, ITerm> termProperties);

    }

}