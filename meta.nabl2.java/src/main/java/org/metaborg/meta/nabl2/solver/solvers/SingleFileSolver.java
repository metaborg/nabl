package org.metaborg.meta.nabl2.solver.solvers;

import java.util.Optional;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.relations.IRelationName;
import org.metaborg.meta.nabl2.relations.IRelations;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.esop.reference.EsopNameResolution;
import org.metaborg.meta.nabl2.scopegraph.terms.Label;
import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.scopegraph.terms.Scope;
import org.metaborg.meta.nabl2.solver.ISolution;
import org.metaborg.meta.nabl2.solver.ISolver;
import org.metaborg.meta.nabl2.solver.ISolver.SolveResult;
import org.metaborg.meta.nabl2.solver.ImmutableSolution;
import org.metaborg.meta.nabl2.solver.SolverCore;
import org.metaborg.meta.nabl2.solver.SolverException;
import org.metaborg.meta.nabl2.solver.components.BaseComponent;
import org.metaborg.meta.nabl2.solver.components.EqualityComponent;
import org.metaborg.meta.nabl2.solver.components.NameResolutionComponent;
import org.metaborg.meta.nabl2.solver.components.NameResolutionComponent.NameResolutionResult;
import org.metaborg.meta.nabl2.solver.components.PolymorphismComponent;
import org.metaborg.meta.nabl2.solver.components.RelationComponent;
import org.metaborg.meta.nabl2.solver.components.SetComponent;
import org.metaborg.meta.nabl2.solver.components.SymbolicComponent;
import org.metaborg.meta.nabl2.solver.properties.ActiveVars;
import org.metaborg.meta.nabl2.solver.properties.HasRelationBuildConstraints;
import org.metaborg.meta.nabl2.symbolic.ISymbolicConstraints;
import org.metaborg.meta.nabl2.symbolic.SymbolicConstraints;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.unification.IUnifier;
import org.metaborg.meta.nabl2.unification.Unifier;
import org.metaborg.meta.nabl2.util.collections.Properties;
import org.metaborg.meta.nabl2.util.functions.Function1;
import org.metaborg.meta.nabl2.util.functions.Predicate1;
import org.metaborg.meta.nabl2.util.functions.Predicate2;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;

public class SingleFileSolver extends BaseSolver {

    public ISolution solve(GraphSolution initial, Function1<String, ITermVar> fresh, ICancel cancel, IProgress progress)
            throws SolverException, InterruptedException {

        // shared
        final IUnifier.Transient unifier = Unifier.Transient.of();
        final IEsopScopeGraph.Transient<Scope, Label, Occurrence, ITerm> scopeGraph = initial.scopeGraph().melt();

        // constraint set properties
        final ActiveVars activeVars = new ActiveVars(unifier);
        final HasRelationBuildConstraints hasRelationBuildConstraints = new HasRelationBuildConstraints();

        // guards
        final Predicate1<ITerm> isTermInactive = t -> !activeVars.contains(t);
        final Predicate1<IRelationName> isRelationComplete = r -> !hasRelationBuildConstraints.contains(r);
        final Predicate2<Scope, Label> isEdgeClosed = (s, l) -> !scopeGraph.isOpen(s, l);

        // solver components
        final SolverCore core = new SolverCore(initial.config(), unifier::find, fresh);
        final BaseComponent baseSolver = new BaseComponent(core);
        final EqualityComponent equalitySolver = new EqualityComponent(core, unifier);
        final NameResolutionComponent nameResolutionSolver = new NameResolutionComponent(core, scopeGraph,
                EsopNameResolution.Transient.of(initial.config().getResolutionParams(), scopeGraph, isEdgeClosed),
                Properties.Transient.of());
        final PolymorphismComponent polySolver = new PolymorphismComponent(core, isTermInactive);
        final RelationComponent relationSolver = new RelationComponent(core, isRelationComplete,
                initial.config().getFunctions(), initial.config().getRelations().melt());
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
            nameResolutionSolver.update();
            SolveResult solveResult = solver.solve(initial.constraints());

            NameResolutionResult nameResolutionResult = nameResolutionSolver.finish();
            IUnifier.Immutable unifierResult = equalitySolver.finish();
            IRelations.Immutable<ITerm> relationResult = relationSolver.finish();
            ISymbolicConstraints symbolicConstraints = symSolver.finish();
            return ImmutableSolution.of(initial.config(), initial.astProperties(), nameResolutionResult.scopeGraph(),
                    nameResolutionResult.nameResolution(), nameResolutionResult.declProperties(), relationResult,
                    unifierResult, symbolicConstraints, solveResult.messages(), solveResult.constraints());
        } catch(RuntimeException ex) {
            throw new SolverException("Internal solver error.", ex);
        }
    }

}