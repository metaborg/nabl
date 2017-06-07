package org.metaborg.meta.nabl2.solver.solvers;

public class IncrementalMultiFileSolver extends BaseSolver {

//    public ISolution solveIntra(ISolution initial, Collection<ITermVar> intfVars, Collection<Scope> intfScopes,
//            Function1<String, ITermVar> fresh, ICancel cancel, IProgress progress)
//            throws SolverException, InterruptedException {
//
//        // shared
//        final IUnifier.Transient unifier = initial.unifier().melt();
//        final IEsopScopeGraph.Transient<Scope, Label, Occurrence, ITerm> scopeGraph = initial.scopeGraph().melt();
//
//        // constraint set properties
//        final ActiveVars activeVars = new ActiveVars(unifier);
//        intfVars.stream().forEach(activeVars::add);
//
//        // guards
//        final Predicate1<ITerm> isTermInactive = v -> !activeVars.contains(v);
//        final Predicate1<IRelationName> isRelationComplete = r -> false;
//        final Predicate2<Scope, Label> isEdgeClosed = (s, l) -> !(intfScopes.contains(s) || scopeGraph.isOpen(s, l));
//
//        // solver components
//        final SolverCore core = new SolverCore(config, unifier, fresh);
//        final AstComponent astSolver = new AstComponent(core, initial.astProperties().melt());
//        final BaseComponent baseSolver = new BaseComponent(core);
//        final EqualityComponent equalitySolver = new EqualityComponent(core, unifier);
//        final ScopeGraphComponent scopeGraphSolver = new ScopeGraphComponent(core, scopeGraph);
//        final NameResolutionComponent nameResolutionSolver = new NameResolutionComponent(core, () -> true, scopeGraph,
//                initial.nameResolution().melt(scopeGraph, isEdgeClosed), initial.declProperties().melt());
//        final PolymorphismComponent polySolver = new PolymorphismComponent(core, isTermInactive);
//        final RelationComponent relationSolver =
//                new RelationComponent(core, isRelationComplete, config.getFunctions(), initial.relations().melt());
//        final SetComponent setSolver = new SetComponent(core, nameResolutionSolver.nameSets());
//        final SymbolicComponent symSolver = new SymbolicComponent(core, initial.symbolic());
//
//        try {
//            final CompositeComponent compositeSolver = new CompositeComponent(core, ISolver.deny(astSolver), baseSolver,
//                    equalitySolver, nameResolutionSolver, polySolver, relationSolver, setSolver,
//                    ISolver.deny(scopeGraphSolver), symSolver);
//
//            final FixedPointSolver solver =
//                    new FixedPointSolver(cancel, progress, compositeSolver, Iterables2.singleton(activeVars));
//            final SolveResult solveResult = solver.solve(initial.constraints());
//
//            final Set<IConstraint> graphConstraints = solveResult.constraints().stream()
//                    .filter(c -> IScopeGraphConstraint.is(c)).collect(Collectors.toSet());
//            final Set<IConstraint> otherConstraints = solveResult.constraints().stream()
//                    .filter(c -> !IScopeGraphConstraint.is(c)).collect(Collectors.toSet());
//
//            final IMessages.Transient messages = initial.messages().melt();
//            messages.addAll(Messages.unsolvedErrors(graphConstraints));
//            messages.addAll(solveResult.messages());
//
//            return ImmutableSolution.builder()
//                    // @formatter:off
//                    .from(compositeSolver.finish())
//                    .messages(solveResult.messages())
//                    .constraints(otherConstraints)
//                    // @formatter:on
//                    .build();
//        } catch(RuntimeException ex) {
//            throw new SolverException("Internal solver error.", ex);
//        }
//
//    }
//
//    public Tuple2<ISolution, Multimap<String, String>> solveInterInference(ISolution initial,
//            Iterable<? extends ISolution> deps, IMessageInfo message, Function1<String, ITermVar> fresh, ICancel cancel,
//            IProgress progress) throws SolverException, InterruptedException {
//
//        // shared
//        final IUnifier.Transient unifier = initial.unifier().melt();
//        final IEsopScopeGraph.Transient<Scope, Label, Occurrence, ITerm> scopeGraph = initial.scopeGraph().melt();
//
//        // constraint set properties
//        final ActiveVars activeVars = new ActiveVars(unifier);
//
//        // guards
//        final Predicate1<ITerm> isTermInactive = t -> !activeVars.contains(t);
//        final Predicate2<Scope, Label> isEdgeClosed = (s, l) -> !scopeGraph.isOpen(s, l);
//
//        // solver components
//        final SolverCore core = new SolverCore(config, unifier, fresh);
//        final AstComponent astSolver = new AstComponent(core, initial.astProperties().melt());
//        final BaseComponent baseSolver = new BaseComponent(core);
//        final EqualityComponent equalitySolver = new EqualityComponent(core, unifier);
//        final ScopeGraphComponent scopeGraphSolver = new ScopeGraphComponent(core, scopeGraph);
//        final NameResolutionComponent nameResolutionSolver = new NameResolutionComponent(core, () -> true, scopeGraph,
//                initial.nameResolution().melt(scopeGraph, isEdgeClosed), initial.declProperties().melt());
//        final PolymorphismComponent polySolver = new PolymorphismComponent(core, isTermInactive);
//        final RelationComponent relationSolver =
//                new RelationComponent(core, r -> false, config.getFunctions(), initial.relations().melt());
//        final SetComponent setSolver = new SetComponent(core, nameResolutionSolver.nameSets());
//        final SymbolicComponent symSolver = new SymbolicComponent(core, initial.symbolic());
//
//        try {
//            final CompositeComponent compositeSolver = new CompositeComponent(core, ISolver.deny(astSolver), baseSolver,
//                    equalitySolver, nameResolutionSolver, polySolver, relationSolver, ISolver.defer(setSolver),
//                    ISolver.deny(scopeGraphSolver), symSolver);
//
//            final IMessages.Transient messages = initial.messages().melt();
//            final Set<IConstraint> constraints = Sets.newHashSet(initial.constraints());
//            for(ISolution dep : deps) {
//                final SeedResult seedResult = compositeSolver.seed(dep, message);
//                messages.addAll(seedResult.messages());
//                constraints.addAll(seedResult.constraints());
//            }
//
//            final FixedPointSolver solver =
//                    new FixedPointSolver(cancel, progress, compositeSolver, Iterables2.from(activeVars));
//            SolveResult solveResult = solver.solve(constraints);
//
//            final ISolution solution = ImmutableSolution.builder()
//                    // @formatter:off
//                    .from(compositeSolver.finish())
//                    .messages(solveResult.messages())
//                    .constraints(solveResult.constraints())
//                    // @formatter:on
//                    .build();
//            return ImmutableTuple2.of(solution, solveResult.dependencies());
//        } catch(RuntimeException ex) {
//            throw new SolverException("Internal solver error.", ex);
//        }
//    }
//
//    public ISolution solveInterChecks(ISolution initial, Iterable<? extends ISolution> deps, IMessageInfo message,
//            Function1<String, ITermVar> fresh, ICancel cancel, IProgress progress)
//            throws SolverException, InterruptedException {
//
//        // shared
//        final IUnifier.Transient unifier = initial.unifier().melt();
//        final IEsopScopeGraph.Transient<Scope, Label, Occurrence, ITerm> scopeGraph = initial.scopeGraph().melt();
//
//        // constraint set properties
//        final ActiveVars activeVars = new ActiveVars(unifier);
//
//        // guards
//        final Predicate1<ITerm> isTermInactive = t -> !activeVars.contains(t);
//
//        // solver components
//        final SolverCore core = new SolverCore(config, unifier, fresh);
//        final AstComponent astSolver = new AstComponent(core, initial.astProperties().melt());
//        final BaseComponent baseSolver = new BaseComponent(core);
//        final EqualityComponent equalitySolver = new EqualityComponent(core, unifier);
//        final ScopeGraphComponent scopeGraphSolver = new ScopeGraphComponent(core, scopeGraph);
//        final NameResolutionComponent nameResolutionSolver = new NameResolutionComponent(core, () -> true, scopeGraph,
//                initial.nameResolution().melt(scopeGraph, (s, l) -> false), initial.declProperties().melt());
//        final PolymorphismComponent polySolver = new PolymorphismComponent(core, isTermInactive);
//        final RelationComponent relationSolver =
//                new RelationComponent(core, r -> true, config.getFunctions(), initial.relations().melt());
//        final SetComponent setSolver = new SetComponent(core, nameResolutionSolver.nameSets());
//        final SymbolicComponent symSolver = new SymbolicComponent(core, initial.symbolic());
//
//        try {
//            final CompositeComponent compositeSolver =
//                    new CompositeComponent(core, ISolver.deny(astSolver), ISolver.deny(baseSolver),
//                            ISolver.deny(equalitySolver), ISolver.deny(nameResolutionSolver), ISolver.deny(polySolver),
//                            relationSolver, setSolver, ISolver.deny(scopeGraphSolver), ISolver.deny(symSolver));
//
//            final Set<IConstraint> checkConstraints = initial.constraints().stream()
//                    .filter(c -> IRelationConstraint.isCheck(c) || ISetConstraint.is(c)).collect(Collectors.toSet());
//            final Set<IConstraint> otherConstraints = initial.constraints().stream()
//                    .filter(c -> !(IRelationConstraint.isCheck(c) || ISetConstraint.is(c))).collect(Collectors.toSet());
//
//            final IMessages.Transient messages = initial.messages().melt();
//            messages.addAll(Messages.unsolvedErrors(otherConstraints));
//
//            final Set<IConstraint> constraints = Sets.newHashSet(checkConstraints);
//            for(ISolution dep : deps) {
//                messages.addAll(dep.messages());
//                final SeedResult seedResult = compositeSolver.seed(dep, message);
//                messages.addAll(seedResult.messages());
//                constraints.addAll(seedResult.constraints());
//            }
//
//            final FixedPointSolver solver =
//                    new FixedPointSolver(cancel, progress, compositeSolver, Iterables2.from(activeVars));
//            SolveResult solveResult = solver.solve(constraints);
//
//            messages.addAll(Messages.unsolvedErrors(solveResult.constraints()));
//            messages.addAll(solveResult.messages());
//
//            return ImmutableSolution.builder()
//                    // @formatter:off
//                    .from(compositeSolver.finish())
//                    .messages(messages.freeze())
//                    .constraints(solveResult.constraints())
//                    // @formatter:on
//                    .build();
//        } catch(RuntimeException ex) {
//            throw new SolverException("Internal solver error.", ex);
//        }
//    }

}