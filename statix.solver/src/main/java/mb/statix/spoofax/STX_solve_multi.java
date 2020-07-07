package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.nabl2.util.Tuple2;
import mb.statix.concurrent.p_raffrayi.IBroker;
import mb.statix.concurrent.p_raffrayi.IResult;
import mb.statix.concurrent.p_raffrayi.IScopeImpl;
import mb.statix.concurrent.p_raffrayi.IUnitResult;
import mb.statix.concurrent.p_raffrayi.impl.Broker;
import mb.statix.concurrent.p_raffrayi.impl.ScopeImpl;
import mb.statix.concurrent.solver.ProjectTypeChecker;
import mb.statix.scopegraph.IScopeGraph;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.IState;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.log.LoggerDebugContext;
import mb.statix.solver.persistent.SolverResult;
import mb.statix.spec.Rule;
import mb.statix.spec.Spec;

public class STX_solve_multi extends StatixPrimitive {
    private static final ILogger logger = LoggerUtils.logger(STX_solve_multi.class);

    @Inject public STX_solve_multi() {
        super(STX_solve_multi.class.getSimpleName(), 4);
    }

    @Override protected Optional<? extends ITerm> call(IContext env, ITerm term, List<ITerm> terms)
            throws InterpreterException {

        final Spec spec =
                StatixTerms.spec().match(terms.get(0)).orElseThrow(() -> new InterpreterException("Expected spec."));
        reportOverlappingRules(spec);

        final IDebugContext debug = new LoggerDebugContext(logger); // getDebugContext(terms.get(1));
        final IProgress progress = getProgress(terms.get(2));
        final ICancel cancel = getCancel(terms.get(3));

        final IMatcher<Tuple2<String, Rule>> constraintMatcher =
                M.tuple2(M.stringValue(), StatixTerms.hoconstraint(), (t, r, c) -> Tuple2.of(r, c));
        final Map<String, Rule> units = M.listElems(constraintMatcher).match(term)
                .orElseThrow(() -> new InterpreterException("Expected list of constraints.")).stream()
                .collect(Collectors.toMap(Tuple2::_1, Tuple2::_2));

        final IScopeImpl<Scope> scopeImpl = new ScopeImpl();

        final IBroker<Scope, ITerm, ITerm, SolverResult> broker = new Broker<>(scopeImpl, spec.allLabels(), cancel);
        broker.add(".", new ProjectTypeChecker(units, spec, debug));

        final double t0 = System.currentTimeMillis();
        broker.run();

        final List<ITerm> results = Lists.newArrayList();
        try {
            final IResult<Scope, ITerm, ITerm, SolverResult> solveResult = broker.result().get();

            final double dt = System.currentTimeMillis() - t0;
            logger.info("Files analyzed in {} s", (dt / 1_000d));

            for(Entry<String, IUnitResult<Scope, ITerm, ITerm, SolverResult>> entry : solveResult.unitResults()
                    .entrySet()) {
                final SolverResult fileResult = entry.getValue().analysis();
                final IScopeGraph.Immutable<Scope, ITerm, ITerm> fileScopeGraph = entry.getValue().scopeGraph();
                final IState.Immutable updatedFileState = fileResult.state().withScopeGraph(fileScopeGraph);
                final SolverResult updatedFileResult = fileResult.withState(updatedFileState);
                results.add(B.newTuple(B.newString(entry.getKey()), B.newBlob(updatedFileResult)));
            }
        } catch(Throwable e) {
            logger.error("Async solving failed.", e);
            throw new InterpreterException("Async solving failed.", e);
        }

        return Optional.of(B.newList(results));
    }

}