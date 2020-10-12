package mb.statix.spec;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermPattern.P;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.util.task.NullCancel;
import org.metaborg.util.task.NullProgress;

import com.google.common.collect.ImmutableList;

import io.usethesource.capsule.Set;
import io.usethesource.capsule.util.stream.CapsuleCollectors;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.matching.Pattern;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.Unifiers;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.nabl2.util.TermFormatter;
import mb.statix.constraints.CExists;
import mb.statix.constraints.Constraints;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.log.NullDebugContext;
import mb.statix.solver.persistent.Solver;
import mb.statix.solver.persistent.State;

@Value.Immutable
@Serial.Version(42L)
public abstract class ARule {

    @Value.Default public String label() {
        return "";
    }

    @Value.Parameter public abstract String name();

    @Value.Parameter public abstract List<Pattern> params();

    @Value.Lazy public Set.Immutable<ITermVar> paramVars() {
        return params().stream().flatMap(t -> t.getVars().stream()).collect(CapsuleCollectors.toSet());
    }

    @Value.Parameter public abstract IConstraint body();

    @Value.Lazy public Optional<Boolean> isAlways(Spec spec) throws InterruptedException {
        // 1. Create arguments
        final ImmutableList.Builder<ITermVar> argsBuilder = ImmutableList.builder();
        for(int i = 0; i < params().size(); i++) {
            argsBuilder.add(B.newVar("", "arg" + Integer.toString(i)));
        }
        final ImmutableList<ITermVar> args = argsBuilder.build();

        // 2. Instantiate body
        final IConstraint instBody;
        try {
            if((instBody = apply(args, Unifiers.Immutable.of()).orElse(null)) == null) {
                return Optional.of(false);
            }
        } catch(Delay e) {
            return Optional.of(false);
        }

        // 3. Solve constraint
        try {
            final IConstraint constraint = new CExists(args, instBody);
            return Optional.of(Solver.entails(spec, State.of(spec), constraint, (s, l, st) -> true,
                    new NullDebugContext(), new NullProgress(), new NullCancel()));
        } catch(Delay d) {
            return Optional.empty();
        }
    }

    public Set.Immutable<ITermVar> freeVars() {
        return Set.Immutable.subtract(Constraints.freeVars(body()), paramVars());
    }

    public Set.Immutable<ITermVar> varSet() {
        return Set.Immutable.union(Constraints.vars(body()), paramVars());
    }

    public Rule apply(ISubstitution.Immutable subst) {
        final IConstraint newBody = body().apply(subst.removeAll(paramVars()));
        return Rule.of(name(), params(), newBody);
    }

    public Rule apply(IRenaming subst) {
        final List<Pattern> newParams =
                params().stream().map(p -> p.apply(subst)).collect(ImmutableList.toImmutableList());
        final IConstraint newBody = body().apply(subst);
        return Rule.of(name(), newParams, newBody);
    }

    public Optional<IConstraint> apply(List<? extends ITerm> args, IUniDisunifier.Immutable unifier) throws Delay {
        return apply(args, unifier, null);
    }

    public Optional<IConstraint> apply(List<? extends ITerm> args, IUniDisunifier.Immutable unifier,
            @Nullable IConstraint cause) throws Delay {
        final ISubstitution.Transient subst;
        final Optional<ISubstitution.Immutable> matchResult =
                P.match(params(), args, unifier).orElseThrow(vars -> Delay.ofVars(vars));
        if((subst = matchResult.map(u -> u.melt()).orElse(null)) == null) {
            return Optional.empty();
        }
        final ISubstitution.Immutable isubst = subst.freeze();
        final IConstraint newBody = body().apply(isubst);
        return Optional.of(newBody.withCause(cause));
    }

    public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        if(!label().isEmpty()) {
            sb.append("[").append(label()).append("] ");
        }
        if(name().isEmpty()) {
            sb.append("{ ");
        } else {
            sb.append(name()).append("(");
        }
        sb.append(params().stream().map(Pattern::toString).collect(Collectors.joining(", ")));
        if(!name().isEmpty()) {
            sb.append(")");
        }
        sb.append(" :- ");
        sb.append(body().toString(termToString));
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
    public static final LeftRightOrder leftRightPatternOrdering = new LeftRightOrder();

    /**
     * Note: this comparator imposes orderings that are inconsistent with equals.
     */
    public static class LeftRightOrder {

        public Optional<Integer> compare(Rule r1, Rule r2) {
            final Pattern p1 = P.newTuple(r1.params());
            final Pattern p2 = P.newTuple(r2.params());
            return Pattern.leftRightOrdering.compare(p1, p2);
        }

        public Comparator<Rule> asComparator() {
            return (r1, r2) -> LeftRightOrder.this.compare(r1, r2).orElse(0);
        }

    }

}