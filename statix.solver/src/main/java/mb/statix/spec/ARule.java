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
import mb.nabl2.terms.substitution.ISubstitution.Immutable;
import mb.nabl2.terms.unification.ud.PersistentUniDisunifier;
import mb.nabl2.util.TermFormatter;
import mb.statix.constraints.CExists;
import mb.statix.constraints.Constraints;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.completeness.ICompleteness;
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
            final ApplyResult applyResult;
            if((applyResult =
                    RuleUtil.apply(PersistentUniDisunifier.Immutable.of(), (Rule) this, args, null, ApplyMode.STRICT)
                            .orElse(null)) == null) {
                return Optional.of(false);
            }
            instBody = applyResult.body();
        } catch(Delay e) {
            return Optional.of(false);
        }

        // 3. Solve constraint
        try {
            final IConstraint constraint = new CExists(args, instBody);
            return Optional.of(Solver.entails(spec, State.of(), constraint, (s, l, st) -> true,
                    new NullDebugContext(), new NullProgress(), new NullCancel()));
        } catch(Delay d) {
            return Optional.empty();
        }
    }

    @Value.Default public @Nullable ICompleteness.Immutable bodyCriticalEdges() {
        return null;
    }

    public Set.Immutable<ITermVar> freeVars() {
        return Set.Immutable.subtract(Constraints.freeVars(body()), paramVars());
    }

    public Set.Immutable<ITermVar> varSet() {
        return Set.Immutable.union(Constraints.vars(body()), paramVars());
    }

    public Rule apply(ISubstitution.Immutable subst) {
        final Immutable localSubst = subst.removeAll(paramVars());
        final IConstraint newBody = body().apply(localSubst);
        final ICompleteness.Immutable newCriticalEdges =
                bodyCriticalEdges() == null ? null : bodyCriticalEdges().apply(localSubst);
        return Rule.of(name(), params(), newBody).withBodyCriticalEdges(newCriticalEdges);
    }

    public Rule apply(IRenaming subst) {
        final List<Pattern> newParams =
                params().stream().map(p -> p.apply(subst)).collect(ImmutableList.toImmutableList());
        final IConstraint newBody = body().apply(subst);
        final ICompleteness.Immutable newCriticalEdges =
                bodyCriticalEdges() == null ? null : bodyCriticalEdges().apply(subst);
        return Rule.of(name(), newParams, newBody).withBodyCriticalEdges(newCriticalEdges);
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