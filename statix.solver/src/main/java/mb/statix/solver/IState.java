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

        default Transient melt() {
            return new Transient(this);
        }

    }

    class Transient implements IState {

        private IState.Immutable state;
        private boolean frozen = false;

        private Transient(IState.Immutable state) {
            this.state = state;
        }

        @Override public Spec spec() {
            freezeTwiceShameOnYou();
            return state.spec();
        }

        @Override public String resource() {
            freezeTwiceShameOnYou();
            return state.resource();
        }

        @Override public Set<ITermVar> vars() {
            freezeTwiceShameOnYou();
            return state.vars();
        }

        @Override public Set<Scope> scopes() {
            freezeTwiceShameOnYou();
            return state.scopes();
        }

        @Override public IUnifier unifier() {
            freezeTwiceShameOnYou();
            return state.unifier();
        }

        @Override public IScopeGraph<Scope, ITerm, ITerm> scopeGraph() {
            freezeTwiceShameOnYou();
            return state.scopeGraph();
        }

        @Override public IRelation3<TermIndex, ITerm, ITerm> termProperties() {
            freezeTwiceShameOnYou();
            return state.termProperties();
        }

        public ITermVar freshVar(String base) {
            freezeTwiceShameOnYou();
            final Tuple2<ITermVar, Immutable> result = state.freshVar(base);
            state = result._2();
            return result._1();
        }

        public Scope freshScope(String base) {
            freezeTwiceShameOnYou();
            final Tuple2<Scope, Immutable> result = state.freshScope(base);
            state = result._2();
            return result._1();
        }

        public Immutable freeze() {
            freezeTwiceShameOnYou();
            frozen = true;
            return state;
        }

        void freezeTwiceShameOnYou() {
            if(frozen) {
                throw new IllegalStateException("Already frozen, cannot modify further.");
            }
        }

    }

}
