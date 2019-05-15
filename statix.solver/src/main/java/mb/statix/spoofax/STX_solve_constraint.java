package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.metaborg.util.functions.Function1;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.constraints.CExists;
import mb.statix.solver.IConstraint;
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

        final Spec spec =
                StatixTerms.spec().match(terms.get(0)).orElseThrow(() -> new InterpreterException("Expected spec."));
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
        final State state = State.of(spec);

        final SolverResult resultConfig;
        try {
            resultConfig = Solver.solve(state, constraint, debug);
        } catch(InterruptedException e) {
            throw new RuntimeException(e);
        }
        final State resultState = resultConfig.state();
        final IUnifier unifier = resultState.unifier();

        final List<ITerm> errorList = Lists.newArrayList();
        if(resultConfig.hasErrors()) {
            resultConfig.errors().stream().map(c -> makeMessage("Failed", c, unifier)).forEach(errorList::add);
        }

        final Collection<IConstraint> unsolved = resultConfig.delays().keySet();
        if(!unsolved.isEmpty()) {
            unsolved.stream().map(c -> makeMessage("Unsolved", c, unifier)).forEach(errorList::add);
        }

        final ITerm substTerm =
                StatixTerms.explicateMapEntries(resultConfig.existentials().entrySet(), resultConfig.state().unifier());
        final ITerm solverTerm = B.newBlob(resultConfig.withDelays(ImmutableMap.of()).withErrors(ImmutableList.of()));
        final ITerm solveResultTerm = B.newAppl("Solution", substTerm, solverTerm);
        final IListTerm errors = B.newList(errorList);
        final IListTerm warnings = B.EMPTY_LIST;
        final IListTerm notes = B.EMPTY_LIST;
        final ITerm resultTerm = B.newTuple(solveResultTerm, errors, warnings, notes);

        return resultTerm;
    }

}
