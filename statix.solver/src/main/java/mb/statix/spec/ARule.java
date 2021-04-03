package mb.statix.spec;

import static mb.nabl2.terms.matching.TermPattern.P;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import com.google.common.collect.ImmutableList;

import io.usethesource.capsule.Set;
import io.usethesource.capsule.util.stream.CapsuleCollectors;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.matching.Pattern;
import mb.nabl2.terms.substitution.FreshVars;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.substitution.ISubstitution.Immutable;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.nabl2.terms.unification.ud.PersistentUniDisunifier;
import mb.nabl2.util.CapsuleUtil;
import mb.nabl2.util.TermFormatter;
import mb.statix.constraints.Constraints;
import mb.statix.solver.IConstraint;
import mb.statix.solver.StateUtil;
import mb.statix.solver.completeness.ICompleteness;

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


    @Value.Parameter abstract Set.Immutable<ITermVar> evars();

    @Value.Parameter abstract IUniDisunifier.Immutable unifier();


    @Value.Parameter public abstract IConstraint body();

    @Value.Default public @Nullable ICompleteness.Immutable bodyCriticalEdges() {
        return null;
    }


    @Value.Lazy public Optional<Boolean> isAlways(Spec spec) throws InterruptedException {
        if(params().stream().anyMatch(p -> p.isConstructed())) {
            return Optional.empty();
        }
        if(!unifier().isEmpty()) {
            return Optional.empty();
        }
        return body().match(Constraints.<Optional<Boolean>>cases()._true(c -> Optional.of(true))
                ._false(c -> Optional.of(false)).otherwise(c -> Optional.empty()));

    }


    /**
     * Apply capture avoiding substitution.
     */
    public Rule apply(ISubstitution.Immutable subst) {
        final Immutable localSubst = subst.removeAll(paramVars());
        if(localSubst.isEmpty()) {
            return (Rule) this;
        }

        final FreshVars fresh = new FreshVars(localSubst.rangeSet());
        final IRenaming ren = fresh.fresh(paramVars());
        fresh.fix();
        if(ren.isEmpty()) {
        // @formatter:off
            return Rule.of(
                    name(),
                    params(),
                    body().apply(localSubst)
            ).withBodyCriticalEdges(bodyCriticalEdges() == null ? null : bodyCriticalEdges().apply(localSubst));
        // @formatter:off
        }

        // @formatter:off
        return Rule.of(
                name(),
                params().stream().map(p -> p.apply(ren)).collect(ImmutableList.toImmutableList()),
                body().apply(ren).apply(localSubst)
        ).withBodyCriticalEdges(bodyCriticalEdges() == null ? null : bodyCriticalEdges().apply(ren).apply(localSubst));
        // @formatter:off
    }

    /**
     * Apply variable renaming.
     */
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
        if(!evars().isEmpty()) {
            sb.append(evars().stream().map(ITerm::toString).collect(Collectors.joining(" ", "{", "} ")));
        }
        if(!unifier().isEmpty()) {
            sb.append(StateUtil.asConstraint(unifier()).stream().map(c -> c.toString(termToString)).collect(Collectors.joining(", ", "", " | ")));
        }
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


    public static Rule of(String name, Iterable<? extends Pattern> params, IConstraint body) {
        return Rule.of(name, params, CapsuleUtil.immutableSet(), PersistentUniDisunifier.Immutable.of(), body);
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