package mb.statix.solver;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.stratego.TermIndex;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.nabl2.util.Tuple2;
import mb.statix.scopegraph.IScopeGraph;
import mb.statix.scopegraph.terms.Scope;

public interface IState {

    String resource();

    Set<ITermVar> vars();

    Set<Scope> scopes();

    IUniDisunifier unifier();

    IScopeGraph<Scope, ITerm, ITerm> scopeGraph();

    Map<Tuple2<TermIndex, ITerm>, ITermProperty> termProperties();

    interface Immutable extends IState {

        IState.Immutable withResource(String resource);

        Tuple2<ITermVar, IState.Immutable> freshWld();

        Tuple2<ITermVar, IState.Immutable> freshVar(ITermVar var);

        Tuple2<Scope, IState.Immutable> freshScope(String base);

        IState.Immutable add(IState.Immutable other);

        @Override Set.Immutable<ITermVar> vars();

        @Override Set.Immutable<Scope> scopes();

        /**
         * Return a state with the scopes and variables cleared, to be used as the initial state for entailment.
         */
        IState.Immutable subState();

        @Override IUniDisunifier.Immutable unifier();

        IState.Immutable withUnifier(IUniDisunifier.Immutable unifier);

        @Override IScopeGraph.Immutable<Scope, ITerm, ITerm> scopeGraph();

        IState.Immutable withScopeGraph(IScopeGraph.Immutable<Scope, ITerm, ITerm> scopeGraph);

        @Override Map.Immutable<Tuple2<TermIndex, ITerm>, ITermProperty> termProperties();

        IState.Immutable withTermProperties(Map.Immutable<Tuple2<TermIndex, ITerm>, ITermProperty> termProperties);

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

        public void subState() {
            freezeTwiceShameOnYou();
            state = state.subState();
        }

        @Override public IUniDisunifier unifier() {
            freezeTwiceShameOnYou();
            return state.unifier();
        }

        @Override public IScopeGraph<Scope, ITerm, ITerm> scopeGraph() {
            freezeTwiceShameOnYou();
            return state.scopeGraph();
        }

        @Override public Map<Tuple2<TermIndex, ITerm>, ITermProperty> termProperties() {
            freezeTwiceShameOnYou();
            return state.termProperties();
        }

        public ITermVar freshVar(ITermVar var) {
            freezeTwiceShameOnYou();
            final Tuple2<ITermVar, Immutable> result = state.freshVar(var);
            state = result._2();
            return result._1();
        }

        public ITermVar freshWld() {
            freezeTwiceShameOnYou();
            final Tuple2<ITermVar, Immutable> result = state.freshWld();
            state = result._2();
            return result._1();
        }

        public Scope freshScope(String base) {
            freezeTwiceShameOnYou();
            final Tuple2<Scope, Immutable> result = state.freshScope(base);
            state = result._2();
            return result._1();
        }

        public void add(IState.Immutable other) {
            freezeTwiceShameOnYou();
            state = state.add(other);
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