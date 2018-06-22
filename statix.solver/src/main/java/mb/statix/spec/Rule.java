package mb.statix.spec;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.matching.IPattern.MatchResult;
import mb.nabl2.terms.matching.MatchException;
import mb.nabl2.terms.matching.TermPattern;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.PersistentUnifier;
import mb.nabl2.terms.unification.UnificationException;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IGuard;
import mb.statix.solver.State;

public class Rule {

    private final String name;
    private final List<ITerm> params;
    private final Set<ITermVar> guardVars;
    private final List<IGuard> guard;
    private final Set<ITermVar> bodyVars;
    private final List<IConstraint> body;

    public Rule(String name, Iterable<? extends ITerm> params, Iterable<ITermVar> guardVars, Iterable<IGuard> guard,
            Iterable<ITermVar> bodyVars, Iterable<IConstraint> body) {
        this.name = name;
        this.params = ImmutableList.copyOf(params);
        this.guardVars = ImmutableSet.copyOf(guardVars);
        this.guard = ImmutableList.copyOf(guard);
        this.bodyVars = ImmutableSet.copyOf(bodyVars);
        this.body = ImmutableList.copyOf(body);
    }

    public String getName() {
        return name;
    }

    public List<ITerm> getParams() {
        return params;
    }

    public Set<ITermVar> getGuardVars() {
        return guardVars;
    }

    public List<IGuard> getGuard() {
        return guard;
    }

    public Set<ITermVar> getBodyVars() {
        return bodyVars;
    }

    public List<IConstraint> getBody() {
        return body;
    }

    public Tuple2<State, Rule> apply(List<ITerm> args, State state) throws MatchException, UnificationException {
        final MatchResult matchResult = new TermPattern(params).match(args);
        ISubstitution.Transient subst = matchResult.substitution().melt();
        State newState = state.withUnifier(state.unifier().unify(matchResult.unifier()).unifier());
        // guard vars
        final ImmutableSet.Builder<ITermVar> freshGuardVars = ImmutableSet.builder();
        for(ITermVar var : guardVars) {
            final Tuple2<ITermVar, State> vs = newState.freshVar(var.getName());
            subst.put(var, vs._1());
            freshGuardVars.add(vs._1());
            newState = vs._2();
        }
        // body vars
        final ImmutableSet.Builder<ITermVar> freshBodyVars = ImmutableSet.builder();
        for(ITermVar var : bodyVars) {
            final Tuple2<ITermVar, State> vs = newState.freshVar(var.getName());
            subst.put(var, vs._1());
            freshBodyVars.add(vs._1());
            newState = vs._2();
        }
        final ISubstitution.Immutable isubst = subst.freeze();
        final Set<IGuard> newGuard = guard.stream().map(c -> c.apply(isubst)).collect(Collectors.toSet());
        final Set<IConstraint> newBody = body.stream().map(c -> c.apply(isubst)).collect(Collectors.toSet());
        final Rule newRule = new Rule(name, args, freshGuardVars.build(), newGuard, freshBodyVars.build(), newBody);
        return ImmutableTuple2.of(newState, newRule);
    }

    public String toString(IUnifier unifier) {
        final StringBuilder sb = new StringBuilder();
        sb.append(name);
        sb.append("(");
        sb.append(unifier.toString(params));
        sb.append(")");
        if(!guard.isEmpty()) {
            sb.append(" | ");
            if(!guardVars.isEmpty()) {
                sb.append("{").append(unifier.toString(guardVars)).append("} ");
            }
            sb.append(IGuard.toString(guard, unifier));
        }
        if(!body.isEmpty()) {
            sb.append(" :- ");
            if(!bodyVars.isEmpty()) {
                sb.append("{").append(unifier.toString(bodyVars)).append("} ");
            }
            sb.append(IConstraint.toString(body, unifier));
        }
        sb.append(".");
        return sb.toString();
    }

    @Override public String toString() {
        return toString(PersistentUnifier.Immutable.of());
    }

}