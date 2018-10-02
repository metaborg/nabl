package mb.nabl2.solver.solvers;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.metaborg.util.Ref;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.functions.Predicate1;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;

import mb.nabl2.constraints.IConstraint;
import mb.nabl2.relations.variants.IVariantRelation;
import mb.nabl2.relations.variants.VariantRelations;
import mb.nabl2.scopegraph.esop.IEsopNameResolution;
import mb.nabl2.scopegraph.esop.IEsopScopeGraph;
import mb.nabl2.scopegraph.esop.lazy.EsopNameResolution;
import mb.nabl2.scopegraph.esop.reference.EsopScopeGraph;
import mb.nabl2.scopegraph.terms.Label;
import mb.nabl2.scopegraph.terms.Occurrence;
import mb.nabl2.scopegraph.terms.Scope;
import mb.nabl2.solver.ISolution;
import mb.nabl2.solver.ISolver;
import mb.nabl2.solver.ISolver.SolveResult;
import mb.nabl2.solver.ImmutableSolution;
import mb.nabl2.solver.SolverConfig;
import mb.nabl2.solver.SolverCore;
import mb.nabl2.solver.SolverException;
import mb.nabl2.solver.components.AstComponent;
import mb.nabl2.solver.components.BaseComponent;
import mb.nabl2.solver.components.EqualityComponent;
import mb.nabl2.solver.components.NameResolutionComponent;
import mb.nabl2.solver.components.NameResolutionComponent.NameResolutionResult;
import mb.nabl2.solver.components.NameSetsComponent;
import mb.nabl2.solver.components.PolymorphismComponent;
import mb.nabl2.solver.components.RelationComponent;
import mb.nabl2.solver.components.ScopeGraphComponent;
import mb.nabl2.solver.components.SetComponent;
import mb.nabl2.solver.components.SymbolicComponent;
import mb.nabl2.solver.messages.IMessages;
import mb.nabl2.solver.messages.Messages;
import mb.nabl2.solver.properties.ActiveDeclTypes;
import mb.nabl2.solver.properties.ActiveVars;
import mb.nabl2.solver.properties.HasRelationBuildConstraints;
import mb.nabl2.solver.properties.PolySafe;
import mb.nabl2.symbolic.ISymbolicConstraints;
import mb.nabl2.symbolic.SymbolicConstraints;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.util.collections.Properties;

public class CompletionSolver {

    private final CallExternal callExternal;

    public CompletionSolver(CallExternal callExternal) {
        this.callExternal = callExternal;
    }

    public ISolution solve(ISolution initial, Function1<String, String> fresh, ICancel cancel, IProgress progress)
            throws SolverException, InterruptedException {
        final ISolution graphSolution = solveGraph(initial, fresh, cancel, progress);
        final ISolution constraintSolution = solveConstraints(graphSolution, fresh, cancel, progress);
        final ISolution solution = reportUnsolvedConstraints(constraintSolution);
        return solution;
    }

    private ISolution solveGraph(ISolution initial, Function1<String, String> fresh, ICancel cancel, IProgress progress)
            throws SolverException, InterruptedException {
        final SolverConfig config = initial.config();

        // shared
        final Ref<IUnifier.Immutable> unifier = new Ref<>(initial.unifier());
        final IEsopScopeGraph.Transient<Scope, Label, Occurrence, ITerm> scopeGraph = EsopScopeGraph.Transient.of();

        // solver components
        final SolverCore core = new SolverCore(config, unifier, fresh, callExternal);
        final AstComponent astSolver = new AstComponent(core, Properties.Transient.of());
        final BaseComponent baseSolver = new BaseComponent(core);
        final EqualityComponent equalitySolver = new EqualityComponent(core, unifier);
        final ScopeGraphComponent scopeGraphSolver = new ScopeGraphComponent(core, scopeGraph);

        try {
            // @formatter:off
            ISolver component =
                    c -> c.matchOrThrow(IConstraint.CheckedCases.<Optional<SolveResult>, InterruptedException>builder()
                    .onAst(astSolver::solve)
                    .onBase(baseSolver::solve)
                    .onEquality(equalitySolver::solve)
                    .onScopeGraph(scopeGraphSolver::solve)
                    .otherwise(ISolver.defer())
                    );
            // @formatter:on

            final FixedPointSolver solver = new FixedPointSolver(cancel, progress, component, Iterables2.empty());
            final SolveResult solveResult = solver.solve(initial.constraints());

            // @formatter:off
            return initial
                    .withAstProperties(astSolver.finish())
                    .withScopeGraph(scopeGraphSolver.finish())
                    .withUnifier(equalitySolver.finish())
                    .withMessages(solveResult.messages())
                    .withConstraints(solveResult.constraints());
            // @formatter:on
        } catch(RuntimeException ex) {
            throw new SolverException("Internal solver error.", ex);
        }
    }

    private ISolution solveConstraints(ISolution initial, Function1<String, String> fresh, ICancel cancel,
            IProgress progress) throws SolverException, InterruptedException {
        final SolverConfig config = initial.config();

        // shared
        final Ref<IUnifier.Immutable> unifier = new Ref<>(initial.unifier());
        final IEsopScopeGraph.Transient<Scope, Label, Occurrence, ITerm> scopeGraph = initial.scopeGraph().melt();
        final IEsopNameResolution<Scope, Label, Occurrence> nameResolution =
                EsopNameResolution.of(config.getResolutionParams(), scopeGraph, (s, l) -> true);

        // constraint set properties
        final ActiveVars activeVars = new ActiveVars(unifier);
        final ActiveDeclTypes activeDeclTypes = new ActiveDeclTypes(unifier);
        final HasRelationBuildConstraints hasRelationBuildConstraints = new HasRelationBuildConstraints();

        // guards
        final Predicate1<String> isRelationComplete = r -> !hasRelationBuildConstraints.contains(r);

        // solver components
        final SolverCore core = new SolverCore(config, unifier, fresh, callExternal);
        final BaseComponent baseSolver = new BaseComponent(core);
        final EqualityComponent equalitySolver = new EqualityComponent(core, unifier);
        final NameResolutionComponent nameResolutionSolver =
                new NameResolutionComponent(core, scopeGraph, nameResolution, Properties.Transient.of());
        final NameSetsComponent nameSetSolver = new NameSetsComponent(core, scopeGraph, nameResolution);
        final RelationComponent relationSolver = new RelationComponent(core, isRelationComplete, config.getFunctions(),
                VariantRelations.transientOf(config.getRelations()));
        final SetComponent setSolver = new SetComponent(core, nameSetSolver.nameSets());
        final SymbolicComponent symSolver = new SymbolicComponent(core, SymbolicConstraints.of());

        // polymorphism solver
        final PolySafe polySafe = new PolySafe(activeVars, activeDeclTypes, nameResolutionSolver);
        final PolymorphismComponent polySolver = new PolymorphismComponent(core, polySafe::isGenSafe,
                polySafe::isInstSafe, nameResolutionSolver::getProperty);

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
                    .otherwise(ISolver.defer())
                    // @formatter:on
                );
        final FixedPointSolver solver = new FixedPointSolver(cancel, progress, component,
                Iterables2.from(activeVars, hasRelationBuildConstraints));

        solver.step().subscribe(r -> {
            if(!r.unifierDiff().isEmpty()) {
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

            final IMessages.Transient messages = initial.messages().melt();
            messages.addAll(solveResult.messages());

            NameResolutionResult nameResolutionResult = nameResolutionSolver.finish();
            IUnifier.Immutable unifierResult = equalitySolver.finish();
            Map<String, IVariantRelation.Immutable<ITerm>> relationResult = relationSolver.finish();
            ISymbolicConstraints symbolicConstraints = symSolver.finish();

            // @formatter:off
            return initial
                    .withScopeGraph(nameResolutionResult.scopeGraph())
                    .withDeclProperties(nameResolutionResult.declProperties())
                    .withRelations(relationResult)
                    .withUnifier(unifierResult)
                    .withSymbolic(symbolicConstraints)
                    .withMessages(messages.freeze())
                    .withConstraints(solveResult.constraints());
            // @formatter:on
        } catch(RuntimeException ex) {
            throw new SolverException("Internal solver error.", ex);
        }
    }

    private ISolution reportUnsolvedConstraints(ISolution initial) {
        IMessages.Transient messages = initial.messages().melt();
        messages.addAll(Messages.unsolvedErrors(initial.constraints()));
        return ImmutableSolution.builder().from(initial).messages(messages.freeze()).constraints(Collections.emptySet())
                .build();
    }

}