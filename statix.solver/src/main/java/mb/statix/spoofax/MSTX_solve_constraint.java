package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;
import static mb.statix.modular.util.TOverrides.*;

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
import mb.statix.modular.incremental.strategy.NonIncrementalStrategy;
import mb.statix.modular.module.IModule;
import mb.statix.modular.module.Module;
import mb.statix.modular.solver.Context;
import mb.statix.modular.solver.concurrent.ConcurrentSolverCoordinator;
import mb.statix.modular.solver.coordinator.ISolverCoordinator;
import mb.statix.modular.solver.coordinator.SolverCoordinator;
import mb.statix.modular.util.TDebug;
import mb.statix.modular.util.TOverrides;
import mb.statix.modular.util.TTimings;
import mb.statix.solver.IConstraint;
import mb.statix.solver.ISolverResult;
import mb.statix.solver.log.IDebugContext;
import mb.statix.spec.Spec;

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
        Optional<ITerm> tbr = M.cases(
            constraintMatcher.map(solveConstraint::apply),
            M.listElems(constraintMatcher).map(vars_constraints -> {
                return B.newList(vars_constraints.stream().map(solveConstraint::apply).collect(Collectors.toList()));
            })
        ).match(term);
        // @formatter:on
        
        if (OUTPUT_SCOPE_GRAPH_SINGLE) TDebug.outputScopeGraph();
        
        return tbr;
    }

    private ITerm solveConstraint(Spec spec, String resource, IConstraint constraint,
            IDebugContext debug) {
        //Create a context and a coordinator
        final Context context = Context.initialContext(new NonIncrementalStrategy(), spec);
        final ISolverCoordinator coordinator = CONCURRENT ? new ConcurrentSolverCoordinator(Executors.newWorkStealingPool(THREADS)) : new SolverCoordinator();
        context.setCoordinator(coordinator);
        
        //Create the top level module and state. It is added to the context automatically.
        final IModule module = Module.topLevelModule(resource);
        
        final ISolverResult resultConfig;
        try {
            resultConfig = coordinator.solve(module.getCurrentState(), constraint, debug);
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
