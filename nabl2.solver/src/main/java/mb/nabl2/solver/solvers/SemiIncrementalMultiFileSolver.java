package mb.nabl2.solver.solvers;

import java.util.Map;
import java.util.Optional;

import org.metaborg.util.Ref;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.functions.Predicate1;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;

import com.google.common.collect.Sets;

import mb.nabl2.config.NaBL2DebugConfig;
import mb.nabl2.constraints.IConstraint;
import mb.nabl2.constraints.messages.IMessageInfo;
import mb.nabl2.relations.variants.IVariantRelation;
import mb.nabl2.relations.variants.VariantRelations;
import mb.nabl2.scopegraph.ScopeGraphReducer;
import mb.nabl2.scopegraph.esop.IEsopNameResolution;
import mb.nabl2.scopegraph.esop.IEsopScopeGraph;
import mb.nabl2.scopegraph.esop.lazy.EsopNameResolution;
import mb.nabl2.scopegraph.terms.Label;
import mb.nabl2.scopegraph.terms.Occurrence;
import mb.nabl2.scopegraph.terms.Scope;
import mb.nabl2.solver.DelayException;
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
import mb.nabl2.solver.components.ImmutableNameResolutionResult;
import mb.nabl2.solver.components.NameResolutionComponent;
import mb.nabl2.solver.components.NameResolutionComponent.NameResolutionResult;
import mb.nabl2.solver.components.NameSetsComponent;
import mb.nabl2.solver.components.RelationComponent;
import mb.nabl2.solver.components.SetComponent;
import mb.nabl2.solver.components.SymbolicComponent;
import mb.nabl2.solver.messages.IMessages;
import mb.nabl2.solver.properties.HasRelationBuildConstraints;
import mb.nabl2.symbolic.ISymbolicConstraints;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.stratego.TermIndex;
import mb.nabl2.terms.unification.u.IUnifier;
import mb.nabl2.util.collections.IProperties;

public class SemiIncrementalMultiFileSolver extends BaseMultiFileSolver {

    public SemiIncrementalMultiFileSolver(NaBL2DebugConfig nabl2Debug, CallExternal callExternal) {
        super(nabl2Debug, callExternal);
    }

    public ISolution solveInter(ISolution initial, Iterable<? extends ISolution> unitSolutions, IMessageInfo message,
            Function1<String, String> fresh, ICancel cancel, IProgress progress)
            throws SolverException, InterruptedException {
        final SolverConfig config = initial.config();

        // shared
        final Ref<IUnifier.Immutable> unifier = new Ref<>(initial.unifier());
        final IEsopScopeGraph.Transient<Scope, Label, Occurrence, ITerm> scopeGraph = initial.scopeGraph().melt();
        final IEsopNameResolution<Scope, Label, Occurrence> nameResolution =
                EsopNameResolution.of(config.getResolutionParams(), scopeGraph, (s, l) -> true);
        final ScopeGraphReducer scopeGraphReducer = new ScopeGraphReducer(scopeGraph, unifier);

        // constraint set properties
        final HasRelationBuildConstraints hasRelationBuildConstraints = new HasRelationBuildConstraints();

        // guards
        final Predicate1<String> isRelationComplete = r -> !hasRelationBuildConstraints.contains(r);

        // solver components
        final SolverCore core = new SolverCore(config, unifier, fresh, callExternal);
        final AstComponent astSolver = new AstComponent(core, initial.astProperties().melt());
        final BaseComponent baseSolver = new BaseComponent(core);
        final EqualityComponent equalitySolver = new EqualityComponent(core, unifier);
        final NameResolutionComponent nameResolutionSolver =
                new NameResolutionComponent(core, scopeGraph, nameResolution, initial.declProperties().melt());
        final NameSetsComponent nameSetSolver = new NameSetsComponent(core, nameResolution);
        final RelationComponent relationSolver = new RelationComponent(core, isRelationComplete, config.getFunctions(),
                VariantRelations.melt(initial.relations()));
        final SetComponent setSolver = new SetComponent(core, nameSetSolver.nameSets());
        final SymbolicComponent symSolver = new SymbolicComponent(core, initial.symbolic());

        final ISolver component =
                c -> c.matchOrThrow(IConstraint.CheckedCases.<Optional<SolveResult>, DelayException>builder()
                // @formatter:off
                    .onBase(baseSolver::solve)
                    .onEquality(equalitySolver::solve)
                    .onNameResolution(nameResolutionSolver::solve)
                    .onRelation(relationSolver::solve)
                    .onSet(setSolver::solve)
                    .onSym(symSolver::solve)
                    .otherwise(ISolver.defer())
                    // @formatter:on
                );
        final FixedPointSolver solver =
                new FixedPointSolver(cancel, progress, component, Iterables2.singleton(hasRelationBuildConstraints));

        solver.step().subscribe(r -> {
            if(!r.result.unifierDiff().isEmpty()) {
                try {
                    r.release(scopeGraphReducer.update(r.result.unifierDiff().varSet()));
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
                final NameResolutionResult nameResult = ImmutableNameResolutionResult.of(unitSolution.scopeGraph(),
                        unitSolution.nameResolutionCache(), unitSolution.declProperties());
                seed(nameResolutionSolver.seed(nameResult, message), messages, constraints);
                seed(relationSolver.seed(unitSolution.relations(), message), messages, constraints);
                seed(symSolver.seed(unitSolution.symbolic(), message), messages, constraints);
                constraints.addAll(unitSolution.constraints());
                messages.addAll(unitSolution.messages());
            }

            // solve constraints
            scopeGraph.reduceAll(unifier.get()::getVars);
            scopeGraphReducer.updateAll();
            SolveResult solveResult = solver.solve(constraints);
            messages.addAll(solveResult.messages());

            // build result
            IProperties.Immutable<TermIndex, ITerm, ITerm> astResult = astSolver.finish();
            NameResolutionResult nameResolutionResult = nameResolutionSolver.finish();
            IUnifier.Immutable unifierResult = equalitySolver.finish();
            Map<String, IVariantRelation.Immutable<ITerm>> relationResult = relationSolver.finish();
            ISymbolicConstraints symbolicConstraints = symSolver.finish();

            return ImmutableSolution.of(config, astResult, nameResolutionResult.scopeGraph(),
                    nameResolutionResult.declProperties(), relationResult, unifierResult, symbolicConstraints,
                    messages.freeze(), solveResult.constraints())
                    .withNameResolutionCache(nameResolutionResult.resolutionCache());
        } catch(RuntimeException ex) {
            throw new SolverException("Internal solver error.", ex);
        }
    }

}
