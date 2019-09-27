package mb.statix.random.strategy;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.oracle.truffle.api.object.dsl.Nullable;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.Diseq;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.util.Tuple2;
import mb.statix.constraints.CEqual;
import mb.statix.random.scopegraph.DataWF;
import mb.statix.random.util.RuleUtil;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IState;
import mb.statix.solver.completeness.IsComplete;
import mb.statix.solver.log.NullDebugContext;
import mb.statix.solver.persistent.Solver;
import mb.statix.solver.persistent.SolverResult;
import mb.statix.spec.Rule;

class ResolveDataWF implements DataWF<ITerm, CEqual> {
    private final IsComplete isComplete3;
    private final IState.Immutable state;
    private final Rule dataWf;
    private final IConstraint cause;

    ResolveDataWF(IsComplete isComplete3, IState.Immutable state, Rule dataWf, @Nullable IConstraint cause) {
        this.isComplete3 = isComplete3;
        this.state = state;
        this.dataWf = dataWf;
        this.cause = cause;
    }

    @Override public Optional<Optional<CEqual>> wf(ITerm datum) throws ResolutionException, InterruptedException {

        // apply rule
        final Optional<Tuple2<IState.Immutable, IConstraint>> stateAndConstraint =
                RuleUtil.apply(state, dataWf, ImmutableList.of(datum), null);
        if(!stateAndConstraint.isPresent()) {
            return Optional.empty();
        }

        // NOTE This method is almost a duplicate of Solver::entails and should be
        //      kept in sync

        // solve rule constraint
        final SolverResult result = Solver.solve(stateAndConstraint.get()._1(), stateAndConstraint.get()._2(),
                isComplete3, new NullDebugContext());
        if(result.hasErrors()) {
            return Optional.empty();
        }
        if(!result.delays().isEmpty()) {
            return Optional.empty();
        }

        final IState.Immutable newState = result.state();
        // NOTE The retain operation is important because it may change
        //      representatives, which can be local to newUnifier.
        final IUnifier.Immutable newUnifier = newState.unifier().retainAll(state.vars()).unifier();

        final Collection<ITermVar> disunifiedVars = newUnifier.disequalities().stream().map(Diseq::toTuple)
                .filter(diseq -> diseq.apply((t1, t2) -> state.unifier().areEqual(t1, t2).orElse(true)))
                .flatMap(diseq -> diseq.apply((t1, t2) -> Stream.concat(t1.getVars().stream(), t2.getVars().stream())))
                .collect(Collectors.toList());
        if(!disunifiedVars.isEmpty()) {
            return Optional.empty();
        }

        final Set<ITermVar> unifiedVars = Sets.difference(newUnifier.varSet(), state.unifier().varSet());
        // FIXME This test assumes the newUnifier is an extension of the old one.
        final List<ITerm> leftTerms = Lists.newArrayList();
        final List<ITerm> rightTerms = Lists.newArrayList();
        for(ITermVar var : unifiedVars) {
            final ITerm term = newUnifier.findTerm(var);
            if(!state.unifier().areEqual(var, term).orElse(false)) {
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