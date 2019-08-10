package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.stratego.TermIndex;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.OccursException;
import mb.nabl2.util.Tuple2;
import mb.statix.constraints.Constraints;
import mb.statix.modular.util.TDebug;
import mb.statix.modular.util.TOverrides;
import mb.statix.modular.util.TTimings;
import mb.statix.scopegraph.IScopeGraph;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.IConstraint;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.persistent.Solver;
import mb.statix.solver.persistent.SolverResult;
import mb.statix.solver.persistent.State;

public class STX_solve_multi_project extends StatixPrimitive {
    private static final ILogger logger = LoggerUtils.logger(STX_solve_multi_project.class);

    @Inject public STX_solve_multi_project() {
        super(STX_solve_multi_project.class.getSimpleName(), 2);
    }
    
    @Override
    protected Optional<? extends ITerm> _call(IContext env, ITerm term, List<ITerm> terms) throws InterpreterException {
        TTimings.startPhase("STX_solve_multi_project", "Settings: " + TOverrides.print(), "Debug: " + TDebug.print(), "Input: " + term.toString());
        
        try {
            return super._call(env, term, terms);
        } finally {
            TTimings.endPhase("STX_solve_multi_project");
        }
    }

    @Override protected Optional<? extends ITerm> call(IContext env, ITerm term, List<ITerm> terms)
            throws InterpreterException {

        TTimings.startPhase("init");
        final SolverResult initial = M.blobValue(SolverResult.class).match(terms.get(0))
                .orElseThrow(() -> new InterpreterException("Expected solver result."));

        final IDebugContext debug = getDebugContext(terms.get(1));

        final List<SolverResult> results = M.listElems(M.blobValue(SolverResult.class)).match(term)
                .orElseThrow(() -> new InterpreterException("Expected list of solver results."));

        final List<IConstraint> constraints = new ArrayList<>(initial.delays().keySet());
        final List<IConstraint> errors = new ArrayList<>(initial.errors());
        State state = initial.state();
        final ImmutableMap.Builder<Tuple2<TermIndex, ITerm>, ITerm> termProperties = ImmutableMap.builder();
        termProperties.putAll(state.termProperties());
        final IUnifier.Transient unifier = state.unifier().melt();
        final IScopeGraph.Transient<Scope, ITerm, ITerm> scopeGraph = state.scopeGraph().melt();
        for(SolverResult result : results) {
            state = state.add(result.state());
            constraints.add(result.delayed());
            errors.addAll(result.errors());
            try {
                unifier.unify(result.state().unifier());
            } catch(OccursException e) {
                // can this ever occur?
                return Optional.empty();
            }
            scopeGraph.addAll(result.state().scopeGraph());
            termProperties.putAll(result.state().termProperties());
        }
        // @formatter:off
        state = state.withUnifier(unifier.freeze())
                     .withScopeGraph(scopeGraph.freeze())
                     .withTermProperties(termProperties.build());
        // @formatter:on
        TTimings.endPhase("init");
        TTimings.startPhase("solving");

        final SolverResult resultConfig;
        try {
            final double t0 = System.currentTimeMillis();
            resultConfig = Solver.solve(state, Constraints.conjoin(constraints), (s, l, st) -> true, debug);
            final double dt = System.currentTimeMillis() - t0;
            logger.info("Project analyzed in {} s", (dt / 1_000d));
        } catch(InterruptedException e) {
            throw new RuntimeException(e);
        }
        TTimings.endPhase("solving");
        
        errors.addAll(resultConfig.errors());
        final ITerm resultTerm = B.newBlob(resultConfig.withErrors(errors));
        return Optional.of(resultTerm);
    }

}