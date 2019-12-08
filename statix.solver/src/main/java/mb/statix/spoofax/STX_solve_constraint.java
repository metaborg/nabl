package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;
import java.util.Optional;

import org.metaborg.util.functions.Function1;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.statix.constraints.CExists;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IState;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.persistent.Solver;
import mb.statix.solver.persistent.SolverResult;
import mb.statix.solver.persistent.State;
import mb.statix.spec.Spec;

public class STX_solve_constraint extends StatixPrimitive {

    @Inject public STX_solve_constraint() {
        super(STX_solve_constraint.class.getSimpleName(), 2);
    }

    @Override protected Optional<? extends ITerm> call(IContext env, ITerm term, List<ITerm> terms)
            throws InterpreterException {

        final ITerm specTerm = terms.get(0);
        final Spec spec = StatixTerms.spec().match(specTerm)
                .orElseThrow(() -> new InterpreterException("Expected spec, got " + specTerm));
        reportOverlappingRules(spec);

        final IDebugContext debug = getDebugContext(terms.get(1));

        final IMatcher<IConstraint> constraintMatcher = M.tuple2(M.listElems(StatixTerms.varTerm()),
                StatixTerms.constraint(), (t, vs, c) -> new CExists(vs, c));
        final Function1<IConstraint, ITerm> solveConstraint = constraint -> solveConstraint(spec, constraint, debug);
        // @formatter:off
        return M.cases(
            constraintMatcher.map(solveConstraint::apply),
            M.listElems(constraintMatcher).map(vars_constraints -> {
                return B.newList(vars_constraints.stream().parallel().map(solveConstraint::apply).collect(ImmutableList.toImmutableList()));
            })
        ).match(term);
        // @formatter:on
    }

    private ITerm solveConstraint(Spec spec, IConstraint constraint, IDebugContext debug) {
        final IState.Immutable state = State.of(spec);

        final SolverResult resultConfig;
        try {
            resultConfig = Solver.solve(spec, state, constraint, debug);
        } catch(InterruptedException e) {
            throw new RuntimeException(e);
        }

        final ITerm substTerm =
                StatixTerms.explicateMapEntries(resultConfig.existentials().entrySet(), resultConfig.state().unifier());
        final ITerm solverTerm = B.newBlob(resultConfig);
        final ITerm resultTerm = B.newAppl("Solution", substTerm, solverTerm);

        return resultTerm;
    }

}
