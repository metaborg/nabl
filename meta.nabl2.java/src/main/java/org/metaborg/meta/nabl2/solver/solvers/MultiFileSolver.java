package org.metaborg.meta.nabl2.solver.solvers;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.constraints.scopegraph.IScopeGraphConstraint;
import org.metaborg.meta.nabl2.relations.IRelationName;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.terms.Label;
import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.scopegraph.terms.Scope;
import org.metaborg.meta.nabl2.solver.ISolution;
import org.metaborg.meta.nabl2.solver.ISolver;
import org.metaborg.meta.nabl2.solver.ImmutableSolution;
import org.metaborg.meta.nabl2.solver.Solution;
import org.metaborg.meta.nabl2.solver.SolverConfig;
import org.metaborg.meta.nabl2.solver.SolverCore;
import org.metaborg.meta.nabl2.solver.SolverException;
import org.metaborg.meta.nabl2.solver.ISolver.SeedResult;
import org.metaborg.meta.nabl2.solver.ISolver.SolveResult;
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
import org.metaborg.meta.nabl2.solver.properties.ActiveVars;
import org.metaborg.meta.nabl2.solver.properties.HasRelationBuildConstraints;
import org.metaborg.meta.nabl2.solver.properties.HasScopeGraphConstraints;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.unification.IUnifier;
import org.metaborg.meta.nabl2.util.functions.Function1;
import org.metaborg.meta.nabl2.util.functions.Predicate0;
import org.metaborg.meta.nabl2.util.functions.Predicate1;
import org.metaborg.meta.nabl2.util.functions.Predicate2;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;

import com.google.common.collect.Sets;

public class MultiFileSolver {

    private final SolverConfig config;

    public MultiFileSolver(SolverConfig config) {
        this.config = config;
    }

    public ISolution solveUnit(Iterable<? extends IConstraint> constraints, Collection<ITermVar> intfVars,
            Collection<Scope> intfScopes, Function1<String, ITermVar> fresh, ICancel cancel, IProgress progress)
            throws SolverException, InterruptedException {

        final ISolution initial = Solution.of(config);

        // shared
        final IUnifier.Transient unifier = initial.unifier().melt();
        final IEsopScopeGraph.Transient<Scope, Label, Occurrence, ITerm> scopeGraph = initial.scopeGraph().melt();

        // constraint set properties
        final ActiveVars activeVars = new ActiveVars(unifier);
        final HasScopeGraphConstraints hasScopeGraphConstraints = new HasScopeGraphConstraints();

        // guards
        final Predicate1<ITermVar> isVarInactive = v -> !(intfVars.contains(v) || activeVars.contains(v));
        final Predicate1<IRelationName> isRelationComplete = r -> false;
        final Predicate0 isGraphComplete = hasScopeGraphConstraints::isEmpty;
        final Predicate2<Scope, Label> isEdgeClosed = (s, l) -> !(intfScopes.contains(s) || scopeGraph.isOpen(s, l));

        // solver components
        final SolverCore core = new SolverCore(config, unifier, fresh);
        final AstComponent astSolver = new AstComponent(core, initial.astProperties().melt());
        final BaseComponent baseSolver = new BaseComponent(core);
        final EqualityComponent equalitySolver = new EqualityComponent(core, unifier);
        final ScopeGraphComponent scopeGraphSolver = new ScopeGraphComponent(core, scopeGraph);
        final NameResolutionComponent nameResolutionSolver = new NameResolutionComponent(core, isGraphComplete,
                isEdgeClosed, scopeGraph, initial.declProperties().melt());
        final PolymorphismComponent polySolver = new PolymorphismComponent(core, isVarInactive);
        final RelationComponent relationSolver =
                new RelationComponent(core, isRelationComplete, config.getFunctions(), initial.relations().melt());
        final SetComponent setSolver = new SetComponent(core, nameResolutionSolver.nameSets());
        final SymbolicComponent symSolver = new SymbolicComponent(core, initial.symbolic());

        try {
            final CompositeComponent compositeSolver =
                    new CompositeComponent(core, astSolver, baseSolver, equalitySolver, nameResolutionSolver,
                            polySolver, relationSolver, setSolver, scopeGraphSolver, symSolver);

            final IMessages.Transient messages = initial.messages().melt();

            final FixedPointSolver solver =
                    new FixedPointSolver(cancel, progress, compositeSolver, activeVars, hasScopeGraphConstraints);
            SolveResult solveResult = solver.solve(constraints, messages);

            solveResult.constraints().stream().filter(c -> IScopeGraphConstraint.is(c)).collect(Collectors.toSet());
            solveResult.constraints().stream().filter(c -> IScopeGraphConstraint.is(c)).collect(Collectors.toSet());

            return ImmutableSolution.builder()
                    // @formatter:off
                    .from(compositeSolver.finish())
                    .messages(solveResult.messages())
                    .constraints(solveResult.constraints())
                    // @formatter:on
                    .build();
        } catch(RuntimeException ex) {
            throw new SolverException("Internal solver error.", ex);
        }

    }

    public ISolution solveFinal(Iterable<? extends ISolution> unitSolutions, IMessageInfo message,
            Function1<String, ITermVar> fresh, ICancel cancel, IProgress progress)
            throws SolverException, InterruptedException {

        final ISolution initial = Solution.of(config);

        // shared
        final IUnifier.Transient unifier = initial.unifier().melt();
        final IEsopScopeGraph.Transient<Scope, Label, Occurrence, ITerm> scopeGraph = initial.scopeGraph().melt();

        // constraint set properties
        final ActiveVars activeVars = new ActiveVars(unifier);
        final HasRelationBuildConstraints hasRelationBuildConstraints = new HasRelationBuildConstraints();
        final HasScopeGraphConstraints hasScopeGraphConstraints = new HasScopeGraphConstraints();

        // guards
        final Predicate1<ITermVar> isVarInactive = v -> !activeVars.contains(v);
        final Predicate1<IRelationName> isRelationComplete = r -> !hasRelationBuildConstraints.contains(r);
        final Predicate0 isGraphComplete = hasScopeGraphConstraints::isEmpty;
        final Predicate2<Scope, Label> isEdgeClosed =
                (s, l) -> hasScopeGraphConstraints.isEmpty() && !scopeGraph.isOpen(s, l);

        // solver components
        final SolverCore core = new SolverCore(config, unifier, fresh);
        final AstComponent astSolver = new AstComponent(core, initial.astProperties().melt());
        final BaseComponent baseSolver = new BaseComponent(core);
        final EqualityComponent equalitySolver = new EqualityComponent(core, unifier);
        final ScopeGraphComponent scopeGraphSolver = new ScopeGraphComponent(core, scopeGraph);
        final NameResolutionComponent nameResolutionSolver = new NameResolutionComponent(core, isGraphComplete,
                isEdgeClosed, scopeGraph, initial.declProperties().melt());
        final PolymorphismComponent polySolver = new PolymorphismComponent(core, isVarInactive);
        final RelationComponent relationSolver =
                new RelationComponent(core, isRelationComplete, config.getFunctions(), initial.relations().melt());
        final SetComponent setSolver = new SetComponent(core, nameResolutionSolver.nameSets());
        final SymbolicComponent symSolver = new SymbolicComponent(core, initial.symbolic());

        try {
            final CompositeComponent compositeSolver =
                    new CompositeComponent(core, astSolver, baseSolver, equalitySolver, nameResolutionSolver,
                            polySolver, relationSolver, setSolver, ISolver.deny(scopeGraphSolver), symSolver);

            final IMessages.Transient messages = initial.messages().melt();
            final Set<IConstraint> constraints = Sets.newHashSet(initial.constraints());
            for(ISolution solution : unitSolutions) {
                final SeedResult seedResult = compositeSolver.seed(solution, message);
                messages.addAll(seedResult.messages());
                constraints.addAll(seedResult.constraints());
            }

            final FixedPointSolver solver = new FixedPointSolver(cancel, progress, compositeSolver, activeVars,
                    hasRelationBuildConstraints, hasScopeGraphConstraints);
            SolveResult result = solver.solve(constraints, messages);

            return ImmutableSolution.builder()
                    // @formatter:off
                    .from(compositeSolver.finish())
                    .messages(result.messages())
                    .constraints(result.constraints())
                    // @formatter:on
                    .build();
        } catch(RuntimeException ex) {
            throw new SolverException("Internal solver error.", ex);
        }
    }

}