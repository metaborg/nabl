package mb.statix.spec;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.matching.MatchException;
import mb.nabl2.terms.matching.TermPattern;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.PersistentUnifier;
import mb.nabl2.terms.unification.UnificationException;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;
import mb.statix.solver.Completeness;
import mb.statix.solver.Config;
import mb.statix.solver.IConstraint;
import mb.statix.solver.NullDebugContext;
import mb.statix.solver.Solver;
import mb.statix.solver.State;

public class Lambda {

    private final List<ITerm> params;
    private final Set<ITermVar> bodyVars;
    private final List<IConstraint> body;

    public Lambda(Iterable<? extends ITerm> params, Iterable<ITermVar> bodyVars, Iterable<IConstraint> body) {
        this.params = ImmutableList.copyOf(params);
        this.bodyVars = ImmutableSet.copyOf(bodyVars);
        this.body = ImmutableList.copyOf(body);
    }

    public Set<ITermVar> getParamVars() {
        return params.stream().flatMap(t -> t.getVars().stream()).collect(Collectors.toSet());
    }

    public List<ITerm> getParams() {
        return params;
    }

    public Set<ITermVar> getBodyVars() {
        return bodyVars;
    }

    public List<IConstraint> getBody() {
        return body;
    }

    public Optional<Boolean> isAlways(Spec spec) throws InterruptedException {
        final State state = State.of(spec);
        final Config config = Config.of(state, body, new Completeness());
        return Solver.entails(config, bodyVars, new NullDebugContext());
    }

    public Lambda apply(ISubstitution.Immutable subst) {
        final ISubstitution.Immutable bodySubst = subst.removeAll(getParamVars()).removeAll(bodyVars);
        final List<IConstraint> newBody = body.stream().map(c -> c.apply(bodySubst)).collect(Collectors.toList());
        return new Lambda(params, bodyVars, newBody);
    }

    public Tuple2<State, Lambda> apply(List<ITerm> args, State state) throws MatchException, UnificationException {
        final ISubstitution.Transient subst = new TermPattern(state.unifier()::areEqual, params).match(args).melt();
        State newState = state;
        final ImmutableSet.Builder<ITermVar> freshBodyVars = ImmutableSet.builder();
        for(ITermVar var : bodyVars) {
            final Tuple2<ITermVar, State> vs = newState.freshVar(var.getName());
            subst.put(var, vs._1());
            freshBodyVars.add(vs._1());
            newState = vs._2();
        }
        final ISubstitution.Immutable isubst = subst.freeze();
        final Set<IConstraint> newBody = body.stream().map(c -> c.apply(isubst)).collect(Collectors.toSet());
        final Lambda newRule = new Lambda(args, freshBodyVars.build(), newBody);
        return ImmutableTuple2.of(newState, newRule);
    }

    public String toString(IUnifier unifier) {
        final StringBuilder sb = new StringBuilder();
        sb.append("{ ");
        sb.append(unifier.toString(params));
        if(!body.isEmpty()) {
            sb.append(" :- ");
            if(!bodyVars.isEmpty()) {
                sb.append("{").append(unifier.toString(bodyVars)).append("} ");
            }
            sb.append(IConstraint.toString(body, unifier));
        }
        sb.append(" }");
        return sb.toString();
    }

    @Override public String toString() {
        return toString(PersistentUnifier.Immutable.of());
    }

}