package mb.statix.spec;

import static mb.nabl2.terms.matching.TermPattern.P;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.matching.Pattern;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.substitution.ISubstitution.Immutable;
import mb.nabl2.util.ImmutableTuple3;
import mb.nabl2.util.TermFormatter;
import mb.nabl2.util.Tuple2;
import mb.nabl2.util.Tuple3;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.Solver;
import mb.statix.solver.SolverResult;
import mb.statix.solver.State;
import mb.statix.solver.log.NullDebugContext;

@Value.Immutable
@Serial.Version(42L)
public abstract class ARule {

    @Value.Parameter public abstract String name();

    @Value.Parameter public abstract List<Pattern> params();

    @Value.Lazy public Set<ITermVar> paramVars() {
        return params().stream().flatMap(t -> t.getVars().stream()).collect(ImmutableSet.toImmutableSet());
    }

    @Value.Parameter public abstract Set<ITermVar> bodyVars();

    @Value.Parameter public abstract List<IConstraint> body();

    @Value.Lazy public Optional<Boolean> isAlways(Spec spec) throws InterruptedException {
        State state = State.of(spec);
        List<ITerm> args = Lists.newArrayList();
        for(@SuppressWarnings("unused") Pattern param : params()) {
            final Tuple2<ITermVar, State> stateAndVar = state.freshVar("arg");
            args.add(stateAndVar._1());
            state = stateAndVar._2();
        }
        Tuple3<State, Set<ITermVar>, List<IConstraint>> stateAndInst;
        try {
            if((stateAndInst = apply(args, state).orElse(null)) == null) {
                return Optional.of(false);
            }
        } catch(Delay e) {
            return Optional.of(false);
        }
        state = stateAndInst._1();
        final Set<ITermVar> instVars = stateAndInst._2();
        final List<IConstraint> instBody = stateAndInst._3();
        try {
            Optional<SolverResult> solverResult =
                    Solver.entails(state, instBody, (s, l, st) -> true, instVars, new NullDebugContext());
            if(solverResult.isPresent()) {
                return Optional.of(true);
            } else {
                return Optional.of(false);
            }
        } catch(Delay d) {
            return Optional.empty();
        }
    }

    public Rule apply(ISubstitution.Immutable subst) {
        final ISubstitution.Immutable bodySubst = subst.removeAll(paramVars()).removeAll(bodyVars());
        final List<IConstraint> newBody =
                body().stream().map(c -> c.apply(bodySubst)).collect(ImmutableList.toImmutableList());
        return Rule.of(name(), params(), bodyVars(), newBody);
    }

    public Optional<Tuple3<State, Set<ITermVar>, List<IConstraint>>> apply(List<ITerm> args, State state) throws Delay {
        final ISubstitution.Transient subst;
        final Optional<Immutable> matchResult = P.match(params(), args, state.unifier()).matchOrThrow(r -> r, vars -> {
            throw Delay.ofVars(vars);
        });
        if((subst = matchResult.map(u -> u.melt()).orElse(null)) == null) {
            return Optional.empty();
        }
        State newState = state;
        final ImmutableSet.Builder<ITermVar> freshBodyVars = ImmutableSet.builder();
        for(ITermVar var : bodyVars()) {
            final Tuple2<ITermVar, State> vs = newState.freshVar(var.getName());
            subst.put(var, vs._1());
            freshBodyVars.add(vs._1());
            newState = vs._2();
        }
        final ISubstitution.Immutable isubst = subst.freeze();
        final List<IConstraint> newBody =
                body().stream().map(c -> c.apply(isubst)).collect(ImmutableList.toImmutableList());
        return Optional.of(ImmutableTuple3.of(newState, freshBodyVars.build(), newBody));
    }

    public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        if(name().isEmpty()) {
            sb.append("{ ").append(params());
        } else {
            sb.append(name()).append("(").append(params()).append(")");
        }
        if(!body().isEmpty()) {
            sb.append(" :- ");
            if(!bodyVars().isEmpty()) {
                sb.append("{").append(bodyVars()).append("} ");
            }
            sb.append(IConstraint.toString(body(), termToString.removeAll(bodyVars())));
        }
        if(name().isEmpty()) {
            sb.append(" }");
        } else {
            sb.append(".");

        }
        return sb.toString();
    }

    @Override public String toString() {
        return toString(ITerm::toString);
    }

    /**
     * Note: this comparator imposes orderings that are inconsistent with equals.
     */
    public static java.util.Comparator<Rule> leftRightPatternOrdering = new LeftRightPatternOrder();

    /**
     * Note: this comparator imposes orderings that are inconsistent with equals.
     */
    private static class LeftRightPatternOrder implements Comparator<Rule> {

        @Override public int compare(Rule r1, Rule r2) {
            final Pattern p1 = P.newTuple(r1.params());
            final Pattern p2 = P.newTuple(r2.params());
            return Pattern.leftRightOrdering.compare(p1, p2);
        }

    }

}