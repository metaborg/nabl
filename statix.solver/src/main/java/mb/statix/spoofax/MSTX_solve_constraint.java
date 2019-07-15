package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;
import static mb.statix.taico.util.TOverrides.*;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.metaborg.util.functions.Function1;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;

import com.google.inject.Inject;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;
import mb.statix.constraints.CExists;
import mb.statix.solver.IConstraint;
import mb.statix.solver.ISolverResult;
import mb.statix.solver.log.IDebugContext;
import mb.statix.spec.Spec;
import mb.statix.taico.incremental.strategy.NonIncrementalStrategy;
import mb.statix.taico.module.IModule;
import mb.statix.taico.module.Module;
import mb.statix.taico.solver.SolverContext;
import mb.statix.taico.solver.concurrent.ConcurrentSolverCoordinator;
import mb.statix.taico.solver.coordinator.ASolverCoordinator;
import mb.statix.taico.solver.coordinator.SolverCoordinator;
import mb.statix.taico.solver.state.MState;
import mb.statix.taico.util.TDebug;
import mb.statix.taico.util.TOverrides;
import mb.statix.taico.util.TTimings;

public class MSTX_solve_constraint extends StatixPrimitive {
    @Inject public MSTX_solve_constraint() {
        super(MSTX_solve_constraint.class.getSimpleName(), 2);
    }
    
    @Override
    protected Optional<? extends ITerm> _call(IContext env, ITerm term, List<ITerm> terms) throws InterpreterException {
        TTimings.startNewRun();
        TTimings.startPhase("MSTX_solve_constraint", "Settings: " + TOverrides.print(), "Debug: " + TDebug.print(), "Spec: " + terms.get(0).hashCode(), "Input: " + term.toString());
        
        try {
            return super._call(env, term, terms);
        } finally {
            TTimings.endPhase("MSTX_solve_constraint");
        }
    }

    @Override protected Optional<? extends ITerm> call(IContext env, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        TTimings.startPhase("init");
        final Spec spec =
                StatixTerms.spec().match(terms.get(0)).orElseThrow(() -> new InterpreterException("Expected spec, but was " + terms.get(0)));
        reportOverlappingRules(spec);

        final IDebugContext debug = getDebugContext(terms.get(1));

        final IMatcher<Tuple2<String, IConstraint>> constraintMatcher =
                M.tuple3(M.stringValue(), M.listElems(StatixTerms.varTerm()), StatixTerms.constraint(),
                        (t, r, vs, c) -> ImmutableTuple2.of(r, new CExists(vs, c)));
        
        final Function1<Tuple2<String, IConstraint>, ITerm> solveConstraint =
                res_vars_constraint -> solveConstraint(spec, res_vars_constraint._1(), res_vars_constraint._2(), debug);
        
        TTimings.endPhase("init");
        
        // @formatter:off
        return M.cases(
            constraintMatcher.map(solveConstraint::apply),
            M.listElems(constraintMatcher).map(vars_constraints -> {
                return B.newList(vars_constraints.stream().map(solveConstraint::apply).collect(Collectors.toList()));
            })
        ).match(term);
        // @formatter:on
//        final Tuple3<String, List<ITermVar>, Set<IConstraint>> vars_constraint = M
//                .tuple3(M.stringValue(), M.listElems(StatixTerms.varTerm()), StatixTerms.constraints(spec.labels()),
//                        (t, r, vs, c) -> ImmutableTuple3.of(r, vs, c))
//                .match(term).orElseThrow(() -> new InterpreterException("Expected constraint."));
//
//        final ITerm resultTerm = solveConstraint(spec, vars_constraint._1(), vars_constraint._2(), vars_constraint._3(), debug);
//        return Optional.of(resultTerm);
    }

    private ITerm solveConstraint(Spec spec, String resource, IConstraint constraint,
            IDebugContext debug) {
        //Create a context and a coordinator
        final SolverContext context = SolverContext.initialContext(new NonIncrementalStrategy(), spec);
        final ASolverCoordinator coordinator = CONCURRENT ? new ConcurrentSolverCoordinator(Executors.newWorkStealingPool(THREADS)) : new SolverCoordinator();
        context.setCoordinator(coordinator);
        
        //Create the top level module and state. It is added to the context automatically.
        final IModule module = new Module(resource);
        final MState state = new MState(module);
        
        final ISolverResult resultConfig;
        try {
            resultConfig = coordinator.solve(state, constraint, debug);
        } catch(InterruptedException e) {
            throw new RuntimeException(e);
        }
        
        final ITerm substTerm =
                StatixTerms.explicateMapEntries(resultConfig.existentials().entrySet(), resultConfig.unifier());
        final ITerm solverTerm = B.newBlob(resultConfig);
        final ITerm resultTerm = B.newAppl("Solution", substTerm, solverTerm);

        return resultTerm;
    }
}
