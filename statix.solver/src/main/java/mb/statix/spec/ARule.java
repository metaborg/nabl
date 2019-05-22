package mb.statix.spec;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermPattern.P;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.immutables.serial.Serial;
import org.immutables.value.Value;

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
import mb.statix.constraints.CExists;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.log.NullDebugContext;
import mb.statix.solver.persistent.Solver;
import mb.statix.solver.persistent.SolverResult;
import mb.statix.solver.persistent.State;

/**
 * Class which describes a statix rule.
 * 
 * <pre>ruleName(paramVars) :- {bodyVars} constraints.</pre>
 * or
 * <pre>{ paramVars :- {bodyVars} constraints }</pre>
 */
@Value.Immutable
@Serial.Version(42L)
public abstract class ARule implements IRule {

    @Override
    @Value.Parameter public abstract String name();

    @Override
    @Value.Parameter public abstract List<Pattern> params();

    @Override
    @Value.Lazy public Set<ITermVar> paramVars() {
        return params().stream().flatMap(t -> t.getVars().stream()).collect(ImmutableSet.toImmutableSet());
    }

    @Override
    @Value.Parameter public abstract IConstraint body();

    @Override
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
            if((instBody = apply(args, PersistentUnifier.Immutable.of()).map(Tuple2::_2).orElse(null)) == null) {
                return Optional.of(false);
            }
        } catch(Delay e) {
            return Optional.of(false);
        }

        // 3. Solve constraint
        try {
            final IConstraint constraint = new CExists(args, instBody);
            final Optional<SolverResult> solverResult =
                    Solver.entails(State.of(spec), constraint, (s, l, st) -> true, new NullDebugContext());
            if(solverResult.isPresent()) {
                return Optional.of(true);
            } else {
                return Optional.of(false);
            }
        } catch(Delay d) {
            return Optional.empty();
        }
    }

    @Override
    public IRule apply(ISubstitution.Immutable subst) {
        final IConstraint newBody = body().apply(subst.removeAll(paramVars()));
        return Rule.of(name(), params(), newBody);
    }

    @Override
    public Optional<Tuple2<ISubstitution.Immutable,IConstraint>> apply(List<? extends ITerm> args, IUnifier unifier, @Nullable IConstraint cause)
            throws Delay {
        final ISubstitution.Transient subst;
        final Optional<ISubstitution.Immutable> matchResult =
                P.match(params(), args, unifier).matchOrThrow(r -> r, vars -> {
                    throw Delay.ofVars(vars);
                });
        if((subst = matchResult.map(u -> u.melt()).orElse(null)) == null) {
            return Optional.empty();
        }
        final ISubstitution.Immutable isubst = subst.freeze();
        final IConstraint newBody = body().apply(isubst);
        return Optional.of(ImmutableTuple2.of(isubst, newBody));
    }

    /**
     * Formats this rule where constraints are formatted with the given TermFormatter.
     * 
     * <pre>&lt;name&gt;(&lt;params&gt;) [:- [{&lt;bodyVars&gt;}] &lt;constraints&gt;].</pre>
     * <pre>{ &lt;params&gt; [:- [{&lt;bodyVars&gt;}] &lt;constraints&gt;] }</pre>
     * 
     * @param termToString
     *      the term formatter to format constraints with
     * 
     * @return
     *      the string
     */
    @Override
    public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        if(name().isEmpty()) {
            sb.append("{ ").append(params());
        } else {
            sb.append(name()).append("(").append(params()).append(")");
        }
        sb.append(" :- ");
        sb.append(body().toString(termToString));
        sb.append(".");
        return sb.toString();
    }

    @Override public String toString() {
        return toString(ITerm::toString);
    }

    /**
     * Note: this comparator imposes orderings that are inconsistent with equals.
     */
    public static java.util.Comparator<IRule> leftRightPatternOrdering = new LeftRightPatternOrder();

    /**
     * Note: this comparator imposes orderings that are inconsistent with equals.
     */
    private static class LeftRightPatternOrder implements Comparator<IRule> {

        @Override public int compare(IRule r1, IRule r2) {
            final Pattern p1 = P.newTuple(r1.params());
            final Pattern p2 = P.newTuple(r2.params());
            return Pattern.leftRightOrdering.compare(p1, p2);
        }

    }

}