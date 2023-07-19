package mb.nabl2.solver.solvers;

import java.util.List;

import org.metaborg.util.Ref;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.functions.Predicate1;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import mb.nabl2.config.NaBL2DebugConfig;
import mb.nabl2.constraints.IConstraint;
import mb.nabl2.log.Logger;
import mb.nabl2.relations.variants.IVariantRelation;
import mb.nabl2.relations.variants.VariantRelations;
import mb.nabl2.solver.ISolution;
import mb.nabl2.solver.ISolver;
import mb.nabl2.solver.Solution;
import mb.nabl2.solver.SolveResult;
import mb.nabl2.solver.SolverConfig;
import mb.nabl2.solver.SolverCore;
import mb.nabl2.solver.components.BaseComponent;
import mb.nabl2.solver.components.EqualityComponent;
import mb.nabl2.solver.components.NameResolutionComponent;
import mb.nabl2.solver.components.NameResolutionResult;
import mb.nabl2.solver.components.NameSetsComponent;
import mb.nabl2.solver.components.RelationComponent;
import mb.nabl2.solver.components.SetComponent;
import mb.nabl2.solver.components.SymbolicComponent;
import mb.nabl2.solver.exceptions.DelayException;
import mb.nabl2.solver.exceptions.SolverException;
import mb.nabl2.solver.messages.IMessages;
import mb.nabl2.solver.properties.HasRelationBuildConstraints;
import mb.nabl2.symbolic.ISymbolicConstraints;
import mb.nabl2.symbolic.SymbolicConstraints;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.u.IUnifier;
import mb.nabl2.util.collections.Properties;
import mb.scopegraph.pepm16.ScopeGraphReducer;
import mb.scopegraph.pepm16.esop15.CriticalEdge;
import mb.scopegraph.pepm16.esop15.IEsopNameResolution;
import mb.scopegraph.pepm16.esop15.IEsopScopeGraph;
import mb.scopegraph.pepm16.terms.Label;
import mb.scopegraph.pepm16.terms.Occurrence;
import mb.scopegraph.pepm16.terms.Scope;

public class SingleFileSolver extends BaseSolver {

    private static final Logger log = Logger.logger(SingleFileSolver.class);

    public SingleFileSolver(NaBL2DebugConfig nabl2Debug, CallExternal callExternal) {
        super(nabl2Debug, callExternal);
    }

    public ISolution solve(GraphSolution initial, Function1<String, String> fresh, ICancel cancel, IProgress progress)
            throws SolverException, InterruptedException {
        final SolverConfig config = initial.config();

        // shared
        final Ref<IUnifier.Immutable> unifier = new Ref<>(initial.unifier());

        // constraint set properties
        final HasRelationBuildConstraints hasRelationBuildConstraints = new HasRelationBuildConstraints();

        // guards
        final Predicate1<String> isRelationComplete = r -> !hasRelationBuildConstraints.contains(r);

        // more shared
        final IEsopScopeGraph.Transient<Scope, Label, Occurrence, ITerm> scopeGraph = initial.scopeGraph().melt();
        final IEsopNameResolution<Scope, Label, Occurrence> nameResolution =
                IEsopNameResolution.of(config.getResolutionParams(), scopeGraph, (s, l) -> true);
        final ScopeGraphReducer scopeGraphReducer = new ScopeGraphReducer(scopeGraph, unifier);

        // solver components
        final SolverCore core = new SolverCore(config, unifier, fresh, callExternal, cancel, progress);
        final BaseComponent baseSolver = new BaseComponent(core);
        final EqualityComponent equalitySolver = new EqualityComponent(core, unifier);
        final NameResolutionComponent nameResolutionSolver =
                new NameResolutionComponent(core, scopeGraph, nameResolution, Properties.Transient.of());
        final NameSetsComponent nameSetSolver = new NameSetsComponent(core, nameResolution);
        final RelationComponent relationSolver = new RelationComponent(core, isRelationComplete, config.getFunctions(),
                VariantRelations.transientOf(config.getRelations()));
        final SetComponent setSolver = new SetComponent(core, nameSetSolver.nameSets());
        final SymbolicComponent symSolver = new SymbolicComponent(core, SymbolicConstraints.of());

        // @formatter:off
        final ISolver component = c -> c.matchOrThrow(IConstraint.CheckedCases.<SolveResult, DelayException>builder()
            .onBase(baseSolver::solve)
            .onEquality(equalitySolver::solve)
            .onNameResolution(nameResolutionSolver::solve)
            .onRelation(relationSolver::solve)
            .onSet(setSolver::solve)
            .onSym(symSolver::solve)
            .otherwise(ISolver.defer()::apply)
        );
        // @formatter:on
        final FixedPointSolver solver = new FixedPointSolver(cancel, progress, component);

        solver.step().subscribe(r -> {
            hasRelationBuildConstraints.addAll(r.result.constraints());
            r.resolveRelations(hasRelationBuildConstraints.remove(r.constraint));
            final Set.Immutable<ITermVar> vars = r.result.unifierDiff().domainSet();
            if(!vars.isEmpty()) {
                try {
                    final List<CriticalEdge> criticalEdges = scopeGraphReducer.update(vars);
                    nameResolution.update(criticalEdges, cancel, progress);
                    r.resolveCriticalEdges(criticalEdges);
                } catch(InterruptedException ex) {
                    // ignore here
                }
            }
        });

        try {
            scopeGraphReducer.updateAll();
            hasRelationBuildConstraints.addAll(initial.constraints());
            SolveResult solveResult = solver.solve(initial.constraints(), unifier);

            final IMessages.Transient messages = initial.messages().melt();
            messages.addAll(solveResult.messages());

            NameResolutionResult nameResolutionResult = nameResolutionSolver.finish();
            IUnifier.Immutable unifierResult = equalitySolver.finish();
            Map.Immutable<String, IVariantRelation.Immutable<ITerm>> relationResult = relationSolver.finish();
            ISymbolicConstraints symbolicConstraints = symSolver.finish();
            setSolver.finish();

            Solution solution = Solution
                    .of(config, initial.astProperties(), nameResolutionResult.scopeGraph(),
                            nameResolutionResult.declProperties(), relationResult, unifierResult, symbolicConstraints,
                            messages.freeze(), solveResult.constraints())
                    .withNameResolutionCache(nameResolutionResult.resolutionCache());

            log.info("finish single: ", solution);

            return solution;
        } catch(RuntimeException ex) {
            throw new SolverException("Internal solver error.", ex);
        }
    }

}
