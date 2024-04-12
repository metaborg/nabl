package statix.lang.strategies;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.IStrategoTuple;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.terms.util.TermUtils;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;
import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.collection.MultiSet;
import org.metaborg.util.collection.MultiSetMap;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import io.usethesource.capsule.Set.Immutable;

public class compute_schema_1_0 extends Strategy {

    private static final ILogger log = LoggerUtils.logger(compute_schema_1_0.class);

    public static final Strategy instance = new compute_schema_1_0();

    @Override public IStrategoTerm invoke(Context context, IStrategoTerm current, Strategy s_debug) {
        // 0. Initialization
        final IStrategoTuple args = TermUtils.asTuple(current).get();
        final IStrategoTerm types = args.get(0);
        final IStrategoTerm edges = args.get(1);
        final IStrategoTerm decls = args.get(2);
        final IStrategoTerm constraints = args.get(3);
        final IStrategoTerm globs = args.get(4);

        final ITermFactory TF = context.getFactory();
        final boolean debug = s_debug.invoke(context, current) != null;

        return new Command(TF, debug).run(types, edges, decls, constraints, globs);
    }


    // Utilities

    public static <K, V> Set<V> getSetValue(Map<K, Set<V>> map, K key) {
        return getValue(map, key, HashSet::new);
    }

    public static <K1, K2, V> Map<K2, V> getMapValue(Map<K1, Map<K2, V>> map, K1 key) {
        return getValue(map, key, HashMap::new);
    }

    public static <K, V> V getValue(Map<K, V> map, K key, Supplier<V> valueSupplier) {
        return map.computeIfAbsent(key, __ -> valueSupplier.get());
    }

    public static boolean isPredicateArg(IStrategoTerm term) {
        return TermUtils.isAppl(term, "PArg", 2);
    }

    private boolean isConstructorArg(IStrategoTerm term) {
        return TermUtils.isAppl(term, "CArg", 2);
    }

    private boolean isRelationArg(IStrategoTerm term) {
        return TermUtils.isAppl(term, "RArg", 2);
    }

    public Cardinality computeNewCardinality(Cardinality oldCard, int edgeMult, int edgeCount, IStrategoTerm source,
            IStrategoTerm target) {
        final Cardinality newCard;
        if(TermUtils.isAppl(source, "PArg", 2) && edgeCount > 1) {
            newCard = oldCard.mult(edgeMult).withLower(0);
        } else {
            newCard = oldCard.mult(edgeMult);
        }
        return newCard;
    }

    // Inner data structures

    private class Command {

        private final ConstraintGraph<IStrategoTerm> cg = new ConstraintGraph<>();

        private final VariableInfo<IStrategoTerm, IStrategoTerm> nodeInfo = new VariableInfo<>();

        private final ITermFactory TF;

        private final boolean debug;

        public Command(ITermFactory TF, boolean debug) {
            this.TF = TF;
            this.debug = debug;
        }

        public IStrategoTerm run(IStrategoTerm types, IStrategoTerm edges, IStrategoTerm decls,
                IStrategoTerm constraints, IStrategoTerm globs) {
            createConstraintGraph(constraints);

            // 1. Forward propagating node type info
            //    This ensures that the types of all _owner_ scopes (and unification assertions thereof)
            //    are propagated through all predicate calls.

            // Variables to be used as starting point for next phase.
            log.info("*** Phase 1: propagate owned scopes ***");
            final HashSet<IStrategoTerm> nextPhase = new HashSet<>();
            for(IStrategoTerm type : types) {
                propagateOwnedTypes(mkVariable(type), mkVarKind(type), nextPhase);
            }
            final IStrategoTerm globKind = TF.makeAppl("Glob");
            for(IStrategoTerm glob : globs) {
                propagateOwnedTypes(glob, globKind, nextPhase);
            }

            // 2. Close scopes with extension permission
            log.info("*** Phase 2: close owned scopes ***");
            final Set<IStrategoTerm> downPreds = new HashSet<>();
            //    a. edges
            for(IStrategoTerm var_rule : edges.getSubterms()) {
                closeOwned(mkVariable(var_rule.getSubterm(0).getSubterm(0)), downPreds);
            }
            //    b. decls
            for(IStrategoTerm var_rule : decls.getSubterms()) {
                closeOwned(mkVariable(var_rule.getSubterm(0).getSubterm(2)), downPreds);
            }
            //    c. owned
            for(IStrategoTerm type : types) {
                closeOwned(mkVariable(type), downPreds);
            }
            //    d. transitive
            for(IStrategoTerm pvar : downPreds) {
                closeOwnedTransitive(pvar);
            }

            // 3. Propagate remainder of constraints
            log.info("*** Phase 3: close unowned scope references ***");
            for(IStrategoTerm var : nextPhase) {
                propagateRemoteTypes(var, TraversalContext.of(debug));
            }

            // 4. Mark scopes with unknown origin
            log.info("*** Phase 4: mark unknown scopes ***");
            final IStrategoTerm UNKNOWN = TF.makeAppl("Unknown");
            for(IStrategoTerm var : cg.nodes) {
                if(!nodeInfo.hasCardinality(var)) {
                    propagateUnknown(var, UNKNOWN, TraversalContext.of(debug));
                }
            }

            log.info("*** Phase 5: build schema ***");
            // 5. Build Schema
            //    a. edges
            final IStrategoTerm edgesTerm = TF.makeAppl("SGEdges", TF.makeList(
                    edges.getSubterms().stream().map(edge -> buildEdgeTerm(edge)).toArray(IStrategoTerm[]::new)));

            //    b. decls
            final IStrategoTerm declsTerm = TF.makeAppl("SGDecls", TF.makeList(
                    decls.getSubterms().stream().map(decl -> buildDeclTerm(decl)).toArray(IStrategoTerm[]::new)));

            //    c. schema vars
            final IStrategoTerm varsTerm = TF.makeAppl("SchemaVars",
                    TF.makeList(cg.nodes.stream()
                            .map(var -> TF.makeAppl("SchemaVar", var, buildScopeKindCardList(var)))
                            .toArray(IStrategoTerm[]::new)));

            return TF.makeAppl("SGSchema", edgesTerm, declsTerm, varsTerm);
        }

        private void createConstraintGraph(IStrategoTerm constraints) {
            for(IStrategoTerm edgeTerm : constraints) {
                if(TermUtils.isAppl(edgeTerm, "DEdge", 2)) {
                    cg.addDirectedEdge(edgeTerm.getSubterm(0), edgeTerm.getSubterm(1));
                } else if(TermUtils.isAppl(edgeTerm, "UEdge", 2)) {
                    cg.addUndirectedEdge(edgeTerm.getSubterm(0), edgeTerm.getSubterm(1));
                } else {
                    throw new IllegalArgumentException("Unrecognized constraint: " + edgeTerm);
                }
            }
        }

        // Phase 1. Propagate info of owned scopes to all positions where it will propagate with certainty.

        private void propagateOwnedTypes(IStrategoTerm var, IStrategoTerm type, Set<IStrategoTerm> nextPhase) {
            propagateOwnedTypes(var, type, Cardinality.ONE, TraversalContext.of(debug), nextPhase);
        }

        private void propagateOwnedTypes(IStrategoTerm var, IStrategoTerm type, Cardinality card, TraversalContext ctx,
                Set<IStrategoTerm> nextPhase) {
            final Cardinality prevCard = nodeInfo.getCardinalityOrDefault(var, type, Cardinality.ZERO);

            // 1. Update the current cardinality
            final Cardinality newCard;
            final TraversalContext newContext;
            if(ctx.visited(var)) {
                // cycle in 'call graph': unbounded recursion
                if(cg.directedEdges.incomingEdges(var).elementSet().size() == 1 && prevCard.getLower() != 0) {
                    // No other branch: infinite recursion
                    newCard = Cardinality.INFINITE;
                } else {
                    newCard = Cardinality.ZERO2INFINITE;
                }
                newContext = ctx.reset();
            } else {
                newCard = card;
                newContext = ctx.withVisited(var);
            }

            if(prevCard.encloses(newCard)) {
                // No new info, stop propagation
                ctx.log("STOP: fixpoint.");
                return;
            }

            ctx.log("Updating cardinality {}.{} to {} (was {}).", var, type, newCard, prevCard);
            nodeInfo.setCardinality(var, type, newCard);
            if(isConstructorArg(var) || isRelationArg(var)) {
                // Terminal node for first propagation
                nextPhase.add(var);
                ctx.log("STOP: first-phase terminal point.");
                return;
            }


            // 2. Traverse Directed Edges
            final MultiSet.Immutable<IStrategoTerm> edges = cg.directedEdges.outgoingEdges(var);
            for(Map.Entry<IStrategoTerm, Integer> edge : edges.entrySet()) {
                final IStrategoTerm tgt = edge.getKey();
                final int count = edge.getValue();
                ctx.log("Traversing directed edge {}->{} ({} * {})", var, tgt, newCard, count);
                propagateOwnedTypes(Edge.forward(var, tgt, count), type, newCard, newContext, nextPhase);
            }


            // 3. Back-Traverse owned scopes 'leaking' though direct predicate arguments
            final MultiSet.Immutable<IStrategoTerm> invEdges = cg.directedEdges.incomingEdges(var);
            for(Map.Entry<IStrategoTerm, Integer> edge : invEdges.entrySet()) {
                final IStrategoTerm tgt = edge.getKey();
                final int count = edge.getValue();
                if(isPredicateArg(tgt) && !ctx.visited(tgt)) {
                    if(cg.directedEdges.outgoingEdges(tgt).elementSet().size() == 1) {
                        // The current rule is the only rule for the predicate,
                        // so currently owned scope will be the correct instance
                        // for the parent as well.
                        final Cardinality subCard = newCard.mult(count);
                        ctx.log("Back-propagating {}->{} ({} * {}).", var, tgt, newCard, count);
                        nodeInfo.setCardinality(tgt, type, subCard);

                        final Set<Entry<IStrategoTerm, Integer>> backEdges =
                                cg.directedEdges.incomingEdges(tgt).entrySet();
                        for(Map.Entry<IStrategoTerm, Integer> backEdge : backEdges) {
                            ctx.log("Back-traversing directed edge {}->{} ({} * {})", var, tgt, subCard,
                                    backEdge.getValue());
                            propagateOwnedTypes(Edge.backward(tgt, backEdge.getKey(), backEdge.getValue()), type,
                                    subCard, newContext.withVisited(tgt).withVisited(Edge.backward(var, tgt, count)),
                                    nextPhase);
                        }
                    } else {
                        // Other scopes may propagate to/from the parent rule,
                        // so merge that properly in the next phase.
                        nextPhase.add(tgt);
                    }
                }
            }


            // 4. Traverse Undirected Edges
            for(Map.Entry<IStrategoTerm, Integer> edge : cg.undirectedEdges.edges(var).entrySet()) {
                final IStrategoTerm tgt = edge.getKey();
                final int count = edge.getValue();
                ctx.log("Traversing undirected edge {}->{} ({} * {})", var, tgt, newCard, count);
                propagateOwnedTypes(Edge.undirected(var, tgt, count), type, newCard, newContext, nextPhase);
            }

        }

        private void propagateOwnedTypes(Edge<IStrategoTerm> edge, IStrategoTerm type, Cardinality card,
                TraversalContext ctx, Set<IStrategoTerm> nextPhase) {
            if(ctx.visited(edge.invert())) {
                ctx.log("STOP: edge already traversed in other direction.");
                return;
            }

            final Cardinality updatedCard = nodeInfo.getCardinality(edge.getSource(), type);
            if(!card.equals(updatedCard)) {
                ctx.log("STOP: Recursive update detected (cur: {}, updated: {}).", card, updatedCard);
                return;
            }

            final IStrategoTerm target = edge.getTarget();
            final IStrategoTerm source = edge.getSource();

            // Properly manage distribution of scopes over edge instantiation.
            if(isPredicateArg(target) && edge.getDirection() == Direction.FORWARD) {
                final MultiSet.Immutable<IStrategoTerm> inEdges = cg.directedEdges.incomingEdges(target);

                // If all incoming edges are known, this argument is fully determined by owned scopes.
                // Therefore, we propagate the info of all incoming types.
                final Set<IStrategoTerm> types = inEdges.elementSet().stream().flatMap(src -> {
                    return nodeInfo.getCardinalities(src).keySet().stream();
                }).collect(Collectors.toSet());

                for(IStrategoTerm tp : types) {
                    // Compute additive aggregation of all incoming cardinalities
                    Cardinality aggCard = null;
                    for(Map.Entry<IStrategoTerm, Integer> inEdge : inEdges.entrySet()) {
                        final IStrategoTerm inSource = inEdge.getKey();
                        final int count = inEdge.getValue();
                        final Cardinality oldCard = nodeInfo.getCardinality(inSource, tp);
                        final boolean currentSource = inSource.equals(source) && type.equals(tp);
                        if(oldCard == null) {
                            if(currentSource) {
                                ctx.log("- ignoring unknown value {}.", inSource);
                                throw new IllegalStateException();
                            }
                            continue;
                        }
                        final Cardinality edgeCard =
                                (currentSource ? card.mult(edge.getWeigth()) : oldCard.mult(count));
                        if(aggCard == null) {
                            aggCard = edgeCard;
                            ctx.log("- initial value {}.", aggCard);
                        } else {
                            aggCard = aggCard.add(edgeCard);
                            ctx.log("- aggregated value {}.", aggCard);
                        }
                    }
                    if(cg.directedEdges.outgoingEdges(source).elementSet().size() > 1) {
                        aggCard = card.mult(edge.getWeigth()).withLower(0);
                    } else {
                        aggCard = card.mult(edge.getWeigth());
                    }
                    final Cardinality newCard = aggCard;

                    propagateOwnedTypes(target, tp, newCard, ctx.withVisited(edge).increaseIndent(), nextPhase);
                }
            } else {
                // Update direction of undirected edges
                if((isConstructorArg(target) || isRelationArg(target))
                        && edge.getDirection().equals(Direction.UNDIRECTED)) {
                    cg.makeDirected(source, target);
                }

                propagateOwnedTypes(target, type, card.mult(edge.getWeigth()), ctx.withVisited(edge).increaseIndent(),
                        nextPhase);
            }
        }


        // Phase 2. Close Dominated scopes

        private void closeOwned(IStrategoTerm var, Set<IStrategoTerm> downPreds) {
            closeOwned(var, TraversalContext.of(debug), downPreds);
        }

        private void closeOwned(IStrategoTerm var, TraversalContext ctx, Set<IStrategoTerm> downPreds) {
            if(isRelationArg(var) || isConstructorArg(var) || nodeInfo.isClosed(var)) {
                return;
            }

            ctx.log("closing {}.", var);
            nodeInfo.close(var);

            for(IStrategoTerm tgt : cg.directedEdges.incomingEdges(var).elementSet()) {
                final Edge<IStrategoTerm> edge = Edge.backward(var, tgt, cg.directedEdges.count(tgt, var));
                if(!ctx.visited(edge.invert())) {
                    closeOwned(tgt, ctx.withVisited(edge).increaseIndent(), downPreds);
                }
            }

            for(IStrategoTerm tgt : cg.undirectedEdges.edges(var).elementSet()) {
                final Edge<IStrategoTerm> edge = Edge.undirected(var, tgt, cg.undirectedEdges.count(tgt, var));
                if(!ctx.visited(edge.invert())) {
                    closeOwned(tgt, ctx.withVisited(edge).increaseIndent(), downPreds);
                }
            }

            for(IStrategoTerm tgt : cg.directedEdges.outgoingEdges(var).elementSet()) {
                final Edge<IStrategoTerm> edge = Edge.forward(var, tgt, cg.directedEdges.count(var, tgt));
                if(!ctx.visited(edge.invert())) {
                    if(isPredicateArg(tgt) || isRelationArg(var)) {
                        ctx.log("- down-pred {}.", tgt);
                        downPreds.add(tgt);
                    }
                }
            }
        }

        private void closeOwnedTransitive(IStrategoTerm var) {
            closeOwnedTransitiveDown(var, TraversalContext.of(debug));
        }

        private void closeOwnedTransitive(IStrategoTerm var, TraversalContext ctx) {
            for(IStrategoTerm tgt : cg.directedEdges.outgoingEdges(var).elementSet()) {
                final Edge<IStrategoTerm> edge = Edge.forward(var, tgt, cg.directedEdges.count(var, tgt));
                if(!ctx.visited(edge.invert())) {
                    closeOwnedTransitiveDown(tgt, ctx.withVisited(edge).increaseIndent());
                }
            }

            for(IStrategoTerm tgt : cg.directedEdges.incomingEdges(var).elementSet()) {
                final Edge<IStrategoTerm> edge = Edge.backward(var, tgt, cg.directedEdges.count(tgt, var));
                if(ctx.visited(edge.invert())) {
                    closeOwnedTransitiveUp(tgt, ctx.withVisited(edge).increaseIndent());
                }
            }

            for(IStrategoTerm tgt : cg.undirectedEdges.edges(var).elementSet()) {
                final Edge<IStrategoTerm> edge = Edge.undirected(var, tgt, cg.undirectedEdges.count(tgt, var));
                if(ctx.visited(edge.invert())) {
                    closeOwnedTransitiveDown(tgt, ctx.withVisited(edge).increaseIndent());
                }
            }
        }

        private void closeOwnedTransitiveDown(IStrategoTerm var, TraversalContext ctx) {
            ctx.log("close trans {}.", var);

            if(isRelationArg(var) || isConstructorArg(var)) {
                ctx.log("STOP: {} not closable.", var);
                return;
            }
            if(nodeInfo.isClosed(var)) {
                ctx.log("STOP: {} already closed.", var);
                return;
            }

            if((isPredicateArg(var) || isRelationArg(var))
                    && !cg.directedEdges.incomingEdges(var).elementSet().stream().anyMatch(nodeInfo::isClosed)) {
                // No proof (yet) that _all_ inputs are closed.
                ctx.log("STOP: {} not all inputs closed.", var);
                return;
            }

            ctx.log("closing (transitive-down) {}.", var);
            nodeInfo.close(var);
            closeOwnedTransitive(var, ctx);
        }

        private void closeOwnedTransitiveUp(IStrategoTerm var, TraversalContext ctx) {
            if(isRelationArg(var) || isConstructorArg(var)) {
                ctx.log("STOP: {} not closable.", var);
                return;
            }
            if(nodeInfo.isClosed(var)) {
                ctx.log("STOP: {} already closed.", var);
                return;
            }


            if(isPredicateArg(var)
                    && !cg.directedEdges.outgoingEdges(var).elementSet().stream().anyMatch(nodeInfo::isClosed)) {
                ctx.log("STOP: {} not all outputs closed.", var);
                return;
            }

            ctx.log("closing (transitive-up) {}.", var);
            nodeInfo.close(var);
            closeOwnedTransitive(var, ctx);
        }

        // Phase 3. Propagate info obtained through queries or term matching/building.

        private void propagateRemoteTypes(IStrategoTerm var, TraversalContext ctx) {
            ctx.log("{}: propagating remote types.", var);

            final HashSet<IStrategoTerm> targets = new HashSet<>();
            targets.addAll(cg.directedEdges.outgoingEdges(var).elementSet());
            targets.addAll(cg.undirectedEdges.edges(var).elementSet());
            targets.addAll(cg.directedEdges.incomingEdges(var).elementSet());


            for(IStrategoTerm tgt : targets) {
                ctx.log("- target : {}", tgt);
                if(nodeInfo.isClosed(tgt)) {
                    ctx.log("  STOP: {} closed.", tgt);
                    continue;
                }
                for(IStrategoTerm type : nodeInfo.getCardinalities(var).keySet()) {
                    if(!Cardinality.ZERO2INFINITE
                            .equals(nodeInfo.setCardinality(tgt, type, Cardinality.ZERO2INFINITE))) {
                        propagateRemoteTypes(tgt, ctx.increaseIndent());
                    } else {
                        ctx.log("  Reached fixpoint at {}.", tgt);
                    }
                }
            }
        }

        // Phase 4. Propagate unknown node info

        private void propagateUnknown(IStrategoTerm var, IStrategoTerm type, TraversalContext ctx) {
            if(nodeInfo.hasCardinality(var, type)) {
                ctx.log("STOP: {} processed.", var);
                return;
            }
            if(nodeInfo.isClosed(var)) {
                ctx.log("STOP: {} closed.", var);
                return;
            }

            ctx.log("Set {}.{} to {}.", var, type, Cardinality.ZERO2INFINITE);
            nodeInfo.setCardinality(var, type, Cardinality.ZERO2INFINITE);

            final HashSet<IStrategoTerm> tgts = new HashSet<>();
            tgts.addAll(cg.directedEdges.outgoingEdges(var).elementSet());
            tgts.addAll(cg.directedEdges.incomingEdges(var).elementSet());
            tgts.addAll(cg.undirectedEdges.edges(var).elementSet());

            for(IStrategoTerm tgt : tgts) {
                propagateUnknown(tgt, type, ctx.increaseIndent());
            }
        }

        // Phase 5. Build Schema Term

        public IStrategoTerm buildEdgeTerm(IStrategoTerm edge_rule) {
            final IStrategoTerm edge = edge_rule.getSubterm(0);
            final IStrategoTerm rule = edge_rule.getSubterm(1);

            final IStrategoTerm src = mkVariable(edge.getSubterm(0));
            final IStrategoTerm lbl = edge.getSubterm(1);
            final IStrategoTerm tgt = mkVariable(edge.getSubterm(2));

            return TF.makeAppl("SGEdge", buildScopeKindCardList(src), lbl, buildScopeKindCardList(tgt), rule);
        }

        public IStrategoTerm buildDeclTerm(IStrategoTerm decl_rule) {
            final IStrategoTerm decl = decl_rule.getSubterm(0);
            final IStrategoTerm rule = decl_rule.getSubterm(1);

            final IStrategoTerm rel = decl.getSubterm(0);
            final IStrategoTerm args = decl.getSubterm(1);
            final IStrategoTerm scope = mkVariable(decl.getSubterm(2));

            return TF.makeAppl("SGDecl", buildScopeKindCardList(scope), rel, buildDataList(args), rule);
        }

        public IStrategoList buildScopeKindCardList(IStrategoTerm var) {
            return TF.makeList(nodeInfo.getCardinalities(var).entrySet(),
                    e -> TF.makeAppl("ScopeKindWithCard", e.getKey(), e.getValue().makeTerm(TF)));
        }

        public IStrategoList buildDataList(IStrategoTerm args) {
            return TF.makeList(args.getSubterms(), arg -> {
                final IStrategoTerm var = mkVariable(arg);
                final Map<IStrategoTerm, Cardinality> types = nodeInfo.getCardinalities(var);
                if(types.isEmpty()) {
                    return TF.makeAppl("DData");
                }
                return TF.makeAppl("DScope", buildScopeKindCardList(var));
            });
        }

        // Utilities

        public IStrategoTerm mkVariable(IStrategoTerm var) {
            return TF.makeAppl("Variable", var);
        }

        public IStrategoTerm mkVarKind(IStrategoTerm var) {
            return TF.makeAppl("KVar", var);
        }

    }

    private static class TraversalContext {

        private final Immutable<IStrategoTerm> visitedNodes;

        private final Immutable<Edge<IStrategoTerm>> visitedEdges;

        private final String indent;

        private final boolean debug;

        private TraversalContext(Immutable<IStrategoTerm> visitedNodes, Immutable<Edge<IStrategoTerm>> visitedEdges,
                String indent, boolean debug) {
            this.visitedNodes = visitedNodes;
            this.visitedEdges = visitedEdges;
            this.indent = indent;
            this.debug = debug;
        }

        public boolean visited(IStrategoTerm var) {
            return visitedNodes.contains(var);
        }

        public boolean visited(Edge<IStrategoTerm> edge) {
            return visitedEdges.contains(edge);
        }

        public TraversalContext withVisited(IStrategoTerm var) {
            return new TraversalContext(visitedNodes.__insert(var), visitedEdges, indent, debug);
        }

        public TraversalContext withVisited(Edge<IStrategoTerm> edge) {
            return new TraversalContext(visitedNodes, visitedEdges.__insert(edge), indent, debug);
        }

        public TraversalContext increaseIndent() {
            return new TraversalContext(visitedNodes, visitedEdges, indent + "  ", debug);
        }

        public TraversalContext reset() {
            return new TraversalContext(CapsuleUtil.immutableSet(), CapsuleUtil.immutableSet(), indent, debug);
        }

        public static TraversalContext of(boolean debug) {
            return new TraversalContext(CapsuleUtil.immutableSet(), CapsuleUtil.immutableSet(), "", debug);
        }

        public void log(String fmt, Object... args) {
            if(debug) {
                log.info(indent + fmt, args);
            }
        }

    }

    private class ConstraintGraph<N> {

        public final Set<N> nodes = new HashSet<>();

        public final DirectedEdgeSet<N> directedEdges = new DirectedEdgeSet<>();

        public final UndirectedEdgeSet<N> undirectedEdges = new UndirectedEdgeSet<>();

        public void addDirectedEdge(N src, N tgt) {
            nodes.add(src);
            nodes.add(tgt);
            directedEdges.addEdge(src, tgt);
        }

        public void makeDirected(N source, N target) {
            int c = undirectedEdges.removeEdge(source, target);
            directedEdges.addEdge(source, target, c);
        }

        public void addUndirectedEdge(N src, N tgt) {
            nodes.add(src);
            nodes.add(tgt);
            undirectedEdges.addEdge(src, tgt);
        }

        @Override public String toString() {
            return "ConstraintGraph [directedEdges=" + directedEdges + ", undirectedEdges=" + undirectedEdges + "]";
        }

    }

    private final class DirectedEdgeSet<N> {

        private MultiSetMap.Immutable<N, N> edges = MultiSetMap.Immutable.of();

        private MultiSetMap.Immutable<N, N> reverseEdges = MultiSetMap.Immutable.of();

        public void addEdge(N src, N tgt) {
            addEdge(src, tgt, 1);
        }

        public void addEdge(N src, N tgt, int n) {
            edges = edges.put(src, tgt, n);
            reverseEdges = reverseEdges.put(tgt, src, n);
        }

        public MultiSet.Immutable<N> outgoingEdges(N node) {
            return edges.get(node);
        }

        public MultiSet.Immutable<N> incomingEdges(N src) {
            return reverseEdges.get(src);
        }

        public int count(N src, N tgt) {
            return edges.count(src, tgt);
        }

        @Override public String toString() {
            return edges.toString();
        }

    }

    private class UndirectedEdgeSet<N> {

        private MultiSetMap.Immutable<N, N> edges = MultiSetMap.Immutable.of();

        public void addEdge(N src, N tgt) {
            edges = edges.put(src, tgt).put(tgt, src);
        }

        public int removeEdge(N src, N tgt) {
            int c = count(src, tgt);
            if(count(tgt, src) != c) {
                throw new IllegalStateException("Direction count BUG!");
            }
            for(int i = 0; i < c; i++) {
                edges = edges.remove(src, tgt).remove(tgt, src);
            }
            if(count(src, tgt) != 0) {
                throw new IllegalStateException("Remove BUG!");
            }
            return c;
        }

        public MultiSet.Immutable<N> edges(N src) {
            return edges.get(src);
        }

        public int count(N src, N tgt) {
            return edges.count(src, tgt);
        }

        @Override public String toString() {
            return edges.toString();
        }
    }

    private static class VariableInfo<V, T> {

        private final Map<V, Map<T, Cardinality>> nodeInfo = new HashMap<>();

        private final Set<V> closed = new HashSet<>();

        public boolean hasCardinality(V var, T type) {
            return getMapValue(nodeInfo, var).containsKey(type);
        }

        public boolean hasCardinality(V var) {
            return !getMapValue(nodeInfo, var).isEmpty();
        }

        public Cardinality getCardinality(V var, T type) {
            return getMapValue(nodeInfo, var).get(type);
        }

        public Cardinality getCardinalityOrDefault(V var, T type, Cardinality defaultCard) {
            return getMapValue(nodeInfo, var).getOrDefault(type, defaultCard);
        }

        public Map<T, Cardinality> getCardinalities(V var) {
            return getMapValue(nodeInfo, var);
        }

        public boolean isClosed(V var) {
            return closed.contains(var);
        }

        public boolean isOpen(V var) {
            return !closed.contains(var);
        }

        public Cardinality setCardinality(V var, T type, Cardinality card) {
            if(isClosed(var)) {
                throw new IllegalStateException(var + " is already closed.");
            }
            return getMapValue(nodeInfo, var).put(type, card);
        }

        public boolean close(V var) {
            return closed.add(var);
        }
    }

    public static class Cardinality {

        public static final int INFINITE_BOUND = -1;

        public static final Cardinality ZERO = new Cardinality(0, 0);
        public static final Cardinality ONE = new Cardinality(1, 1);
        public static final Cardinality ZERO2INFINITE = new Cardinality(0, INFINITE_BOUND);
        public static final Cardinality INFINITE = new Cardinality(INFINITE_BOUND, INFINITE_BOUND);

        private final int lower;

        private final int upper;

        public Cardinality(int lower, int upper) {
            if(!boundSmallerEq(lower, upper)) {
                throw new IllegalArgumentException("Incompatible bounds: " + lower + " > " + upper);
            }
            if(lower < -1 || upper < -1) {
                throw new IllegalArgumentException("Cannot have negative bounds: [" + lower + ", " + upper + "]");
            }
            this.lower = lower;
            this.upper = upper;
        }

        public int getLower() {
            return lower;
        }

        public int getUpper() {
            return upper;
        }

        public Cardinality withLower(int lower) {
            return new Cardinality(lower, upper);
        }

        public Cardinality withUpper(int upper) {
            return new Cardinality(lower, upper);
        }

        public Cardinality add(Cardinality other) {
            return new Cardinality(addBound(lower, other.lower), addBound(upper, other.upper));
        }

        private int addBound(int b1, int b2) {
            if(b1 == INFINITE_BOUND || b2 == INFINITE_BOUND) {
                return INFINITE_BOUND;
            }
            return b1 + b2;
        }

        public Cardinality mult(int factor) {
            return new Cardinality(multBound(lower, factor), multBound(upper, factor));
        }

        private int multBound(int b, int factor) {
            if(b == INFINITE_BOUND) {
                return INFINITE_BOUND;
            }
            return b * factor;
        }

        private boolean boundSmallerEq(int b1, int b2) {
            return b2 == INFINITE_BOUND || (b1 != INFINITE_BOUND && b1 <= b2);
        }

        public boolean encloses(Cardinality other) {
            return boundSmallerEq(this.lower, other.lower) && boundSmallerEq(other.upper, this.upper);
        }

        public IStrategoTerm makeTerm(ITermFactory TF) {
            return TF.makeAppl("Cardinality", boundToTerm(lower, TF), boundToTerm(upper, TF));
        }

        private IStrategoTerm boundToTerm(int bound, ITermFactory TF) {
            if(bound == -1) {
                return TF.makeAppl("INF");
            }
            return TF.makeAppl("BNum", TF.makeInt(bound));
        }

        @Override public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + lower;
            result = prime * result + upper;
            return result;
        }

        @Override public boolean equals(Object obj) {
            if(this == obj)
                return true;
            if(obj == null)
                return false;
            if(getClass() != obj.getClass())
                return false;
            Cardinality other = (Cardinality) obj;
            if(lower != other.lower)
                return false;
            if(upper != other.upper)
                return false;
            return true;
        }

        @Override public String toString() {
            return boundToString(lower) + ".." + boundToString(upper);
        }

        private String boundToString(int bound) {
            if(bound == INFINITE_BOUND) {
                return "INF";
            }
            return Integer.toString(bound);
        }
    }

    private static class Edge<N> {

        private final N source; // traversal source
        private final N target; // traversal target
        private final Direction direction;
        private final int weigth;

        public Edge(N source, N target, Direction direction, int weigth) {
            this.source = source;
            this.target = target;
            this.direction = direction;
            this.weigth = weigth;
        }

        public N getSource() {
            return source;
        }

        public N getTarget() {
            return target;
        }

        public Direction getDirection() {
            return direction;
        }

        public int getWeigth() {
            return weigth;
        }

        public Edge<N> invert() {
            return new Edge<>(target, source, direction.invert(), weigth); // same edge, opposite direction
        }

        @Override public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((direction == null) ? 0 : direction.hashCode());
            result = prime * result + ((source == null) ? 0 : source.hashCode());
            result = prime * result + ((target == null) ? 0 : target.hashCode());
            return result;
        }

        @SuppressWarnings("rawtypes") @Override public boolean equals(Object obj) {
            if(this == obj)
                return true;
            if(obj == null)
                return false;
            if(getClass() != obj.getClass())
                return false;
            Edge other = (Edge) obj;
            if(direction != other.direction)
                return false;
            if(weigth != other.weigth)
                return false;
            if(source == null) {
                if(other.source != null)
                    return false;
            } else if(!source.equals(other.source))
                return false;
            if(target == null) {
                if(other.target != null)
                    return false;
            } else if(!target.equals(other.target))
                return false;
            return true;
        }


        public static <N> Edge<N> forward(N source, N target, int weight) {
            return new Edge<N>(source, target, Direction.FORWARD, weight);
        }

        public static <N> Edge<N> backward(N source, N target, int weight) {
            return new Edge<N>(source, target, Direction.BACKWARD, weight);
        }

        public static <N> Edge<N> undirected(N source, N target, int weight) {
            return new Edge<N>(source, target, Direction.UNDIRECTED, weight);
        }
    }


    private enum Direction {
        FORWARD, BACKWARD, UNDIRECTED;

        public Direction invert() {
            switch(this) {
                case FORWARD:
                    return BACKWARD;
                case BACKWARD:
                    return FORWARD;
                default:
                    return this;
            }
        }
    }

}
