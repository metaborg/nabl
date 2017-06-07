package org.metaborg.meta.nabl2.solver.solvers;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.config.NaBL2DebugConfig;
import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.constraints.relations.IRelationConstraint;
import org.metaborg.meta.nabl2.constraints.sets.ISetConstraint;
import org.metaborg.meta.nabl2.relations.IRelations;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopNameResolution;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.esop.reference.EsopNameResolution;
import org.metaborg.meta.nabl2.scopegraph.esop.reference.EsopScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPath;
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
import org.metaborg.meta.nabl2.solver.components.EqualityComponent;
import org.metaborg.meta.nabl2.solver.components.NameResolutionComponent;
import org.metaborg.meta.nabl2.solver.components.NameSetsComponent;
import org.metaborg.meta.nabl2.solver.components.PolymorphismComponent;
import org.metaborg.meta.nabl2.solver.components.RelationComponent;
import org.metaborg.meta.nabl2.solver.components.SetComponent;
import org.metaborg.meta.nabl2.solver.components.SymbolicComponent;
import org.metaborg.meta.nabl2.solver.messages.IMessages;
import org.metaborg.meta.nabl2.solver.messages.Messages;
import org.metaborg.meta.nabl2.solver.properties.ActiveVars;
import org.metaborg.meta.nabl2.solver.properties.HasRelationBuildConstraints;
import org.metaborg.meta.nabl2.stratego.TermIndex;
import org.metaborg.meta.nabl2.symbolic.ISymbolicConstraints;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.unification.IUnifier;
import org.metaborg.meta.nabl2.util.collections.IProperties;
import org.metaborg.meta.nabl2.util.functions.Function1;
import org.metaborg.meta.nabl2.util.functions.Predicate1;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

public class IncrementalMultiFileSolver extends BaseMultiFileSolver {
    private static final ILogger logger = LoggerUtils.logger(IncrementalMultiFileSolver.class);

    private final String globalSource;

    public IncrementalMultiFileSolver(String globalSource, NaBL2DebugConfig nabl2Debug) {
        super(nabl2Debug);
        this.globalSource = globalSource;
    }

    public IncrementalSolution solveInter(ISolution globalIntra, Map<String, ISolution> unitIntras,
            IMessageInfo message, Function1<String, ITermVar> fresh, ICancel cancel, IProgress progress)
            throws InterruptedException, SolverException {

        // build global scope graph
        final IEsopScopeGraph.Transient<Scope, Label, Occurrence, ITerm> scopeGraph = EsopScopeGraph.Transient.of();
        for(ISolution unitIntra : unitIntras.values()) {
            scopeGraph.addAll(unitIntra.scopeGraph());
        }
        final IEsopNameResolution.Transient<Scope, Label, Occurrence> nameResolution =
                EsopNameResolution.Transient.of(globalIntra.config().getResolutionParams(), scopeGraph, (s, l) -> true);
        nameResolution.resolve();

        // FIXME do something with previous state

        final Map<String, ISolution> pending = Maps.newHashMap(unitIntras);
        final Map<String, ISolution> finished = Maps.newHashMap();
        final SetMultimap<String, String> deps = HashMultimap.create();
        final SetMultimap<String, String> missingDeps = HashMultimap.create();

        // solve global
        ISolution globalInter = reportUnsolvedNonCheckConstraints(globalIntra);
        globalInter = solveInterChecks(globalInter, scopeGraph, nameResolution, cancel, progress);

        // seed global
        for(Map.Entry<String, ISolution> entry : pending.entrySet()) {
            entry.setValue(mergeDep(entry.getValue(), Iterables2.singleton(globalInter), message));
        }

        // solve inference
        boolean change = true;
        while(change) {
            change = false;

            final SetMultimap<String, String> newDeps = discoverDependencies(nameResolution, deps);
            if(!newDeps.isEmpty()) {
                missingDeps.putAll(newDeps);
            }

            Iterator<Map.Entry<String, ISolution>> it = pending.entrySet().iterator();
            while(it.hasNext()) {
                final Map.Entry<String, ISolution> entry = it.next();
                final String resource = entry.getKey();
                final ISolution initial = entry.getValue();
                if(missingDeps.get(resource).isEmpty()) {
                    if(nabl2Debug.files()) {
                        logger.info("Finished inter analysis of {}.", resource);
                    }
                    finished.put(resource, initial);
                    it.remove();
                    change |= true;
                } else {
                    final List<ISolution> depsToMerge = Lists.newArrayList();
                    final Iterator<String> depIt = missingDeps.get(resource).iterator();
                    while(depIt.hasNext()) {
                        final String depResource = depIt.next();
                        if(finished.containsKey(depResource)) {
                            depsToMerge.add(finished.get(depResource).findAndLock());
                            depIt.remove();
                        }
                    }
                    if(!depsToMerge.isEmpty()) {
                        ISolution solution = mergeDep(initial, depsToMerge, message);
                        solution = solveInterInference(solution, scopeGraph, nameResolution, fresh, cancel, progress);
                        pending.put(resource, solution);
                        change |= true;
                    }
                }
            }
        }

        // finalize and report errors
        for(Map.Entry<String, ISolution> entry : pending.entrySet()) {
            String resource = entry.getKey();
            logger.info("Could not finish inter analysis of {}, missing dependencies {}", resource,
                    missingDeps.get(resource));
        }
        finished.putAll(pending);

        for(Map.Entry<String, ISolution> entry : finished.entrySet()) {
            ISolution solution = entry.getValue();
            solution = reportUnsolvedNonCheckConstraints(solution);
            solution = solveInterChecks(solution, scopeGraph, nameResolution, cancel, progress);
            entry.setValue(solution);
        }

        final IEsopScopeGraph.Immutable<Scope, Label, Occurrence, ITerm> finalScopeGraph = scopeGraph.freeze();
        final IEsopNameResolution.Immutable<Scope, Label, Occurrence> finalNameResolution = nameResolution.freeze();
        for(Map.Entry<String, ISolution> entry : finished.entrySet()) {
            ISolution solution = ImmutableSolution.builder()
                    // @formatter:off
                    .from(entry.getValue())
                    .scopeGraph(finalScopeGraph)
                    .nameResolution(finalNameResolution)
                    .build();
                    // @formatter:on
            solution = reportUnsolvedNonCheckConstraints(solution);
            entry.setValue(solution);
        }

        if(nabl2Debug.files()) {
            logger.info("Dependencies: {}", deps);
        }

        return ImmutableIncrementalSolution.of(globalInter, finished, deps);
    }

    private SetMultimap<String, String> discoverDependencies(
            IEsopNameResolution<Scope, Label, Occurrence> nameResolution, SetMultimap<String, String> dependencies) {
        // FIXME Track newly found paths only, instead of iterating over all
        SetMultimap<String, String> newDependencies = HashMultimap.create();
        for(Occurrence ref : nameResolution.getAllRefs()) {
            for(IResolutionPath<Scope, Label, Occurrence> path : nameResolution.resolve(ref)) {
                String refResource = path.getReference().getIndex().getResource();
                for(String depResource : pathResources(path)) {
                    if(!depResource.equals(refResource) && !depResource.equals(globalSource)
                            && dependencies.put(refResource, depResource)) {
                        newDependencies.put(refResource, depResource);
                    }
                }
            }
        }
        return newDependencies;
    }

    private Set<String> pathResources(IResolutionPath<Scope, Label, Occurrence> path) {
        Set<String> res = Sets.newHashSet();
        res.add(path.getDeclaration().getIndex().getResource());
        for(IResolutionPath<Scope, Label, Occurrence> imp : path.getImportPaths()) {
            res.add(imp.getReference().getIndex().getResource());
            res.add(imp.getDeclaration().getIndex().getResource());
        }
        return res;
    }

    private ISolution mergeDep(ISolution initial, Iterable<? extends ISolution> dependencySolutions,
            IMessageInfo message) throws InterruptedException, SolverException {
        final SolverConfig config = initial.config();

        // shared
        final IUnifier.Transient unifier = initial.unifier().melt();
        final IEsopScopeGraph.Transient<Scope, Label, Occurrence, ITerm> scopeGraph = initial.scopeGraph().melt();
        final IEsopNameResolution.Transient<Scope, Label, Occurrence> nameResolution =
                initial.nameResolution().melt(scopeGraph, (s, l) -> false);

        // solver components
        final SolverCore core = new SolverCore(config, unifier::find, n -> {
            throw new IllegalStateException("Fresh not available during merge.");
        });
        final EqualityComponent equalitySolver = new EqualityComponent(core, unifier);
        final NameResolutionComponent nameResolutionSolver =
                new NameResolutionComponent(core, scopeGraph, nameResolution, initial.declProperties().melt());
        final RelationComponent relationSolver =
                new RelationComponent(core, r -> false, config.getFunctions(), initial.relations().melt());
        final SymbolicComponent symSolver = new SymbolicComponent(core, initial.symbolic());

        try {
            // seed unit solutions
            final java.util.Set<IConstraint> constraints = Sets.newHashSet(initial.constraints());
            final IMessages.Transient messages = initial.messages().melt();
            for(ISolution dependencySolution : dependencySolutions) {
                seed(nameResolutionSolver.seed(dependencySolution.declProperties(), message), messages, constraints);
                seed(relationSolver.seed(dependencySolution.relations(), message), messages, constraints);
                seed(symSolver.seed(dependencySolution.symbolic(), message), messages, constraints);
            }
            nameResolutionSolver.update();

            // build result
            IUnifier.Immutable unifierResult = equalitySolver.finish();
            IProperties.Immutable<Occurrence, ITerm, ITerm> declResult = nameResolutionSolver.finishDeclProperties();
            IRelations.Immutable<ITerm> relationResult = relationSolver.finish();
            ISymbolicConstraints symbolicConstraints = symSolver.finish();
            ISolution solution = ImmutableSolution.builder().from(initial).unifier(unifierResult)
                    .declProperties(declResult).relations(relationResult).symbolic(symbolicConstraints)
                    .messages(messages.freeze()).constraints(constraints).build();
            return solution;
        } catch(RuntimeException ex) {
            throw new SolverException("Internal solver error.", ex);
        }
    }

    private ISolution solveInterInference(ISolution initial,
            IEsopScopeGraph.Transient<Scope, Label, Occurrence, ITerm> scopeGraph,
            IEsopNameResolution.Transient<Scope, Label, Occurrence> nameResolution, Function1<String, ITermVar> fresh,
            ICancel cancel, IProgress progress) throws SolverException, InterruptedException {
        final SolverConfig config = initial.config();

        // shared
        final IUnifier.Transient unifier = initial.unifier().melt();

        // constraint set properties
        final ActiveVars activeVars = new ActiveVars(unifier);
        final HasRelationBuildConstraints hasRelationBuildConstraints = new HasRelationBuildConstraints();

        // guards
        final Predicate1<ITerm> isTermInactive = t -> !activeVars.contains(t);

        // solver components
        final SolverCore core = new SolverCore(config, unifier::find, fresh);
        final AstComponent astSolver = new AstComponent(core, initial.astProperties().melt());
        final BaseComponent baseSolver = new BaseComponent(core);
        final EqualityComponent equalitySolver = new EqualityComponent(core, unifier);
        final NameResolutionComponent nameResolutionSolver =
                new NameResolutionComponent(core, scopeGraph, nameResolution, initial.declProperties().melt());
        final NameSetsComponent nameSetSolver = new NameSetsComponent(core, scopeGraph, nameResolution);
        final PolymorphismComponent polySolver = new PolymorphismComponent(core, isTermInactive);
        final RelationComponent relationSolver =
                new RelationComponent(core, r -> false, config.getFunctions(), initial.relations().melt());
        final SetComponent setSolver = new SetComponent(core, nameSetSolver.nameSets());
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
            // solve constraints
            nameResolutionSolver.update();
            SolveResult solveResult = solver.solve(initial.constraints());

            // build result
            IProperties.Immutable<TermIndex, ITerm, ITerm> astResult = astSolver.finish();
            IProperties.Immutable<Occurrence, ITerm, ITerm> declResult = nameResolutionSolver.finishDeclProperties();
            IUnifier.Immutable unifierResult = equalitySolver.finish();
            IRelations.Immutable<ITerm> relationResult = relationSolver.finish();
            ISymbolicConstraints symbolicConstraints = symSolver.finish();
            ISolution solution = ImmutableSolution.of(config, astResult, initial.scopeGraph(), initial.nameResolution(),
                    declResult, relationResult, unifierResult, symbolicConstraints, solveResult.messages(),
                    solveResult.constraints());

            return solution;
        } catch(RuntimeException ex) {
            throw new SolverException("Internal solver error.", ex);
        }
    }

    public ISolution reportUnsolvedNonCheckConstraints(ISolution initial) {
        java.util.Set<IConstraint> checkConstraints = Sets.newHashSet();
        java.util.Set<IConstraint> otherConstraints = Sets.newHashSet();
        initial.constraints().stream().forEach(c -> {
            if(IRelationConstraint.isCheck(c) || ISetConstraint.is(c)) {
                checkConstraints.add(c);
            } else {
                otherConstraints.add(c);
            }
        });
        IMessages.Transient messages = initial.messages().melt();
        messages.addAll(Messages.unsolvedErrors(otherConstraints));
        return ImmutableSolution.builder().from(initial).messages(messages.freeze()).constraints(checkConstraints)
                .build();
    }

    private ISolution solveInterChecks(ISolution initial,
            IEsopScopeGraph.Transient<Scope, Label, Occurrence, ITerm> scopeGraph,
            IEsopNameResolution.Transient<Scope, Label, Occurrence> nameResolution, ICancel cancel, IProgress progress)
            throws SolverException, InterruptedException {
        final SolverConfig config = initial.config();

        // solver components
        final SolverCore core = new SolverCore(config, initial.unifier()::find, n -> {
            throw new IllegalStateException("Fresh variables cannot be created in this phase.");
        });
        final NameSetsComponent nameSetSolver = new NameSetsComponent(core, scopeGraph, nameResolution);
        final RelationComponent relationSolver =
                new RelationComponent(core, r -> true, config.getFunctions(), initial.relations().melt());
        final SetComponent setSolver = new SetComponent(core, nameSetSolver.nameSets());

        final ISolver denySolver = ISolver.deny("Not allowed in this phase");
        final ISolver component =
                c -> c.matchOrThrow(
                        IConstraint.CheckedCases.<Optional<SolveResult>, InterruptedException>builder()
                // @formatter:off
                .onRelation(cc -> cc.matchOrThrow(
                        IRelationConstraint.CheckedCases.<Optional<SolveResult>, InterruptedException>of(
                                denySolver::apply, relationSolver::solve, denySolver::apply)))
                .onSet(setSolver::solve)
                .otherwise(denySolver)
                // @formatter:on
                );
        final FixedPointSolver solver = new FixedPointSolver(cancel, progress, component, Iterables2.empty());

        try {
            // solve constraints
            SolveResult solveResult = solver.solve(initial.constraints());

            final IMessages.Transient messages = initial.messages().melt();
            messages.addAll(solveResult.messages());

            // build result
            return ImmutableSolution.builder().from(initial).messages(messages.freeze())
                    .constraints(solveResult.constraints()).build();
        } catch(RuntimeException ex) {
            throw new SolverException("Internal solver error.", ex);
        }

    }

    @Value.Immutable
    @Serial.Version(42l)
    public static abstract class IncrementalSolution {

        @Value.Parameter public abstract ISolution globalInter();

        @Value.Parameter public abstract Map<String, ISolution> unitInters();

        @Value.Parameter public abstract SetMultimap<String, String> dependencies();

    }

}