package org.metaborg.meta.nabl2.solver.solvers;

import java.util.Map;
import java.util.Optional;

import org.metaborg.meta.nabl2.config.NaBL2DebugConfig;
import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.controlflow.terms.CFGNode;
import org.metaborg.meta.nabl2.relations.variants.IVariantRelation;
import org.metaborg.meta.nabl2.relations.variants.VariantRelations;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopNameResolution;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.esop.reference.EsopNameResolution;
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
import org.metaborg.meta.nabl2.solver.components.BaseComponent;
import org.metaborg.meta.nabl2.solver.components.ControlFlowComponent;
import org.metaborg.meta.nabl2.solver.components.EqualityComponent;
import org.metaborg.meta.nabl2.solver.components.NameResolutionComponent;
import org.metaborg.meta.nabl2.solver.components.NameResolutionComponent.NameResolutionResult;
import org.metaborg.meta.nabl2.solver.components.NameSetsComponent;
import org.metaborg.meta.nabl2.solver.components.PolymorphismComponent;
import org.metaborg.meta.nabl2.solver.components.RelationComponent;
import org.metaborg.meta.nabl2.solver.components.SetComponent;
import org.metaborg.meta.nabl2.solver.components.SymbolicComponent;
import org.metaborg.meta.nabl2.solver.messages.IMessages;
import org.metaborg.meta.nabl2.solver.properties.ActiveDeclTypes;
import org.metaborg.meta.nabl2.solver.properties.ActiveVars;
import org.metaborg.meta.nabl2.solver.properties.DeclTypeDeps;
import org.metaborg.meta.nabl2.solver.properties.HasRelationBuildConstraints;
import org.metaborg.meta.nabl2.symbolic.ISymbolicConstraints;
import org.metaborg.meta.nabl2.symbolic.SymbolicConstraints;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.unification.IUnifier;
import org.metaborg.meta.nabl2.util.collections.Properties;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.functions.Predicate1;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;

import meta.flowspec.nabl2.controlflow.IControlFlowGraph;
import meta.flowspec.nabl2.controlflow.impl.ControlFlowGraph;

public class SingleFileSolver extends BaseSolver {

    public SingleFileSolver(NaBL2DebugConfig nabl2Debug, CallExternal callExternal) {
        super(nabl2Debug, callExternal);
    }

    public ISolution solve(GraphSolution initial, Function1<String, String> fresh, ICancel cancel, IProgress progress)
            throws SolverException, InterruptedException {
        final SolverConfig config = initial.config();

        // shared
        final IUnifier.Transient unifier = initial.unifier().melt();

        // constraint set properties
        final ActiveVars activeVars = new ActiveVars(unifier);
        final ActiveDeclTypes activeDeclTypes = new ActiveDeclTypes(unifier);
        final DeclTypeDeps declTypeDeps = new DeclTypeDeps(unifier, activeDeclTypes::contains);
        final HasRelationBuildConstraints hasRelationBuildConstraints = new HasRelationBuildConstraints();

        // guards
        final Predicate1<ITerm> isGenSafe = t -> !activeVars.contains(t) && !declTypeDeps.contains(t);
        final Predicate1<Occurrence> isInstSafe = d -> activeDeclTypes.contains(d);
        final Predicate1<String> isRelationComplete = r -> !hasRelationBuildConstraints.contains(r);

        // more shared
        final IEsopScopeGraph.Transient<Scope, Label, Occurrence, ITerm> scopeGraph = initial.scopeGraph().melt();
        final IEsopNameResolution.Transient<Scope, Label, Occurrence> nameResolution =
                EsopNameResolution.Transient.of(config.getResolutionParams(), scopeGraph, (s, l) -> true);

        // solver components
        final SolverCore core = new SolverCore(config, unifier::find, fresh, callExternal);
        final BaseComponent baseSolver = new BaseComponent(core);
        final EqualityComponent equalitySolver = new EqualityComponent(core, unifier);
        final Properties.Transient<Occurrence, ITerm, ITerm> properties = Properties.Transient.of();
        final NameResolutionComponent nameResolutionSolver =
                new NameResolutionComponent(core, scopeGraph, nameResolution, properties);
        final NameSetsComponent nameSetSolver = new NameSetsComponent(core, scopeGraph, nameResolution);
        final PolymorphismComponent polySolver =
                new PolymorphismComponent(core, isGenSafe, isInstSafe, nameResolutionSolver::getProperty);
        final RelationComponent relationSolver = new RelationComponent(core, isRelationComplete, config.getFunctions(),
                VariantRelations.transientOf(config.getRelations()));
        final SetComponent setSolver = new SetComponent(core, nameSetSolver.nameSets());
        final SymbolicComponent symSolver = new SymbolicComponent(core, SymbolicConstraints.of());
        final ControlFlowComponent cfgSolver = new ControlFlowComponent(core, ControlFlowGraph.of(), properties);

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
                    .onControlflow(cfgSolver::solve)
                    .otherwise(ISolver.deny("Not allowed in this phase"))
                    // @formatter:on
                );
        final FixedPointSolver solver = new FixedPointSolver(cancel, progress, component,
                Iterables2.from(activeVars, hasRelationBuildConstraints));

        solver.step().subscribe(r -> {
            if(!r.unifiedVars().isEmpty()) {
                try {
                    nameResolutionSolver.update();
                    cfgSolver.update();
                } catch(InterruptedException ex) {
                    // ignore here
                }
            }
        });

        try {
            nameResolutionSolver.update();
            cfgSolver.update();
            SolveResult solveResult = solver.solve(initial.constraints());

            final IMessages.Transient messages = initial.messages().melt();
            messages.addAll(solveResult.messages());

            NameResolutionResult nameResolutionResult = nameResolutionSolver.finish();
            IUnifier.Immutable unifierResult = equalitySolver.finish();
            Map<String, IVariantRelation.Immutable<ITerm>> relationResult = relationSolver.finish();
            ISymbolicConstraints symbolicConstraints = symSolver.finish();
            IControlFlowGraph<CFGNode> cfg = cfgSolver.getControlFlowGraph();
            
            // TODO: add dataflow solver call here
            
            return ImmutableSolution.of(config, initial.astProperties(), nameResolutionResult.scopeGraph(),
                    nameResolutionResult.nameResolution(), nameResolutionResult.declProperties(), relationResult,
                    unifierResult, symbolicConstraints, cfg, solveResult.messages(), solveResult.constraints());
        } catch(RuntimeException ex) {
            throw new SolverException("Internal solver error.", ex);
        }
    }

}