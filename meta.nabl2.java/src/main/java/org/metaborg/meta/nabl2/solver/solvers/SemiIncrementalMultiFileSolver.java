package org.metaborg.meta.nabl2.solver.solvers;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.relations.IRelationName;
import org.metaborg.meta.nabl2.relations.IRelations;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopNameResolution;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.esop.reference.EsopNameResolution;
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
import org.metaborg.meta.nabl2.solver.components.EqualityComponent;
import org.metaborg.meta.nabl2.solver.components.ImmutableNameResolutionResult;
import org.metaborg.meta.nabl2.solver.components.NameResolutionComponent;
import org.metaborg.meta.nabl2.solver.components.NameResolutionComponent.NameResolutionResult;
import org.metaborg.meta.nabl2.solver.components.PolymorphismComponent;
import org.metaborg.meta.nabl2.solver.components.RelationComponent;
import org.metaborg.meta.nabl2.solver.components.SetComponent;
import org.metaborg.meta.nabl2.solver.components.SymbolicComponent;
import org.metaborg.meta.nabl2.solver.messages.IMessages;
import org.metaborg.meta.nabl2.solver.properties.ActiveVars;
import org.metaborg.meta.nabl2.solver.properties.HasRelationBuildConstraints;
import org.metaborg.meta.nabl2.stratego.TermIndex;
import org.metaborg.meta.nabl2.symbolic.ISymbolicConstraints;
import org.metaborg.meta.nabl2.symbolic.SymbolicConstraints;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.unification.IUnifier;
import org.metaborg.meta.nabl2.unification.Unifier;
import org.metaborg.meta.nabl2.util.collections.IProperties;
import org.metaborg.meta.nabl2.util.collections.Properties;
import org.metaborg.meta.nabl2.util.functions.Function1;
import org.metaborg.meta.nabl2.util.functions.Predicate1;
import org.metaborg.meta.nabl2.util.functions.Predicate2;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;

import com.google.common.collect.Sets;

public class SemiIncrementalMultiFileSolver extends BaseSolver {

    public ISolution solveUnit(GraphSolution initial, Collection<ITermVar> intfVars, Collection<Scope> intfScopes,
            Function1<String, ITermVar> fresh, ICancel cancel, IProgress progress)
            throws SolverException, InterruptedException {
        final SolverConfig config = initial.config();

        // shared
        final IUnifier.Transient unifier = Unifier.Transient.of();
        final IEsopScopeGraph.Transient<Scope, Label, Occurrence, ITerm> scopeGraph = initial.scopeGraph().melt();

        // constraint set properties
        final ActiveVars activeVars = new ActiveVars(unifier);
        intfVars.stream().forEach(activeVars::add);

        // guards
        final Predicate1<ITerm> isTermInactive = v -> !activeVars.contains(v);
        final Predicate1<IRelationName> isRelationComplete = r -> false;
        final Predicate2<Scope, Label> isEdgeClosed = (s, l) -> !intfScopes.contains(s);

        // solver components
        final SolverCore core = new SolverCore(config, unifier::find, fresh);
        final BaseComponent baseSolver = new BaseComponent(core);
        final EqualityComponent equalitySolver = new EqualityComponent(core, unifier);
        final NameResolutionComponent nameResolutionSolver = new NameResolutionComponent(core, scopeGraph,
                EsopNameResolution.Transient.of(config.getResolutionParams(), scopeGraph, isEdgeClosed),
                Properties.Transient.of());
        final PolymorphismComponent polySolver = new PolymorphismComponent(core, isTermInactive);
        final RelationComponent relationSolver =
                new RelationComponent(core, isRelationComplete, config.getFunctions(), config.getRelations().melt());
        final SetComponent setSolver = new SetComponent(core, nameResolutionSolver.nameSets());
        final SymbolicComponent symSolver = new SymbolicComponent(core, SymbolicConstraints.of());

        final ISolver component =
                c -> c.matchOrThrow(IConstraint.CheckedCases.<Optional<SolveResult>, InterruptedException>builder()
                        // @formatter:off
                        .onBase(baseSolver::solve)
                        .onEquality(equalitySolver::solve)
                        .onNameResolution(nameResolutionSolver::solve)
                        .onPoly(polySolver::solve)
                        .onRelation(relationSolver::solve)
                        .onSet(setSolver::solve)
                        .onSym(symSolver::solve)
                        .otherwise(ISolver.deny("Not allowed in this phase"))
                        // @formatter:on
                );
        final FixedPointSolver solver = new FixedPointSolver(cancel, progress, component, Iterables2.from(activeVars));

        solver.step().subscribe(r -> {
            if(!r.unifiedVars().isEmpty()) {
                try {
                    nameResolutionSolver.update();
                } catch(InterruptedException ex) {
                    // ignore here
                }
            }
        });

        try {
            nameResolutionSolver.update();
            final SolveResult solveResult = solver.solve(initial.constraints());

            NameResolutionResult nameResolutionResult = nameResolutionSolver.finish();
            IUnifier.Immutable unifierResult = equalitySolver.finish();
            IRelations.Immutable<ITerm> relationResult = relationSolver.finish();
            ISymbolicConstraints symbolicConstraints = symSolver.finish();

            return ImmutableSolution.of(config, initial.astProperties(), nameResolutionResult.scopeGraph(),
                    nameResolutionResult.nameResolution(), nameResolutionResult.declProperties(), relationResult,
                    unifierResult, symbolicConstraints, solveResult.messages(), solveResult.constraints());
        } catch(RuntimeException ex) {
            throw new SolverException("Internal solver error.", ex);
        }

    }

    public ISolution solveFinal(ISolution initial, Iterable<? extends ISolution> unitSolutions, IMessageInfo message,
            Function1<String, ITermVar> fresh, ICancel cancel, IProgress progress)
            throws SolverException, InterruptedException {
        final SolverConfig config = initial.config();

        // shared
        final IUnifier.Transient unifier = initial.unifier().melt();
        final IEsopScopeGraph.Transient<Scope, Label, Occurrence, ITerm> scopeGraph = initial.scopeGraph().melt();
        final IEsopNameResolution.Transient<Scope, Label, Occurrence> nameResolution =
                initial.nameResolution().melt(scopeGraph, (s, l) -> true);

        // constraint set properties
        final ActiveVars activeVars = new ActiveVars(unifier);
        final HasRelationBuildConstraints hasRelationBuildConstraints = new HasRelationBuildConstraints();

        // guards
        final Predicate1<ITerm> isTermInactive = t -> !activeVars.contains(t);
        final Predicate1<IRelationName> isRelationComplete = r -> !hasRelationBuildConstraints.contains(r);

        // solver components
        final SolverCore core = new SolverCore(config, unifier::find, fresh);
        final AstComponent astSolver = new AstComponent(core, initial.astProperties().melt());
        final BaseComponent baseSolver = new BaseComponent(core);
        final EqualityComponent equalitySolver = new EqualityComponent(core, unifier);
        final NameResolutionComponent nameResolutionSolver =
                new NameResolutionComponent(core, scopeGraph, nameResolution, initial.declProperties().melt());
        final PolymorphismComponent polySolver = new PolymorphismComponent(core, isTermInactive);
        final RelationComponent relationSolver =
                new RelationComponent(core, isRelationComplete, config.getFunctions(), initial.relations().melt());
        final SetComponent setSolver = new SetComponent(core, nameResolutionSolver.nameSets());
        final SymbolicComponent symSolver = new SymbolicComponent(core, initial.symbolic());

        final ISolver component =
                c -> c.matchOrThrow(IConstraint.CheckedCases.<Optional<SolveResult>, InterruptedException>builder()
                        // @formatter:off
                        .onBase(baseSolver::solve)
                        .onEquality(equalitySolver::solve)
                        .onNameResolution(nameResolutionSolver::solve)
                        .onPoly(polySolver::solve)
                        .onRelation(relationSolver::solve)
                        .onSet(setSolver::solve)
                        .onSym(symSolver::solve)
                        .otherwise(ISolver.deny("Not allowed in this phase"))
                        // @formatter:on
                );
        final FixedPointSolver solver = new FixedPointSolver(cancel, progress, component,
                Iterables2.from(activeVars, hasRelationBuildConstraints));

        solver.step().subscribe(r -> {
            if(!r.unifiedVars().isEmpty()) {
                try {
                    nameResolutionSolver.update();
                } catch(InterruptedException ex) {
                    // ignore here
                }
            }
        });

        try {
            // seed unit solutions
            final java.util.Set<IConstraint> constraints = Sets.newHashSet(initial.constraints());
            final IMessages.Transient messages = initial.messages().melt();
            for(ISolution unitSolution : unitSolutions) {
                seed(astSolver.seed(unitSolution.astProperties(), message), messages, constraints);
                seed(equalitySolver.seed(unitSolution.unifier(), message), messages, constraints);
                seed(nameResolutionSolver.seed(ImmutableNameResolutionResult.of(unitSolution.scopeGraph(),
                        unitSolution.nameResolution(), unitSolution.declProperties()), message), messages, constraints);
                seed(relationSolver.seed(unitSolution.relations(), message), messages, constraints);
                seed(symSolver.seed(unitSolution.symbolic(), message), messages, constraints);
            }

            // solve constraints
            nameResolutionSolver.update();
            SolveResult solveResult = solver.solve(constraints);
            messages.addAll(solveResult.messages());

            // build result
            IProperties.Immutable<TermIndex, ITerm, ITerm> astResult = astSolver.finish();
            NameResolutionResult nameResolutionResult = nameResolutionSolver.finish();
            IUnifier.Immutable unifierResult = equalitySolver.finish();
            IRelations.Immutable<ITerm> relationResult = relationSolver.finish();
            ISymbolicConstraints symbolicConstraints = symSolver.finish();
            return ImmutableSolution.of(config, astResult, nameResolutionResult.scopeGraph(),
                    nameResolutionResult.nameResolution(), nameResolutionResult.declProperties(), relationResult,
                    unifierResult, symbolicConstraints, messages.freeze(), solveResult.constraints());
        } catch(RuntimeException ex) {
            throw new SolverException("Internal solver error.", ex);
        }
    }

    private boolean seed(SeedResult result, IMessages.Transient messages, Set<IConstraint> constraints) {
        boolean change = false;
        change |= messages.addAll(result.messages());
        change |= constraints.addAll(result.constraints());
        return change;
    }

}