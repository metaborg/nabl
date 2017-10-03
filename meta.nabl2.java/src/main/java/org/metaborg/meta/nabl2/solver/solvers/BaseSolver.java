package org.metaborg.meta.nabl2.solver.solvers;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.config.NaBL2DebugConfig;
import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.ast.IAstConstraint;
import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.constraints.scopegraph.IScopeGraphConstraint;
import org.metaborg.meta.nabl2.controlflow.terms.CFGNode;
import org.metaborg.meta.nabl2.relations.variants.IVariantRelation;
import org.metaborg.meta.nabl2.relations.variants.VariantRelations;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopNameResolution;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.esop.reference.EsopScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.terms.Label;
import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.scopegraph.terms.Scope;
import org.metaborg.meta.nabl2.solver.ISolution;
import org.metaborg.meta.nabl2.solver.ISolver;
import org.metaborg.meta.nabl2.solver.ISolver.SeedResult;
import org.metaborg.meta.nabl2.solver.ISolver.SolveResult;
import org.metaborg.meta.nabl2.solver.ImmutableSolution;
import org.metaborg.meta.nabl2.solver.SolverConfig;
import org.metaborg.meta.nabl2.solver.SolverCore;
import org.metaborg.meta.nabl2.solver.SolverException;
import org.metaborg.meta.nabl2.solver.components.AstComponent;
import org.metaborg.meta.nabl2.solver.components.BaseComponent;
import org.metaborg.meta.nabl2.solver.components.ControlFlowComponent;
import org.metaborg.meta.nabl2.solver.components.EqualityComponent;
import org.metaborg.meta.nabl2.solver.components.NameResolutionComponent;
import org.metaborg.meta.nabl2.solver.components.NameResolutionComponent.NameResolutionResult;
import org.metaborg.meta.nabl2.solver.components.RelationComponent;
import org.metaborg.meta.nabl2.solver.components.ScopeGraphComponent;
import org.metaborg.meta.nabl2.solver.components.SymbolicComponent;
import org.metaborg.meta.nabl2.solver.messages.IMessages;
import org.metaborg.meta.nabl2.solver.messages.Messages;
import org.metaborg.meta.nabl2.stratego.TermIndex;
import org.metaborg.meta.nabl2.symbolic.ISymbolicConstraints;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.unification.IUnifier;
import org.metaborg.meta.nabl2.unification.Unifier;
import org.metaborg.meta.nabl2.util.collections.IProperties;
import org.metaborg.meta.nabl2.util.collections.Properties;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.functions.Predicate2;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import meta.flowspec.nabl2.controlflow.IControlFlowGraph;
import meta.flowspec.nabl2.controlflow.impl.ControlFlowGraph;

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
        final IUnifier.Transient unifier = Unifier.Transient.of();
        final IEsopScopeGraph.Transient<Scope, Label, Occurrence, ITerm> scopeGraph = EsopScopeGraph.Transient.of();

        // solver components
        final SolverCore core = new SolverCore(initial.config(), unifier::find, fresh, callExternal);
        final AstComponent astSolver = new AstComponent(core, Properties.Transient.of());
        final BaseComponent baseSolver = new BaseComponent(core);
        final EqualityComponent equalitySolver = new EqualityComponent(core, unifier);
        final ScopeGraphComponent scopeGraphSolver = new ScopeGraphComponent(core, scopeGraph);

        try {
            ISolver component =
                    c -> c.matchOrThrow(IConstraint.CheckedCases.<Optional<SolveResult>, InterruptedException>builder()
                    // @formatter:off
                    .onAst(astSolver::solve)
                    .onBase(baseSolver::solve)
                    .onEquality(equalitySolver::solve)
                    .onScopeGraph(scopeGraphSolver::solve)
                    .otherwise(cc -> Optional.empty())
                    // @formatter:on
                    );

            final FixedPointSolver solver = new FixedPointSolver(cancel, progress, component, Iterables2.empty());
            final SolveResult solveResult = solver.solve(initial.constraints());

            return ImmutableGraphSolution.of(initial.config(), astSolver.finish(), scopeGraphSolver.finish(),
                    equalitySolver.finish(), solveResult.messages(), solveResult.constraints());
        } catch(RuntimeException ex) {
            throw new SolverException("Internal solver error.", ex);
        }

    }

    public GraphSolution reportUnsolvedGraphConstraints(GraphSolution initial) {
        java.util.Set<IConstraint> graphConstraints = Sets.newHashSet();
        java.util.Set<IConstraint> otherConstraints = Sets.newHashSet();
        initial.constraints().stream().forEach(c -> {
            if(IAstConstraint.is(c) || IScopeGraphConstraint.is(c)) {
                graphConstraints.add(c);
            } else {
                otherConstraints.add(c);
            }
        });
        IMessages.Transient messages = initial.messages().melt();
        messages.addAll(Messages.unsolvedErrors(graphConstraints));
        return ImmutableGraphSolution.copyOf(initial).withMessages(messages.freeze()).withConstraints(otherConstraints);
    }

    public ISolution reportUnsolvedConstraints(ISolution initial) {
        IMessages.Transient messages = initial.messages().melt();
        messages.addAll(Messages.unsolvedErrors(initial.constraints()));
        return ImmutableSolution.builder().from(initial).messages(messages.freeze()).constraints(Collections.emptySet())
                .build();
    }

    public ISolution merge(ISolution initial, Iterable<? extends ISolution> solutions,
            Predicate2<Scope, Label> isEdgeComplete, IMessageInfo message) throws InterruptedException {

        final IUnifier.Transient unifier = initial.unifier().melt();
        final IEsopScopeGraph.Transient<Scope, Label, Occurrence, ITerm> scopeGraph = initial.scopeGraph().melt();
        final IEsopNameResolution.Transient<Scope, Label, Occurrence> nameResolution =
                initial.nameResolution().melt(scopeGraph, isEdgeComplete);

        final SolverCore core = new SolverCore(initial.config(), unifier::find, n -> {
            throw new IllegalStateException("Fresh is not available when merging.");
        }, callExternal);
        final AstComponent astSolver = new AstComponent(core, initial.astProperties().melt());
        final EqualityComponent equalitySolver = new EqualityComponent(core, unifier);
        final IProperties.Transient<Occurrence, ITerm, ITerm> properties = initial.declProperties().melt();
        final NameResolutionComponent nameResolutionSolver =
                new NameResolutionComponent(core, scopeGraph, nameResolution, properties);
        final RelationComponent relationSolver = new RelationComponent(core, r -> false,
                initial.config().getFunctions(), VariantRelations.melt(initial.relations()));
        final SymbolicComponent symSolver = new SymbolicComponent(core, initial.symbolic());
        final ControlFlowComponent cfgSolver = new ControlFlowComponent(core, ControlFlowGraph.of(), properties);

        final java.util.Set<IConstraint> constraints = Sets.newHashSet(initial.constraints());
        final IMessages.Transient messages = initial.messages().melt();
        for(ISolution solution : solutions) {
            seed(astSolver.seed(solution.astProperties(), message), messages, constraints);
            seed(equalitySolver.seed(solution.unifier(), message), messages, constraints);
            scopeGraph.addAll(solution.scopeGraph());
            seed(nameResolutionSolver.seed(solution.declProperties(), message), messages, constraints);
            seed(relationSolver.seed(solution.relations(), message), messages, constraints);
            seed(symSolver.seed(solution.symbolic(), message), messages, constraints);
        }
        nameResolutionSolver.update();
        cfgSolver.update();

        IProperties.Immutable<TermIndex, ITerm, ITerm> astResult = astSolver.finish();
        NameResolutionResult nameResult = nameResolutionSolver.finish();
        Map<String, IVariantRelation.Immutable<ITerm>> relationResult = relationSolver.finish();
        IUnifier.Immutable unifyResult = equalitySolver.finish();
        ISymbolicConstraints symbolicResult = symSolver.finish();
        IControlFlowGraph<CFGNode> cfg = cfgSolver.getControlFlowGraph();
        
        // TODO: add dataflow solver call here

        return ImmutableSolution.of(initial.config(), astResult, nameResult.scopeGraph(), nameResult.nameResolution(),
                nameResult.declProperties(), relationResult, unifyResult, symbolicResult, cfg, messages.freeze(),
                constraints);
    }

    protected boolean seed(SeedResult result, IMessages.Transient messages, Set<IConstraint> constraints) {
        boolean change = false;
        change |= messages.addAll(result.messages());
        change |= constraints.addAll(result.constraints());
        return change;
    }

    @Value.Immutable
    @Serial.Version(42l)
    public static abstract class BaseSolution {

        @Value.Parameter public abstract SolverConfig config();

        @Value.Parameter public abstract ImmutableSet<IConstraint> constraints();

    }

    @Value.Immutable
    @Serial.Version(42l)
    public static abstract class GraphSolution {

        @Value.Parameter public abstract SolverConfig config();

        @Value.Parameter public abstract IProperties.Immutable<TermIndex, ITerm, ITerm> astProperties();

        @Value.Parameter public abstract IEsopScopeGraph.Immutable<Scope, Label, Occurrence, ITerm> scopeGraph();

        @Value.Parameter public abstract IUnifier.Immutable unifier();

        @Value.Parameter public abstract IMessages.Immutable messages();

        @Value.Parameter public abstract ImmutableSet<IConstraint> constraints();

    }

}