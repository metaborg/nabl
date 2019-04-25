package mb.statix.spec;

import static mb.nabl2.terms.matching.TermPattern.P;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.util.Ref;
import org.metaborg.util.functions.Function1;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.matching.Pattern;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.PersistentUnifier;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.TermFormatter;
import mb.nabl2.util.Tuple2;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.Solver;
import mb.statix.solver.log.NullDebugContext;
import mb.statix.solver.SolverResult;
import mb.statix.solver.State;

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
        final Ref<State> state = new Ref<>(State.of(spec));
        final Function1<String, ITermVar> freshVar = (base) -> {
            final Tuple2<ITermVar, State> stateAndVar = state.get().freshVar(base);
            state.set(stateAndVar._2());
            return stateAndVar._1();
        };
        List<ITerm> args = params().stream().map(p -> freshVar.apply("arg")).collect(ImmutableList.toImmutableList());
        Tuple2<Set<ITermVar>, List<IConstraint>> inst;
        try {
            if((inst = apply(args, PersistentUnifier.Immutable.of(), freshVar).orElse(null)) == null) {
                return Optional.of(false);
            }
        } catch(Delay e) {
            return Optional.of(false);
        }
        final Set<ITermVar> instVars = inst._1();
        final List<IConstraint> instBody = inst._2();
        try {
            Optional<SolverResult> solverResult =
                    Solver.entails(state.get(), instBody, (s, l, st) -> true, instVars, new NullDebugContext());
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

    public Optional<Tuple2<Set<ITermVar>, List<IConstraint>>> apply(List<ITerm> args, IUnifier unifier,
            Function1<String, ITermVar> freshVar) throws Delay {
        return apply(args, unifier, freshVar, null);
    }

    public Optional<Tuple2<Set<ITermVar>, List<IConstraint>>> apply(List<ITerm> args, IUnifier unifier,
            Function1<String, ITermVar> freshVar, @Nullable IConstraint cause) throws Delay {
        final ISubstitution.Transient subst;
        final Optional<ISubstitution.Immutable> matchResult =
                P.match(params(), args, unifier).matchOrThrow(r -> r, vars -> {
                    throw Delay.ofVars(vars);
                });
        if((subst = matchResult.map(u -> u.melt()).orElse(null)) == null) {
            return Optional.empty();
        }
        final ImmutableSet.Builder<ITermVar> freshBodyVars = ImmutableSet.builder();
        for(ITermVar var : bodyVars()) {
            final ITermVar v = freshVar.apply(var.getName());
            subst.put(var, v);
            freshBodyVars.add(v);
        }
        final ISubstitution.Immutable isubst = subst.freeze();
        final List<IConstraint> newBody =
                body().stream().map(c -> c.apply(isubst).withCause(cause)).collect(ImmutableList.toImmutableList());
        return Optional.of(ImmutableTuple2.of(freshBodyVars.build(), newBody));
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
                sb.append("{").append(termToString.format(bodyVars())).append("} ");
            }
            sb.append(IConstraint.toString(body(), termToString));
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