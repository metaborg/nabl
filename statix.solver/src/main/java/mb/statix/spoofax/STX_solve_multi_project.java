package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;

import com.google.common.collect.Maps;
import com.google.inject.Inject;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.stratego.TermIndex;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.OccursException;
import mb.nabl2.util.collections.HashTrieRelation3;
import mb.nabl2.util.collections.IRelation3;
import mb.statix.constraints.Constraints;
import mb.statix.constraints.messages.IMessage;
import mb.statix.scopegraph.IScopeGraph;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IState;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.persistent.Solver;
import mb.statix.solver.persistent.SolverResult;
import mb.statix.spec.Spec;

public class STX_solve_multi_project extends StatixPrimitive {
    private static final ILogger logger = LoggerUtils.logger(STX_solve_multi_project.class);

    @Inject public STX_solve_multi_project() {
        super(STX_solve_multi_project.class.getSimpleName(), 2);
    }

    @Override protected Optional<? extends ITerm> call(IContext env, ITerm term, List<ITerm> terms)
            throws InterpreterException {

        final Spec spec =
                StatixTerms.spec().match(terms.get(0)).orElseThrow(() -> new InterpreterException("Expected spec."));
        reportOverlappingRules(spec);

        final SolverResult initial = M.blobValue(SolverResult.class).match(terms.get(1))
                .orElseThrow(() -> new InterpreterException("Expected solver result."));

        final IDebugContext debug = getDebugContext(terms.get(2));

        final List<SolverResult> results = M.listElems(M.blobValue(SolverResult.class)).match(term)
                .orElseThrow(() -> new InterpreterException("Expected list of solver results."));

        final List<IConstraint> constraints = new ArrayList<>(initial.delays().keySet());
        final Map<IConstraint, IMessage> messages = Maps.newHashMap(initial.messages());
        IState.Immutable state = initial.state();
        final IRelation3.Transient<TermIndex, ITerm, ITerm> termProperties = HashTrieRelation3.Transient.of();
        termProperties.putAll(state.termProperties());
        IUnifier.Immutable unifier = state.unifier();
        final IScopeGraph.Transient<Scope, ITerm, ITerm> scopeGraph = state.scopeGraph().melt();
        for(SolverResult result : results) {
            state = state.add(result.state());
            constraints.add(result.delayed());
            messages.putAll(result.messages());
            try {
                final Optional<IUnifier.Immutable.Result<IUnifier.Immutable>> unifyResult =
                        unifier.unify(result.state().unifier());
                if(!unifyResult.isPresent()) {
                    return Optional.empty();
                }
                unifier = unifyResult.get().unifier();
            } catch(OccursException e) {
                // can this ever occur?
                return Optional.empty();
            }
            scopeGraph.addAll(result.state().scopeGraph());
            termProperties.putAll(result.state().termProperties());
        }
        // @formatter:off
        state = state.withUnifier(unifier)
                     .withScopeGraph(scopeGraph.freeze())
                     .withTermProperties(termProperties.freeze());
        // @formatter:on

        final SolverResult resultConfig;
        try {
            final double t0 = System.currentTimeMillis();
            resultConfig = Solver.solve(spec, state, Constraints.conjoin(constraints), (s, l, st) -> true, debug);
            final double dt = System.currentTimeMillis() - t0;
            logger.info("Project analyzed in {} s", (dt / 1_000d));
        } catch(InterruptedException e) {
            throw new RuntimeException(e);
        }
        messages.putAll(resultConfig.messages());
        final ITerm resultTerm = B.newBlob(resultConfig.withMessages(messages));
        return Optional.of(resultTerm);
    }

}