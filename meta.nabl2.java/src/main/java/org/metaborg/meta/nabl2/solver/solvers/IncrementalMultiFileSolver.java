package org.metaborg.meta.nabl2.solver.solvers;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.config.NaBL2DebugConfig;
import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.constraints.relations.IRelationConstraint;
import org.metaborg.meta.nabl2.constraints.sets.ISetConstraint;
import org.metaborg.meta.nabl2.relations.IRelationName;
import org.metaborg.meta.nabl2.relations.RelationException;
import org.metaborg.meta.nabl2.relations.variants.IVariantRelation;
import org.metaborg.meta.nabl2.relations.variants.VariantRelations;
import org.metaborg.meta.nabl2.scopegraph.ScopeGraphCommon;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopNameResolution;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.esop.reference.EsopNameResolution;
import org.metaborg.meta.nabl2.scopegraph.esop.reference.EsopScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPath;
import org.metaborg.meta.nabl2.scopegraph.terms.Label;
import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.scopegraph.terms.Scope;
import org.metaborg.meta.nabl2.solver.Dependencies;
import org.metaborg.meta.nabl2.solver.Dependencies.TopoSortedComponents;
import org.metaborg.meta.nabl2.solver.IPublicSolution;
import org.metaborg.meta.nabl2.solver.ISolution;
import org.metaborg.meta.nabl2.solver.ISolver;
import org.metaborg.meta.nabl2.solver.ISolver.SolveResult;
import org.metaborg.meta.nabl2.solver.ImmutablePublicSolution;
import org.metaborg.meta.nabl2.solver.ImmutableSolution;
import org.metaborg.meta.nabl2.solver.PublicSolution;
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
import org.metaborg.meta.nabl2.symbolic.ImmutableSymbolicConstraints;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.unification.IUnifier;
import org.metaborg.meta.nabl2.unification.Unifier;
import org.metaborg.meta.nabl2.util.collections.IProperties;
import org.metaborg.meta.nabl2.util.collections.Properties;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.functions.Predicate1;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import io.usethesource.capsule.Set;

public class IncrementalMultiFileSolver extends BaseMultiFileSolver {
    private static final ILogger logger = LoggerUtils.logger(IncrementalMultiFileSolver.class);

    private final String globalSource;

    public IncrementalMultiFileSolver(String globalSource, NaBL2DebugConfig nabl2Debug) {
        super(nabl2Debug);
        this.globalSource = globalSource;
    }

    public IncrementalSolution solveInter(IncrementalSolution initial, Map<String, ISolution> updatedUnits,
            Collection<String> removedUnits, Collection<Scope> intfScopes, IMessageInfo message,
            Function1<String, ITermVar> fresh, ICancel cancel, IProgress progress)
            throws InterruptedException, SolverException {

        try {
            final Dependencies.Transient<String> dependencies = initial.dependencies().melt();
            final ISolution globalIntra = initial.globalIntra();
            final Map<String, ISolution> unitIntras = Maps.newHashMap(initial.unitIntras());
            final Map<Set.Immutable<String>, ISolution> unitInters = Maps.newHashMap(initial.unitInters());

            final SolverConfig config = globalIntra.config();

            if(nabl2Debug.files()) {
                logger.info("Updated {}", updatedUnits.keySet());
            }

            // calculate diff
            java.util.Set<Occurrence> globalDiff = Sets.newHashSet();
            {
                for(String unit : removedUnits) {
                    final ISolution prevIntra = unitIntras.get(unit);
                    final java.util.Set<Occurrence> prevDecls;
                    if(prevIntra != null) {
                        ScopeGraphCommon<Scope, Label, Occurrence, ITerm> prevGraph =
                                new ScopeGraphCommon<>(prevIntra.scopeGraph());
                        prevDecls = intfScopes.stream().flatMap(s -> prevGraph.reachableDecls(s).stream())
                                .collect(Collectors.toSet());
                        globalDiff.addAll(prevDecls);
                    }

                }
                for(Map.Entry<String, ISolution> entry : updatedUnits.entrySet()) {
                    final ISolution prevIntra = unitIntras.get(entry.getKey());
                    final java.util.Set<Occurrence> prevDecls;
                    if(prevIntra != null) {
                        ScopeGraphCommon<Scope, Label, Occurrence, ITerm> prevGraph =
                                new ScopeGraphCommon<>(prevIntra.scopeGraph());
                        prevDecls = intfScopes.stream().flatMap(s -> prevGraph.reachableDecls(s).stream())
                                .collect(Collectors.toSet());
                    } else {
                        prevDecls = Collections.emptySet();
                    }

                    final ISolution intra = entry.getValue();
                    final java.util.Set<Occurrence> currDecls;
                    ScopeGraphCommon<Scope, Label, Occurrence, ITerm> currGraph =
                            new ScopeGraphCommon<>(intra.scopeGraph());
                    currDecls = intfScopes.stream().flatMap(s -> currGraph.reachableDecls(s).stream())
                            .collect(Collectors.toSet());

                    final java.util.Set<Occurrence> unitDiff = Sets.symmetricDifference(prevDecls, currDecls);
                    globalDiff.addAll(unitDiff);
                }
            }
            if(nabl2Debug.analysis()) {
                logger.warn("Changed top-level declarations: {}", globalDiff);
            }

            // update to new intras
            removedUnits.stream().forEach(unitIntras::remove);
            unitIntras.putAll(updatedUnits);

            // create global scope graph
            final IEsopScopeGraph.Transient<Scope, Label, Occurrence, ITerm> scopeGraph =
                    globalIntra.scopeGraph().melt();
            for(ISolution unitIntra : unitIntras.values()) {
                scopeGraph.addAll(unitIntra.scopeGraph());
            }
            final IEsopNameResolution.Transient<Scope, Label, Occurrence> nameResolution =
                    EsopNameResolution.Transient.of(config.getResolutionParams(), scopeGraph, (s, l) -> true);
            nameResolution.resolveAll();

            // invalidate units and dependents
            final java.util.Set<String> invalidatedUnits = Sets.newHashSet();
            {
                // invalidate removed units
                {
                    invalidatedUnits.addAll(removedUnits);
                    Set.Immutable<String> removedDependents = dependencies.getAllDependents(removedUnits);
                    invalidatedUnits.addAll(removedDependents);
                    if(nabl2Debug.analysis()) {
                        logger.info("Invalidated removed {} and dependents {}.", removedUnits, removedDependents);
                    }
                }

                // invalidate updated units
                {
                    invalidatedUnits.addAll(updatedUnits.keySet());
                    Set.Immutable<String> updatedDependents = dependencies.getAllDependents(updatedUnits.keySet());
                    invalidatedUnits.addAll(updatedDependents);
                    if(nabl2Debug.files()) {
                        logger.info("Invalidated updated {} and dependents {}.", updatedUnits.keySet(),
                                updatedDependents);
                    }
                }

                // invalidate remaining units for which resolution changed
                {
                    TopoSortedComponents<String> components = dependencies.getTopoSortedComponents();
                    Optional<ISolution> inter = initial.globalInter();
                    Predicate<String> resolutionChanged = unit -> {
                        if(!inter.isPresent()) {
                            return true;
                        }
                        ISolution intra = unitIntras.get(unit);
                        java.util.Set<Occurrence> free =
                                Sets.difference(intra.scopeGraph().getAllRefs(), intra.nameResolution().getAllRefs());
                        Set.Immutable<String> component = components.component(unit); // unused, because we use the global inter here, which contains all solutions
                        for(Occurrence ref : free) {
                            Optional<Set.Immutable<IResolutionPath<Scope, Label, Occurrence>>> currDecls =
                                    nameResolution.tryResolve(ref);
                            if(currDecls.isPresent()) {
                                Set.Immutable<IResolutionPath<Scope, Label, Occurrence>> prevDecls =
                                        inter.get().nameResolution().resolve(ref);
                                if(!currDecls.get().equals(prevDecls)) {
                                    return true;
                                }
                            }
                        }
                        return false;
                    };
                    java.util.Set<String> differentlyResolvedUnits =
                            Sets.difference(unitIntras.keySet(), invalidatedUnits).stream().filter(resolutionChanged)
                                    .collect(Collectors.toSet());
                    invalidatedUnits.addAll(differentlyResolvedUnits);
                    Set.Immutable<String> differentlyResolvedDependents =
                            dependencies.getAllDependents(differentlyResolvedUnits);
                    invalidatedUnits.addAll(differentlyResolvedDependents);
                    if(nabl2Debug.files()) {
                        logger.info("Invalidated differently resolved {} and dependents {}.", differentlyResolvedUnits,
                                differentlyResolvedDependents);
                    }
                }

                // remove solutions and dependencies for invalidated units
                TopoSortedComponents<String> invalidedComponents =
                        dependencies.inverse().getTopoSortedComponents(invalidatedUnits);
                invalidedComponents.components().stream().forEach(c -> {
                    unitInters.remove(c);
                    dependencies.removeAll(c);
                    if(nabl2Debug.files()) {
                        logger.info("Removed invalidated component {}.", c);
                    }
                });
            }

            // remove removed units, and update dependency graph
            invalidatedUnits.retainAll(unitIntras.keySet());
            for(String unit : invalidatedUnits) {
                final List<IResolutionPath<Scope, Label, Occurrence>> paths =
                        nameResolution.getAllRefs().stream().filter(r -> r.getIndex().getResource().equals(unit))
                                .flatMap(r -> nameResolution.resolve(r).stream()).collect(Collectors.toList());
                for(IResolutionPath<Scope, Label, Occurrence> path : paths) {
                    String refResource = path.getReference().getIndex().getResource();
                    dependencies.addAll(refResource, getPathResources(path));
                }
            }
            if(nabl2Debug.files()) {
                logger.info("Found dependencies: {}", dependencies);
            }

            // solve global
            if(nabl2Debug.files()) {
                logger.info("Analyzing global");
            }
            ISolution globalInter = solveInterInference(globalIntra, PublicSolution.of(config), scopeGraph,
                    nameResolution, fresh, cancel, progress);
            globalInter = reportUnsolvedNonCheckConstraints(globalInter);

            // analyze components
            final TopoSortedComponents<String> allComponents =
                    dependencies.getTopoSortedComponents(unitIntras.keySet());
            final TopoSortedComponents<String> invalidatedComponents =
                    dependencies.inverse().getTopoSortedComponents(invalidatedUnits);
            for(Set.Immutable<String> component : invalidatedComponents.components().reverse()) {
                if(nabl2Debug.files()) {
                    logger.info("Analyzing component {}", component);
                }
                java.util.Set<ISolution> componentIntras =
                        component.stream().map(unitIntras::get).collect(Collectors.toSet());
                java.util.Set<IPublicSolution> dependencyInters =
                        dependencies.getAllDependencies(component).stream().map(allComponents::component)
                                .map(unitInters::get).filter(s -> s != null).collect(Collectors.toSet());
                IPublicSolution context =
                        combinePublicSolutions(config, Iterables2.cons(globalInter, dependencyInters));
                ISolution inter = combineSolutions(config, componentIntras);
                inter = solveInterInference(inter, context, scopeGraph, nameResolution, fresh, cancel, progress);
                inter = reportUnsolvedNonCheckConstraints(inter);
                inter = solveInterChecks(inter, context, scopeGraph, nameResolution, cancel, progress);
                unitInters.put(component, inter.findAndLock());
            }

            // check global
            if(nabl2Debug.files()) {
                logger.info("Checking global");
            }
            IEsopScopeGraph.Immutable<Scope, Label, Occurrence, ITerm> finalScopeGraph = scopeGraph.freeze();
            IEsopNameResolution.Immutable<Scope, Label, Occurrence> finalNameResolution = nameResolution.freeze();
            IMessages.Transient messages = globalInter.messages().melt();

            unitInters.replaceAll((unit, s) -> ImmutableSolution.builder().from(s).scopeGraph(finalScopeGraph)
                    .nameResolution(finalNameResolution).build());
            unitInters.values().stream().map(ISolution::messages).forEach(messages::addAll);

            globalInter = combineSolutions(config, Iterables2.cons(globalInter, unitInters.values()));
            globalInter = solveInterChecks(globalInter, PublicSolution.of(config), scopeGraph, nameResolution, cancel,
                    progress);
            globalInter = ImmutableSolution.builder().from(globalInter).scopeGraph(finalScopeGraph)
                    .nameResolution(finalNameResolution).messages(messages.freeze()).build();

            return ImmutableIncrementalSolution.of(globalIntra, unitIntras, Optional.of(globalInter), unitInters,
                    dependencies.freeze());
        } catch(Exception ex) {
            throw new SolverException(ex);
        }
    }

    private Iterable<String> getPathResources(IResolutionPath<Scope, Label, Occurrence> path) {
        java.util.Set<String> resources = Sets.newHashSet(path.getDeclaration().getIndex().getResource());
        path.getScopes().stream().map(s -> s.getResource()).forEach(resources::add);
        path.getImports().stream().map(i -> i.getIndex().getResource()).forEach(resources::add);
        resources.remove(path.getReference().getIndex().getResource());
        resources.remove(globalSource);
        return resources;
    }

    private ISolution combineSolutions(SolverConfig config, Iterable<? extends ISolution> solutions)
            throws InterruptedException, SolverException {

        final IProperties.Transient<TermIndex, ITerm, ITerm> astProperties = Properties.Transient.of();
        final IEsopScopeGraph.Transient<Scope, Label, Occurrence, ITerm> scopeGraph = EsopScopeGraph.Transient.of();
        final IEsopNameResolution.Transient<Scope, Label, Occurrence> nameResolution =
                EsopNameResolution.Transient.of(config.getResolutionParams(), scopeGraph, (s, l) -> false);
        final IProperties.Transient<Occurrence, ITerm, ITerm> declProperties = Properties.Transient.of();
        final Map<IRelationName, IVariantRelation.Transient<ITerm>> relations =
                VariantRelations.transientOf(config.getRelations());
        final IUnifier.Transient unifier = Unifier.Transient.of();
        final Set.Transient<ITerm> symbolicFacts = Set.Transient.of();
        final Set.Transient<ITerm> symbolicGoals = Set.Transient.of();
        final IMessages.Transient messages = Messages.Transient.of();
        final java.util.Set<IConstraint> constraints = Sets.newHashSet();

        try {
            for(ISolution solution : solutions) {
                astProperties.putAll(solution.astProperties());
                scopeGraph.addAll(solution.scopeGraph());
                nameResolution.addAll(solution.nameResolution());
                declProperties.putAll(solution.declProperties());
                for(Map.Entry<IRelationName, IVariantRelation.Immutable<ITerm>> entry : solution.relations()
                        .entrySet()) {
                    relations.get(entry.getKey()).addAll(entry.getValue());
                }
                unifier.putAll(solution.unifier());
                symbolicFacts.__insertAll(solution.symbolic().getFacts());
                symbolicGoals.__insertAll(solution.symbolic().getGoals());
                messages.addAll(solution.messages());
                constraints.addAll(solution.constraints());
            }
        } catch(RelationException ex) {
            throw new SolverException(ex);
        }

        ISolution solution = ImmutableSolution.builder().config(config)
                // @formatter:off
                .astProperties(astProperties.freeze())
                .scopeGraph(scopeGraph.freeze())
                .nameResolution(nameResolution.freeze())
                .declProperties(declProperties.freeze())
                .relations(VariantRelations.freeze(relations))
                .unifier(unifier.freeze())
                .symbolic(ImmutableSymbolicConstraints.of(symbolicFacts.freeze(), symbolicGoals.freeze()))
                .messages(messages.freeze())
                .constraints(constraints)
                .build();
                // @formatter:on

        return solution;
    }

    private IPublicSolution combinePublicSolutions(SolverConfig config, Iterable<? extends IPublicSolution> solutions)
            throws InterruptedException, SolverException {

        final IEsopScopeGraph.Transient<Scope, Label, Occurrence, ITerm> scopeGraph = EsopScopeGraph.Transient.of();
        final IProperties.Transient<Occurrence, ITerm, ITerm> declProperties = Properties.Transient.of();
        final Map<IRelationName, IVariantRelation.Transient<ITerm>> relations =
                VariantRelations.transientOf(config.getRelations());
        final Set.Transient<ITerm> symbolicFacts = Set.Transient.of();
        final Set.Transient<ITerm> symbolicGoals = Set.Transient.of();

        try {
            for(IPublicSolution solution : solutions) {
                scopeGraph.addAll(solution.scopeGraph());
                declProperties.putAll(solution.declProperties());
                for(Map.Entry<IRelationName, IVariantRelation.Immutable<ITerm>> entry : solution.relations()
                        .entrySet()) {
                    relations.get(entry.getKey()).addAll(entry.getValue());
                }
                symbolicFacts.__insertAll(solution.symbolic().getFacts());
                symbolicGoals.__insertAll(solution.symbolic().getGoals());
            }
        } catch(RelationException ex) {
            throw new SolverException(ex);
        }

        IPublicSolution solution = ImmutablePublicSolution.builder().config(config)
                // @formatter:off
                .scopeGraph(scopeGraph.freeze())
                .declProperties(declProperties.freeze())
                .relations(VariantRelations.freeze(relations))
                .symbolic(ImmutableSymbolicConstraints.of(symbolicFacts.freeze(), symbolicGoals.freeze()))
                .build();
                // @formatter:on

        return solution;
    }

    private ISolution solveInterInference(ISolution initial, IPublicSolution context,
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
        final NameResolutionComponent nameResolutionSolver = new NameResolutionComponent(core, scopeGraph,
                nameResolution, Properties.extend(initial.declProperties().melt(), context.declProperties()));
        final NameSetsComponent nameSetSolver = new NameSetsComponent(core, scopeGraph, nameResolution);
        final PolymorphismComponent polySolver = new PolymorphismComponent(core, isTermInactive);
        RelationComponent relationSolver;
        try {
            relationSolver = new RelationComponent(core, r -> false, config.getFunctions(),
                    VariantRelations.extend(VariantRelations.melt(initial.relations()), context.relations()));
        } catch(RelationException e) {
            throw new SolverException(e);
        }
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

        final ISolution solution;
        try {
            // initial resolve
            nameResolutionSolver.update();

            // solve constraints
            SolveResult solveResult = solver.solve(initial.constraints());

            // build result
            IProperties.Immutable<TermIndex, ITerm, ITerm> astResult = astSolver.finish();
            IProperties.Immutable<Occurrence, ITerm, ITerm> declResult = nameResolutionSolver.finishDeclProperties();
            IUnifier.Immutable unifierResult = equalitySolver.finish();
            Map<IRelationName, IVariantRelation.Immutable<ITerm>> relationResult = relationSolver.finish();
            ISymbolicConstraints symbolicConstraints = symSolver.finish();
            solution = ImmutableSolution.of(config, astResult, initial.scopeGraph(), initial.nameResolution(),
                    declResult, relationResult, unifierResult, symbolicConstraints, solveResult.messages(),
                    solveResult.constraints());
        } catch(RuntimeException ex) {
            throw new SolverException("Internal solver error.", ex);
        }
        return solution;
    }

    private ISolution reportUnsolvedNonCheckConstraints(ISolution initial) {
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

    private ISolution solveInterChecks(ISolution initial, IPublicSolution context,
            IEsopScopeGraph.Transient<Scope, Label, Occurrence, ITerm> scopeGraph,
            IEsopNameResolution.Transient<Scope, Label, Occurrence> nameResolution, ICancel cancel, IProgress progress)
            throws SolverException, InterruptedException {
        final SolverConfig config = initial.config();

        // solver components
        final SolverCore core = new SolverCore(config, initial.unifier()::find, n -> {
            throw new IllegalStateException("Fresh variables cannot be created in this phase.");
        });
        final NameSetsComponent nameSetSolver = new NameSetsComponent(core, scopeGraph, nameResolution);
        RelationComponent relationSolver;
        try {
            relationSolver = new RelationComponent(core, r -> true, config.getFunctions(),
                    VariantRelations.extend(VariantRelations.melt(initial.relations()), context.relations()));
        } catch(RelationException e) {
            throw new SolverException(e);
        }
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

        @Value.Parameter public abstract ISolution globalIntra();

        @Value.Parameter public abstract Map<String, ISolution> unitIntras();

        @Value.Parameter public abstract Optional<ISolution> globalInter();

        @Value.Parameter public abstract Map<Set.Immutable<String>, ISolution> unitInters();

        @Value.Parameter public abstract Dependencies.Immutable<String> dependencies();

        public static IncrementalSolution of(ISolution globalIntra) {
            return ImmutableIncrementalSolution.of(globalIntra, ImmutableMap.of(), Optional.empty(), ImmutableMap.of(),
                    Dependencies.Immutable.of());
        }

    }

}