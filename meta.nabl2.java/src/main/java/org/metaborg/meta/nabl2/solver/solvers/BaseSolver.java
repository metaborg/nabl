package org.metaborg.meta.nabl2.solver.solvers;

import java.util.Set;
import java.util.stream.Collectors;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.ast.IAstConstraint;
import org.metaborg.meta.nabl2.constraints.scopegraph.IScopeGraphConstraint;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.terms.Label;
import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.scopegraph.terms.Scope;
import org.metaborg.meta.nabl2.solver.ISolution;
import org.metaborg.meta.nabl2.solver.ISolver;
import org.metaborg.meta.nabl2.solver.ISolver.SolveResult;
import org.metaborg.meta.nabl2.solver.ImmutableSolution;
import org.metaborg.meta.nabl2.solver.SolverConfig;
import org.metaborg.meta.nabl2.solver.SolverCore;
import org.metaborg.meta.nabl2.solver.SolverException;
import org.metaborg.meta.nabl2.solver.components.AstComponent;
import org.metaborg.meta.nabl2.solver.components.BaseComponent;
import org.metaborg.meta.nabl2.solver.components.CompositeComponent;
import org.metaborg.meta.nabl2.solver.components.EqualityComponent;
import org.metaborg.meta.nabl2.solver.components.NameResolutionComponent;
import org.metaborg.meta.nabl2.solver.components.PolymorphismComponent;
import org.metaborg.meta.nabl2.solver.components.RelationComponent;
import org.metaborg.meta.nabl2.solver.components.ScopeGraphComponent;
import org.metaborg.meta.nabl2.solver.components.SetComponent;
import org.metaborg.meta.nabl2.solver.components.SymbolicComponent;
import org.metaborg.meta.nabl2.solver.messages.IMessages;
import org.metaborg.meta.nabl2.solver.messages.Messages;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.unification.IUnifier;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;

public class BaseSolver {

    protected final SolverConfig config;

    public BaseSolver(SolverConfig config) {
        this.config = config;
    }

    public ISolution solveGraph(ISolution initial, ICancel cancel, IProgress progress)
            throws SolverException, InterruptedException {

        // shared
        final IUnifier.Transient unifier = initial.unifier().melt();
        final IEsopScopeGraph.Transient<Scope, Label, Occurrence, ITerm> scopeGraph = initial.scopeGraph().melt();

        // solver components
        final SolverCore core = new SolverCore(config, unifier, n -> {
            throw new IllegalStateException("Fresh variables are not available when solving assumptions.");
        });
        final AstComponent astSolver = new AstComponent(core, initial.astProperties().melt());
        final BaseComponent baseSolver = new BaseComponent(core);
        final EqualityComponent equalitySolver = new EqualityComponent(core, unifier);
        final ScopeGraphComponent scopeGraphSolver = new ScopeGraphComponent(core, scopeGraph);
        final NameResolutionComponent nameResolutionSolver = new NameResolutionComponent(core, () -> false,
                (s, l) -> false, scopeGraph, initial.declProperties().melt());
        final PolymorphismComponent polySolver = new PolymorphismComponent(core, v -> true);
        final RelationComponent relationSolver =
                new RelationComponent(core, r -> false, config.getFunctions(), initial.relations().melt());
        final SetComponent setSolver = new SetComponent(core, nameResolutionSolver.nameSets());
        final SymbolicComponent symSolver = new SymbolicComponent(core, initial.symbolic());

        try {
            final CompositeComponent compositeSolver = new CompositeComponent(core, astSolver,
                    ISolver.defer(baseSolver), equalitySolver, ISolver.defer(nameResolutionSolver),
                    ISolver.defer(polySolver), ISolver.defer(relationSolver), ISolver.defer(setSolver),
                    scopeGraphSolver, ISolver.defer(symSolver));

            final FixedPointSolver solver = new FixedPointSolver(cancel, progress, compositeSolver, Iterables2.empty());
            final SolveResult solveResult = solver.solve(initial.constraints());

            final Set<IConstraint> assumptionConstraint = solveResult.constraints().stream()
                    .filter(c -> IAstConstraint.is(c) || IScopeGraphConstraint.is(c)).collect(Collectors.toSet());
            final Set<IConstraint> otherConstraints = solveResult.constraints().stream()
                    .filter(c -> !IScopeGraphConstraint.is(c)).collect(Collectors.toSet());

            final IMessages.Transient messages = initial.messages().melt();
            messages.addAll(Messages.unsolvedErrors(assumptionConstraint));
            messages.addAll(solveResult.messages());

            return ImmutableSolution.builder()
                    // @formatter:off
                    .from(compositeSolver.finish())
                    .messages(solveResult.messages())
                    .constraints(otherConstraints)
                    // @formatter:on
                    .build();
        } catch(RuntimeException ex) {
            throw new SolverException("Internal solver error.", ex);
        }

    }

}