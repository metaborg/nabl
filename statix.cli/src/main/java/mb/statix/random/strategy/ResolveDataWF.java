package mb.statix.random.strategy;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.oracle.truffle.api.object.dsl.Nullable;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
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
        final IState.Immutable newState = stateAndConstraint.get()._1();
        final IConstraint constraint = stateAndConstraint.get()._2();

        // solve rule constraint
        final SolverResult result = Solver.solve(newState, constraint, isComplete3, new NullDebugContext());
        if(result.hasErrors()) {
            return Optional.empty();
        }
        if(!result.delays().isEmpty()) {
            return Optional.empty();
        }

        final List<ITerm> leftTerms = Lists.newArrayList();
        final List<ITerm> rightTerms = Lists.newArrayList();
        // NOTE The retain operation is important because it may change
        //      representatives, which can be local to newUnifier.
        final IUnifier.Immutable newUnifier = result.state().unifier().retainAll(state.vars()).unifier();
        for(ITermVar var : state.vars()) {
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