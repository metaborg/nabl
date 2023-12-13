package mb.statix.spec;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermPattern.P;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.annotation.Nullable;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.task.NullCancel;
import org.metaborg.util.task.NullProgress;

import io.usethesource.capsule.Set;
import io.usethesource.capsule.Set.Immutable;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.matching.Pattern;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.substitution.Renaming;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IState;
import mb.statix.solver.log.NullDebugContext;
import mb.statix.solver.persistent.Solver;
import mb.statix.solver.persistent.SolverResult;
import mb.statix.solver.persistent.State;
import mb.statix.solver.tracer.EmptyTracer.Empty;
import mb.statix.spec.ApplyMode.Safety;

/**
 * A special rule application that supports indexing in its parameters. The index allows grouping based on which
 * arguments match a constraint, and finding matching arguments using lookup.
 *
 * The class throws Delay exceptions in the following cases:
 * <ul>
 * <li>If the initial application does not reduce enough, and the remaining constraint has free variables left. The free
 * variables prevent computing the applyIndex without additional state information.
 * <li>If the applyIndex has free variables. The index cannot be used for lookup, as index equality is modulo further
 * instantiation of the free variables. the arguments, potentially putting terms in different buckets that should be in
 * the same.
 * <li>If the lookupIndex has free variables. The resulting index cannot be used for lookup, as index equality is modulo
 * further instantiation of the variables.
 * </ul>
 */
public class IndexedRuleApplication {

    private static final ILogger logger = LoggerUtils.logger(IndexedRuleApplication.class);

    private final Spec spec;
    private final List<Pattern> params;
    private final @Nullable IConstraint constraint;
    private final ITerm index;

    private IndexedRuleApplication(Spec spec, List<Pattern> params, @Nullable IConstraint constraint, ITerm index) {
        this.spec = spec;
        this.params = params;
        this.constraint = constraint;
        this.index = index;
    }

    private IndexedRuleApplication apply(IRenaming renaming) {
        final List<Pattern> newParams = params.stream().map(p -> p.apply(renaming)).collect(Collectors.toList());
        final IConstraint newConstraint = constraint == null ? null : constraint.apply(renaming);
        return new IndexedRuleApplication(spec, newParams, newConstraint, renaming.apply(index));
    }

    /**
     * Compute a lookup index for the given state. The state should be an extension of the state given in the
     * construction of this object.
     */
    public ITerm lookupIndex(IState.Immutable state) throws Delay, InterruptedException {
        final ITerm lookupIndex = state.unifier().findRecursive(index);
        if(!lookupIndex.isGround()) {
            throw Delay.ofVars(lookupIndex.getVars());
        }
        return lookupIndex;
    }

    public Optional<ITerm> applyIndex(ITerm... args) throws Delay, InterruptedException {
        return applyIndex(Arrays.asList(args));
    }

    /**
     * Compute an apply index for the given arguments.
     */
    public Optional<ITerm> applyIndex(List<ITerm> args) throws Delay, InterruptedException {
        final ISubstitution.Immutable subst;
        if((subst = P.match(params, args).orElse(null)) == null) {
            return Optional.empty();
        }
        if(constraint != null) {
            final State state = State.of();
            final NullDebugContext debug = new NullDebugContext();
            final SolverResult<Empty> solveResult = Solver.solve(spec, state, constraint.apply(subst), debug, new NullCancel(),
                    new NullProgress(), Solver.RETURN_ON_FIRST_ERROR);
            try {
                if(!Solver.entailed(state, solveResult, debug)) {
                    return Optional.empty();
                }
            } catch(Delay d) {
                logger.warn("Unexpected delay when computing apply index.", d);
                return Optional.empty();
            }
        }
        final ITerm applyIndex = subst.apply(index);
        if(!applyIndex.isGround()) {
            throw Delay.ofVars(applyIndex.getVars());
        }
        return Optional.of(applyIndex);
    }

    @Override public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("(").append(params.stream().map(Object::toString).collect(Collectors.joining(",", "", "")))
                .append(")");
        sb.append("[").append(index).append("]");
        if(constraint != null) {
            sb.append(" :- ").append(constraint);
        }
        sb.append(".");
        return sb.toString();
    }

    public static Optional<IndexedRuleApplication> of(Spec spec, Rule rule) throws Delay, InterruptedException {
        final IState.Transient state = State.of().melt();
        final Renaming.Builder _renaming = Renaming.builder();
        for(ITermVar freeVar : rule.freeVars()) {
            _renaming.put(freeVar, state.freshVar(freeVar));
        }
        final IRenaming renaming = _renaming.build();
        final IndexedRuleApplication newRule;
        try {
            if((newRule = of(state.freeze(), spec, rule.apply(renaming)).orElse(null)) == null) {
                return Optional.empty();
            }
        } catch(Delay d) {
            throw Delay.ofVars(d.vars().stream().map(renaming::rename).collect(Collectors.toList()));
        }
        return Optional.of(newRule.apply(renaming));
    }

    public static Optional<IndexedRuleApplication> of(IState.Immutable state, Spec spec, Rule rule)
            throws Delay, InterruptedException {
        final Set.Immutable<ITermVar> freeVars = rule.freeVars();

        final IState.Transient _state = state.melt();
        final List<ITermVar> args = new ArrayList<>();
        for (@SuppressWarnings("unused") Pattern param : rule.params()) {
            final ITermVar v = _state.freshVar(B.newVar("", "arg"));
            args.add(v);
        }
        final IState.Immutable newState = _state.freeze();

        final ApplyResult applyResult = RuleUtil.apply(
                newState.unifier(),
                rule,
                args,
                null,
                ApplyMode.RELAXED,
                Safety.SAFE,
                true
        ).orElse(null);
        if (applyResult == null) {
            return Optional.empty();
        }

        final IConstraint bodyAsConstraint = applyResult.body();
        final SolverResult<Empty> solveResult = Solver.solve(spec, newState, bodyAsConstraint, new NullDebugContext(),
                new NullCancel(), new NullProgress(), Solver.RETURN_ON_FIRST_ERROR);
        if (solveResult.hasErrors()) {
            return Optional.empty();
        }

        final SortedSet<ITermVar> indexVars = new TreeSet<>();
        final List<Pattern> newParams = new ArrayList<>();
        final java.util.Set<ITermVar> newParamVars = new HashSet<>();
        for (ITermVar arg : args) {
            final ITerm term = solveResult.state().unifier().findRecursive(arg);
            final Immutable<ITermVar> termVars = term.getVars();
            final Pattern param = P.fromTerm(term);
            indexVars.addAll(termVars.__retainAll(freeVars));
            newParams.add(param);
            newParamVars.addAll(termVars);
        }

        final ITerm index = B.newTuple(indexVars);

        final IndexedRuleApplication ira;
        if (solveResult.delays().isEmpty()) {
            ira = new IndexedRuleApplication(spec, newParams, null, index);
        } else {
            final IConstraint residualConstraint = solveResult.delayed();
            final Set.Immutable<ITermVar> newFreeVars = residualConstraint.freeVars().__removeAll(newParamVars);
            if (!newFreeVars.isEmpty()) {
                throw Delay.ofVars(newFreeVars);
            }
            ira = new IndexedRuleApplication(spec, newParams, residualConstraint, index);
        }

        return Optional.of(ira);
    }

}
