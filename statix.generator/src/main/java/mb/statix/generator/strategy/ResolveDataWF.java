package mb.statix.generator.strategy;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.collection.ImList;
import org.metaborg.util.collection.Sets;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.task.NullCancel;
import org.metaborg.util.task.NullProgress;

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
import mb.statix.solver.completeness.IsComplete;
import mb.statix.solver.log.NullDebugContext;
import mb.statix.solver.persistent.Solver;
import mb.statix.solver.persistent.SolverResult;
import mb.statix.spec.ApplyMode;
import mb.statix.spec.ApplyMode.Safety;
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
        // UNSAFE : we assume the resource of spec variables is empty and of state variables non-empty
        final ApplyResult applyResult = RuleUtil.apply(
                state.unifier(),
                dataWf,
                ImList.Immutable.of(datum),
                null,
                ApplyMode.RELAXED,
                Safety.UNSAFE,
                true
        ).orElse(null);
        if (applyResult == null) {
            return Optional.empty();
        }
        final IState.Immutable applyState = state;
        final IConstraint applyConstraint = applyResult.body();

        // update completeness for new state and constraint
        final ICompleteness.Transient completeness = this.completeness.melt();
        completeness.add(applyConstraint, spec, applyState.unifier());

        // NOTE This part is almost a duplicate of Solver::entails and should be
        //      kept in sync

        // no substate is used here, as we allow variables from the context to be unified
        final SolverResult result = Solver.solve(spec, applyState, Iterables2.singleton(applyConstraint),
                CapsuleUtil.immutableMap(), completeness.freeze(), IsComplete.ALWAYS, new NullDebugContext(),
                new NullProgress(), new NullCancel(), Solver.RETURN_ON_FIRST_ERROR);

        // NOTE This part is almost a duplicate of Solver::entailed and should be
        //      kept in sync

        if (result.hasErrors()) {
            return Optional.empty();
        }

        final IState.Immutable newState = result.state();

        if (!result.delays().isEmpty()) {
            return Optional.empty();
        }

        // NOTE The retain operation is important because it may change
        //      representatives, which can be local to newUnifier.
        final IUniDisunifier.Immutable newUnifier = newState.unifier().retainAll(state.vars()).unifier();

        // check that all (remaining) disequalities are implied (i.e., not unifiable) in the original unifier
        // @formatter:off
        final List<ITermVar> disunifiedVars = newUnifier.disequalities().stream()
                .filter(diseq -> diseq.toTuple().apply(unifier::disunify).map(r -> r.result().isPresent()).orElse(true))
                .flatMap(diseq -> diseq.domainSet().stream())
                .collect(Collectors.toList());
        // @formatter:on
        if (!disunifiedVars.isEmpty()) {
            return Optional.empty();
        }

        // FIXME This test assumes the newUnifier is an extension of the old one.
        //       Without this assumption, we should use the more expensive test
        //       `newUnifier.equals(state.unifier())`
        final List<ITerm> leftTerms = new ArrayList<>();
        final List<ITerm> rightTerms = new ArrayList<>();
        for (ITermVar var : Sets.difference(newUnifier.domainSet(), unifier.domainSet())) {
            final ITerm term = newUnifier.findTerm(var);
            if (!unifier.diff(var, term).map(IUnifier::isEmpty).orElse(false)) {
                leftTerms.add(var);
                rightTerms.add(term);
            }
        }
        if (!leftTerms.isEmpty()) {
            final CEqual eq = new CEqual(B.newTuple(leftTerms), B.newTuple(rightTerms), cause);
            return Optional.of(Optional.of(eq));
        }

        return Optional.of(Optional.empty());
    }

}
