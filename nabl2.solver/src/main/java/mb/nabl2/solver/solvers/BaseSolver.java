package mb.nabl2.solver.solvers;

import java.util.List;
import java.util.Set;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.util.Ref;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;

import com.google.common.collect.ImmutableSet;

import io.usethesource.capsule.Set.Immutable;
import mb.nabl2.config.NaBL2DebugConfig;
import mb.nabl2.constraints.IConstraint;
import mb.nabl2.scopegraph.ScopeGraphReducer;
import mb.nabl2.scopegraph.esop.CriticalEdge;
import mb.nabl2.scopegraph.esop.IEsopScopeGraph;
import mb.nabl2.scopegraph.esop.reference.EsopScopeGraph;
import mb.nabl2.scopegraph.terms.Label;
import mb.nabl2.scopegraph.terms.Occurrence;
import mb.nabl2.scopegraph.terms.Scope;
import mb.nabl2.solver.ISolver;
import mb.nabl2.solver.SeedResult;
import mb.nabl2.solver.SolveResult;
import mb.nabl2.solver.SolverConfig;
import mb.nabl2.solver.SolverCore;
import mb.nabl2.solver.components.AstComponent;
import mb.nabl2.solver.components.BaseComponent;
import mb.nabl2.solver.components.EqualityComponent;
import mb.nabl2.solver.components.ExternalRelationComponent;
import mb.nabl2.solver.components.ScopeGraphComponent;
import mb.nabl2.solver.exceptions.DelayException;
import mb.nabl2.solver.exceptions.SolverException;
import mb.nabl2.solver.messages.IMessages;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.stratego.TermIndex;
import mb.nabl2.terms.unification.u.IUnifier;
import mb.nabl2.util.collections.IProperties;
import mb.nabl2.util.collections.Properties;

public class BaseSolver {

    protected final NaBL2DebugConfig nabl2Debug;
    protected final CallExternal callExternal;

    public BaseSolver(NaBL2DebugConfig nabl2Debug, CallExternal callExternal) {
        this.nabl2Debug = nabl2Debug;
        this.callExternal = callExternal;
    }

    public GraphSolution solveGraph(BaseSolution initial, Function1<String, String> fresh, ICancel cancel,
            IProgress progress) throws SolverException, InterruptedException {

        // shared
        final Ref<IUnifier.Immutable> unifier = new Ref<>(initial.unifier());
        final IEsopScopeGraph.Transient<Scope, Label, Occurrence, ITerm> scopeGraph = EsopScopeGraph.Transient.of();
        final ScopeGraphReducer scopeGraphReducer = new ScopeGraphReducer(scopeGraph, unifier);

        // solver components
        final SolverCore core = new SolverCore(initial.config(), unifier, fresh, callExternal, cancel, progress);
        final AstComponent astSolver = new AstComponent(core, Properties.Transient.of());
        final BaseComponent baseSolver = new BaseComponent(core);
        final EqualityComponent equalitySolver = new EqualityComponent(core, unifier);
        final ScopeGraphComponent scopeGraphSolver = new ScopeGraphComponent(core, scopeGraph);
        final ExternalRelationComponent relationSolver = new ExternalRelationComponent(core);

        try {
            // @formatter:off
            ISolver component = c -> c.matchOrThrow(IConstraint.CheckedCases.<SolveResult, DelayException>builder()
                .onAst(astSolver::solve)
                .onBase(baseSolver::solve)
                .onEquality(equalitySolver::solve)
                .onScopeGraph(scopeGraphSolver::solve)
                .onRelation(relationSolver::solve)
                .otherwise(ISolver.defer())
            );
            // @formatter:on

            final FixedPointSolver solver = new FixedPointSolver(cancel, progress, component);

            solver.step().subscribe(r -> {
                final Immutable<ITermVar> vars = r.result.unifierDiff().varSet();
                if(!vars.isEmpty()) {
                    try {
                        final List<CriticalEdge> criticalEdges = scopeGraphReducer.update(vars);
                        r.resolveCriticalEdges(criticalEdges);
                    } catch(InterruptedException ex) {
                        // ignore here
                    }
                }
            });

            scopeGraphReducer.updateAll();
            final SolveResult solveResult = solver.solve(initial.constraints(), unifier);

            return GraphSolution.of(initial.config(), astSolver.finish(), scopeGraphSolver.finish(),
                    equalitySolver.finish(), solveResult.messages(), solveResult.constraints());
        } catch(RuntimeException ex) {
            throw new SolverException("Internal solver error.", ex);
        }

    }

    protected boolean seed(SeedResult result, IMessages.Transient messages, Set<IConstraint> constraints) {
        boolean change = false;
        change |= messages.addAll(result.messages());
        change |= constraints.addAll(result.constraints());
        return change;
    }

    @Value.Immutable
    @Serial.Version(value = 42L)
    public static abstract class ABaseSolution {

        @Value.Parameter public abstract SolverConfig config();

        @Value.Parameter public abstract ImmutableSet<IConstraint> constraints();

        @Value.Parameter public abstract IUnifier.Immutable unifier();

    }

    @Value.Immutable
    @Serial.Version(value = 42L)
    public static abstract class AGraphSolution {

        @Value.Parameter public abstract SolverConfig config();

        @Value.Parameter public abstract IProperties.Immutable<TermIndex, ITerm, ITerm> astProperties();

        @Value.Parameter public abstract IEsopScopeGraph.Immutable<Scope, Label, Occurrence, ITerm> scopeGraph();

        @Value.Parameter public abstract IUnifier.Immutable unifier();

        @Value.Parameter public abstract IMessages.Immutable messages();

        @Value.Parameter public abstract ImmutableSet<IConstraint> constraints();

    }

}
