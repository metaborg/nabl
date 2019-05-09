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
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.substitution.ISubstitution.Immutable;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;
import mb.statix.solver.IConstraint;
import mb.statix.solver.Solver;
import mb.statix.solver.SolverResult;
import mb.statix.solver.State;
import mb.statix.solver.log.IDebugContext;
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

        final IMatcher<Tuple2<List<ITermVar>, List<IConstraint>>> constraintMatcher = M.tuple2(
                M.listElems(StatixTerms.varTerm()), StatixTerms.constraints(), (t, vs, c) -> ImmutableTuple2.of(vs, c));
        final Function1<Tuple2<List<ITermVar>, List<IConstraint>>, ITerm> solveConstraint =
                vars_constraint -> solveConstraint(spec, vars_constraint._1(), vars_constraint._2(), debug);
        // @formatter:off
        return M.cases(
            constraintMatcher.map(solveConstraint::apply),
            M.listElems(constraintMatcher).map(vars_constraints -> {
                return B.newList(vars_constraints.stream().parallel().map(solveConstraint::apply).collect(ImmutableList.toImmutableList()));
            })
        ).match(term);
        // @formatter:on
    }

    private ITerm solveConstraint(Spec spec, List<ITermVar> topLevelVars, Collection<IConstraint> constraints,
            IDebugContext debug) {
        State state = State.of(spec);

        final Tuple2<Immutable, State> freshVarsAndState = freshenToplevelVariables(topLevelVars, state);
        final ISubstitution.Immutable subst = freshVarsAndState._1();
        state = freshVarsAndState._2();

        constraints = constraints.stream().map(c -> c.apply(subst)).collect(ImmutableList.toImmutableList());

        final SolverResult resultConfig;
        try {
            resultConfig = Solver.solve(state, constraints, debug);
        } catch(InterruptedException e) {
            throw new RuntimeException(e);
        }
        final State resultState = resultConfig.state();
        final IUnifier.Immutable unifier = resultState.unifier();

        final List<ITerm> errorList = Lists.newArrayList();
        if(resultConfig.hasErrors()) {
            resultConfig.errors().stream().map(c -> makeMessage("Failed", c, unifier)).forEach(errorList::add);
        }

        final Collection<IConstraint> unsolved = resultConfig.delays().keySet();
        if(!unsolved.isEmpty()) {
            unsolved.stream().map(c -> makeMessage("Unsolved", c, unifier)).forEach(errorList::add);
        }

        final ITerm substTerm =
                StatixTerms.explicateMapEntries(toplevelSubstitution(topLevelVars, subst, resultState).entrySet());
        final ITerm solverTerm = B.newBlob(resultConfig.withDelays(ImmutableMap.of()).withErrors(ImmutableList.of()));
        final ITerm solveResultTerm = B.newAppl("Solution", substTerm, solverTerm);
        final IListTerm errors = B.newList(errorList);
        final IListTerm warnings = B.EMPTY_LIST;
        final IListTerm notes = B.EMPTY_LIST;
        final ITerm resultTerm = B.newTuple(solveResultTerm, errors, warnings, notes);

        return resultTerm;
    }

}