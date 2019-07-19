package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;

import com.google.inject.Inject;

import mb.nabl2.terms.ITerm;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.ISolverResult;
import mb.statix.taico.solver.MSolverResult;
import mb.statix.taico.util.TDebug;
import mb.statix.taico.util.TOverrides;
import mb.statix.taico.util.TTimings;

public class MSTX_solve_multi_project extends StatixPrimitive {

    @Inject public MSTX_solve_multi_project() {
        super(MSTX_solve_multi_project.class.getSimpleName(), 2);
    }
    
    @Override
    protected Optional<? extends ITerm> _call(IContext env, ITerm term, List<ITerm> terms) throws InterpreterException {
        TTimings.startPhase("MSTX_solve_multi_project", "Settings: " + TOverrides.print(), "Debug: " + TDebug.print());
        
        try {
            return super._call(env, term, terms);
        } finally {
            TTimings.endPhase("MSTX_solve_multi_project");
        }
    }

    @Override
    protected Optional<? extends ITerm> call(IContext env, ITerm term, List<ITerm> terms) throws InterpreterException {
        final MSolverResult initial = M.blobValue(MSolverResult.class).match(terms.get(0))
                .orElseThrow(() -> new InterpreterException("Expected solver result (MSolverResult), but got " + terms.get(0)));

        final List<ISolverResult> results = M.listElems(M.blobValue(ISolverResult.class)).match(term)
                .orElseThrow(() -> new InterpreterException("Expected list of solver results, but was " + term));

        if (TOverrides.OUTPUT_SCOPE_GRAPH_MULTI) TDebug.outputScopeGraph();
        
        final MSolverResult aggregate = aggregateResults(initial, results);
        final ITerm resultTerm = B.newBlob(aggregate);
        return Optional.of(resultTerm);
    }

    /**
     * Aggregates results of all the solvers into one SolverResult.
     * 
     * @param initialResult
     *      the initial solver result
     * @param results
     *      the solver results to aggregate
     * 
     * @return
     *      the aggregated results
     */
    public MSolverResult aggregateResults(MSolverResult initialResult, List<ISolverResult> results) {
        Set<IConstraint> errors = new LinkedHashSet<>();
        Map<IConstraint, Delay> delays = new LinkedHashMap<>();
        for (ISolverResult result : results) {
            errors.addAll(result.errors());
            delays.putAll(result.delays());
        }
        return MSolverResult.of(initialResult.state(), errors, delays, initialResult.existentials());
    }
}