package mb.statix.generator.strategy;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.task.NullCancel;
import org.metaborg.util.task.NullProgress;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import io.usethesource.capsule.Map;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.u.IUnifier;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.statix.constraints.CEqual;
import mb.statix.generator.scopegraph.DataWF;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IState;
import mb.statix.solver.completeness.ICompleteness;
import mb.statix.solver.log.NullDebugContext;
import mb.statix.solver.persistent.Solver;
import mb.statix.solver.persistent.SolverResult;
import mb.statix.spec.ApplyResult;
import mb.statix.spec.Rule;
import mb.statix.spec.RuleUtil;
import mb.statix.spec.Spec;

public class ResolveDataWF implements DataWF<ITerm, CEqual> {
    private final IState.Immutable state;
    private final ICompleteness.Immutable completeness;
    private final Rule dataWf;
    private final IConstraint cause;

    public ResolveDataWF(IState.Immutable state, ICompleteness.Immutable completeness, Rule dataWf, IConstraint cause) {
        this.state = state;
        this.completeness = completeness;
        this.dataWf = dataWf;
        this.cause = cause;
    }

    @Override public Optional<Optional<CEqual>> wf(Spec spec, ITerm datum) throws InterruptedException {
        final IUniDisunifier.Immutable unifier = state.unifier();

        // apply rule
        final ApplyResult applyResult;
        if((applyResult = RuleUtil.apply(state, dataWf, ImmutableList.of(datum), null).orElse(null)) == null) {
            return Optional.empty();
        }
        final IState.Immutable applyState = applyResult.state();
        final IConstraint applyConstraint = applyResult.body();

        // update completeness for new state and constraint
        final ICompleteness.Transient completeness = this.completeness.melt();
        completeness.updateAll(applyResult.diff().varSet(), applyState.unifier());
        completeness.add(applyConstraint, applyState.unifier());

        // NOTE This method is almost a duplicate of Solver::entails and should be
        //      kept in sync

        // solve rule constraint
        final SolverResult result =
                Solver.solve(spec, applyState, Iterables2.singleton(applyConstraint), Map.Immutable.of(),
                        completeness.freeze(), new NullDebugContext(), new NullProgress(), new NullCancel());
        if(result.hasErrors()) {
            return Optional.empty();
        }
        if(!result.delays().isEmpty()) {
            return Optional.empty();
        }

        final IState.Immutable newState = result.state();
        // NOTE The retain operation is important because it may change
        //      representatives, which can be local to newUnifier.
        final IUniDisunifier.Immutable newUnifier = newState.unifier().retainAll(state.vars()).unifier();

        // check that all (remaining) disequalities are implied (i.e., not unifiable) in the original unifier
        // @formatter:off
        final List<ITermVar> disunifiedVars = newUnifier.disequalities().stream()
                .filter(diseq -> diseq.toTuple().apply(unifier::disunify).map(r -> r.result().isPresent()).orElse(true))
                .flatMap(diseq -> diseq.varSet().stream())
                .collect(Collectors.toList());
        // @formatter:on
        if(!disunifiedVars.isEmpty()) {
            return Optional.empty();
        }

        final Set<ITermVar> unifiedVars = Sets.difference(newUnifier.varSet(), unifier.varSet());
        // FIXME This test assumes the newUnifier is an extension of the old one.
        //       Without this assumption, we should use the more expensive test
        //       `newUnifier.equals(state.unifier())`
        final List<ITerm> leftTerms = Lists.newArrayList();
        final List<ITerm> rightTerms = Lists.newArrayList();
        for(ITermVar var : unifiedVars) {
            final ITerm term = newUnifier.findTerm(var);
            if(!unifier.diff(var, term).map(IUnifier::isEmpty).orElse(false)) {
                leftTerms.add(var);
                rightTerms.add(term);
            }
        }
        if(!leftTerms.isEmpty()) {
            final CEqual eq = new CEqual(B.newTuple(leftTerms), B.newTuple(rightTerms), cause);
            return Optional.of(Optional.of(eq));
        }

        return Optional.of(Optional.empty());
    }

}
