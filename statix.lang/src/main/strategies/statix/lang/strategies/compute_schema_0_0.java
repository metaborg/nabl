package statix.lang.strategies;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Supplier;

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

public class compute_schema_0_0 extends Strategy {

    private static final ILogger log = LoggerUtils.logger(compute_schema_0_0.class);

    public static final Strategy instance = new compute_schema_0_0();

    @Override public IStrategoTerm invoke(Context context, IStrategoTerm current) {
        // 0. Initialization
        final IStrategoTuple args = TermUtils.asTuple(current).get();
        final IStrategoTerm types = args.get(0);
        final IStrategoTerm edges = args.get(1);
        final IStrategoTerm decls = args.get(2);
        final IStrategoTerm constraints = args.get(3);

        final ConstraintGraph<IStrategoTerm> cg = createConstraintGraph(constraints);
        log.info("Constraint graph: {}.", cg);

        // 1. Forward propagating node type info
        //    This ensures that the types of all _owner_ scopes (and unification assertions thereof)
        //    are propagated through all predicate calls.
        //

        // Mapping :: Variable -> ScopeType -> Cardinality
        final Map<IStrategoTerm, Map<IStrategoTerm, Cardinality>> initialNodeInfo = new HashMap<>();
        // Variables to be used as starting point for next phase.
        final HashSet<IStrategoTerm> nextPhase = new HashSet<>();
        for(IStrategoTerm type : types) {
            propagateOwnedTypes(type, initialNodeInfo, cg, context.getFactory(), nextPhase);
        }

        // 2. Propagate remainder of constraints
        final Map<IStrategoTerm, Map<IStrategoTerm, Cardinality>> nextNodeInfo = new HashMap<>();
        for(IStrategoTerm var : nextPhase) {

        }

        for(IStrategoTerm var : initialNodeInfo.keySet()) {
            log.info("{}", var);
            final Map<IStrategoTerm, Cardinality> cards = initialNodeInfo.get(var);
            for(Map.Entry<IStrategoTerm, Cardinality> card : cards.entrySet()) {
                final IStrategoTerm type = card.getKey();
                log.info("* {}: {}", type, card.getValue());
            }
        }
        log.info("Next phase: {}.", nextPhase);

        return current;
    }

    private ConstraintGraph<IStrategoTerm> createConstraintGraph(IStrategoTerm constraints) {
        final ConstraintGraph<IStrategoTerm> cg = new ConstraintGraph<>();

        for(IStrategoTerm edgeTerm : constraints) {
            if(TermUtils.isAppl(edgeTerm, "DEdge", 2)) {
                cg.addDirectedEdge(edgeTerm.getSubterm(0), edgeTerm.getSubterm(1));
            } else if(TermUtils.isAppl(edgeTerm, "UEdge", 2)) {
                cg.addUndirectedEdge(edgeTerm.getSubterm(0), edgeTerm.getSubterm(1));
            } else {
                throw new IllegalArgumentException("Unrecognized constraint: " + edgeTerm);
            }
        }

        return cg;
    }

    // Phase 1. Propagate info of owned scopes to all positions where it will propagate with certainty.

    private void propagateOwnedTypes(IStrategoTerm type, Map<IStrategoTerm, Map<IStrategoTerm, Cardinality>> nodeInfo,
            ConstraintGraph<IStrategoTerm> cg, ITermFactory TF, Set<IStrategoTerm> nextPhase) {
        propagateOwnedTypes(mkVariable(type, TF), type, nodeInfo, cg, Cardinality.ONE, CapsuleUtil.immutableSet(),
                CapsuleUtil.immutableSet(), nextPhase, "");
    }

    private void propagateOwnedTypes(IStrategoTerm var, IStrategoTerm type,
            Map<IStrategoTerm, Map<IStrategoTerm, Cardinality>> nodeInfo, ConstraintGraph<IStrategoTerm> cg,
            Cardinality card, Immutable<IStrategoTerm> visitedNodes, Immutable<Edge<IStrategoTerm>> visitedEdges,
            Set<IStrategoTerm> nextPhase, String indent) {
        final Map<IStrategoTerm, Cardinality> varInfo = getMapValue(nodeInfo, var);
        final Cardinality prevCard = varInfo.getOrDefault(type, Cardinality.ZERO);

        // 1. Update the current cardinality
        final Cardinality newCard;
        final Immutable<Edge<IStrategoTerm>> newVisitedEdges;
        final Immutable<IStrategoTerm> newVisitedNodes;
        if(visitedNodes.contains(var)) {
            // cycle in 'call graph': unbounded recursion
            if(cg.directedEdges.incomingEdges(var).elementSet().size() == 1 && prevCard.getLower() != 0) {
                // No other branch: infinite recursion
                newCard = Cardinality.INFINITE;
            } else {
                newCard = Cardinality.ZERO2INFINITE;
            }
            newVisitedEdges = CapsuleUtil.immutableSet();
            newVisitedNodes = CapsuleUtil.immutableSet();
        } else {
            newCard = card;
            newVisitedEdges = visitedEdges; // edge inserted by traversal helper
            newVisitedNodes = visitedNodes.__insert(var);
        }

        if(prevCard.equals(newCard)) {
            // No new info, stop propagation
            log.info("{}STOP: fixpoint.", indent);
            return;
        }

        log.info("{}Updating cardinality {}.{} to {} (was {}).", indent, var, type, newCard, prevCard);
        varInfo.put(type, newCard);
        if(isConstructorArg(var) || isRelationArg(var)) {
            // Terminal node for first propagation
            nextPhase.add(var);
            log.info("{}STOP: first-phase terminal point.", indent);
            return;
        }


        // 2. Traverse Directed Edges
        final MultiSet.Immutable<IStrategoTerm> edges = cg.directedEdges.outgoingEdges(var);
        for(Map.Entry<IStrategoTerm, Integer> edge : edges.entrySet()) {
            final IStrategoTerm tgt = edge.getKey();
            final int count = edge.getValue();
            log.info("{}Traversing directed edge {}->{} ({} * {})", indent, var, tgt, newCard, count);
            propagateOwnedTypes(Edge.forward(var, tgt, count), type, nodeInfo, cg, newCard, newVisitedNodes,
                    newVisitedEdges, nextPhase, indent);
        }


        // 3. Back-Traverse owned scopes 'leaking' though direct predicate arguments
        final MultiSet.Immutable<IStrategoTerm> invEdges = cg.directedEdges.incomingEdges(var);
        for(Map.Entry<IStrategoTerm, Integer> edge : invEdges.entrySet()) {
            final IStrategoTerm tgt = edge.getKey();
            final int count = edge.getValue();
            if(isPredicateArg(tgt) && !newVisitedNodes.contains(tgt)) {
                if(cg.directedEdges.outgoingEdges(tgt).elementSet().size() == 1) {
                    // The current rule is the only rule for the predicate,
                    // so currently owned scope will be the correct instance
                    // for the parent as well.
                    final Cardinality subCard = newCard.mult(count);
                    log.info("{}Back-propagating {}->{} ({} * {}).", indent, var, tgt, newCard, count);
                    getMapValue(nodeInfo, tgt).put(type, subCard);

                    final Set<Entry<IStrategoTerm, Integer>> backEdges = cg.directedEdges.incomingEdges(tgt).entrySet();
                    for(Map.Entry<IStrategoTerm, Integer> backEdge : backEdges) {
                        log.info("{}Back-traversing directed edge {}->{} ({} * {})", indent, var, tgt, subCard, backEdge.getValue());
                        propagateOwnedTypes(Edge.backward(tgt, backEdge.getKey(), backEdge.getValue()), type, nodeInfo, cg, subCard,
                                newVisitedNodes.__insert(tgt), newVisitedEdges.__insert(Edge.backward(var, tgt, count)),
                                nextPhase, indent);
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
            log.info("{}Traversing undirected edge {}->{} ({} * {})", indent, var, tgt, newCard, count);
            propagateOwnedTypes(Edge.undirected(var, tgt, count), type, nodeInfo, cg, newCard, newVisitedNodes,
                    newVisitedEdges, nextPhase, indent);
        }

    }

    private void propagateOwnedTypes(Edge<IStrategoTerm> edge, IStrategoTerm type,
            Map<IStrategoTerm, Map<IStrategoTerm, Cardinality>> nodeInfo, ConstraintGraph<IStrategoTerm> cg,
            Cardinality card, Immutable<IStrategoTerm> visitedNodes, Immutable<Edge<IStrategoTerm>> visitedEdges,
            Set<IStrategoTerm> nextPhase, String indent) {
        if(visitedEdges.contains(edge.invert())) {
            log.info("{}STOP: edge already traversed in other direction.", indent);
            return;
        }

        final Cardinality updatedCard = getMapValue(nodeInfo, edge.getSource()).get(type);
        if(!card.equals(updatedCard)) {
            log.info("{}STOP: Recursive update detected (cur: {}, updated: {}).", indent, card, updatedCard);
            return;
        }

        final IStrategoTerm target = edge.getTarget();
        final IStrategoTerm source = edge.getSource();

        // Properly manage distribution of scopes over edge instantiation.
        final Cardinality newCard;
        if(isPredicateArg(target) && edge.getDirection() == Direction.FORWARD) {
            // Compute additive aggregation of all incoming cardinalities
            Cardinality aggCard = null;
            for(Map.Entry<IStrategoTerm, Integer> inEdge : cg.directedEdges.incomingEdges(target).entrySet()) {
                final IStrategoTerm inSource = inEdge.getKey();
                final int count = inEdge.getValue();
                final Cardinality oldCard = getMapValue(nodeInfo, inSource).get(type);
                final boolean currentSource = inSource.equals(source);
                final Cardinality edgeCard;
                if(oldCard == null) {
                    if(currentSource) {
                        // cannot happen, invoking method must have set value
                        throw new IllegalStateException("BUG!");
                    } else {
                        // no value set, does not influence next cardinality
                        log.info("{}- no value for {}, ignoring.", indent, inSource);
                        continue;
                    }
                } else {
                    edgeCard = (currentSource ? card.mult(edge.getWeigth()) : oldCard.mult(count));
                }
                if(aggCard == null) {
                    aggCard = edgeCard;
                    log.info("{}- initial value {}.", indent, aggCard);
                } else {
                    aggCard = aggCard.add(edgeCard);
                    log.info("{}- aggregated value {}.", indent, aggCard);
                }
            }
            newCard = aggCard;
        } else if(isPredicateArg(source) && edge.getDirection() == Direction.FORWARD
                && cg.directedEdges.outgoingEdges(source).elementSet().size() > 1) {
            // nullify due to branching possibility
            newCard = card.mult(edge.getWeigth()).withLower(0);
        } else {
            newCard = card.mult(edge.getWeigth());
        }

        // Update direction of undirected edges
        if(isConstructorArg(target) || isRelationArg(target) && edge.getDirection().equals(Direction.UNDIRECTED)) {
            cg.makeDirected(source, target);
        }

        propagateOwnedTypes(target, type, nodeInfo, cg, newCard, visitedNodes, visitedEdges.__insert(edge), nextPhase,
                indent + "  ");
    }

    // Phase 2. Propagate info obtained through queries or term matching/building.



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

    public IStrategoTerm mkVariable(IStrategoTerm var, ITermFactory TF) {
        return TF.makeAppl("Variable", var);
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

    private static class Cardinality {

        public static final int INFINITE_BOUND = -1;

        public static final Cardinality ZERO = new Cardinality(0, 0);
        public static final Cardinality ONE = new Cardinality(1, 1);
        public static final Cardinality ZERO2INFINITE = new Cardinality(0, INFINITE_BOUND);
        public static final Cardinality INFINITE = new Cardinality(INFINITE_BOUND, INFINITE_BOUND);

        private final int lower;

        private final int upper;

        public Cardinality(int lower, int upper) {
            if(upper != INFINITE_BOUND && lower > upper) {
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

        @SuppressWarnings("rawtypes")
        @Override public boolean equals(Object obj) {
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
