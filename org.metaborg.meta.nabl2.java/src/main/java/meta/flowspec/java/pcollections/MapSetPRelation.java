package meta.flowspec.java.pcollections;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.pcollections.Empty;
import org.pcollections.PMap;
import org.pcollections.PSet;

public class MapSetPRelation<L, R> implements PRelation<L, R> {
    protected final PMap<L, PSet<R>> map;

    public MapSetPRelation() {
        this.map = Empty.map();
    }

    protected MapSetPRelation(PRelation<L, R> rel) {
        PMap<L, PSet<R>> map = Empty.map();
        for (Map.Entry<L, R> e : rel.entrySet()) {
            PSet<R> rhs = map.get(e.getKey());
            if (rhs == null) {
                rhs = Empty.set();
            }
            map = map.plus(e.getKey(), rhs.plus(e.getValue()));
        }
        this.map = map;
    }

    protected MapSetPRelation(PMap<L, PSet<R>> map) {
        this.map = map;
    }

    @Override
    public PRelation<L, R> plus(L lhs, R rhs) {
        return new MapSetPRelation<>(this.map.plus(lhs, this.getRhsSet(lhs).plus(rhs)));
    }

    @Override
    public PRelation<L, R> plusAll(PRelation<? extends L, ? extends R> map) {
        PRelation<L, R> result = this;
        for (Map.Entry<? extends L, ? extends R> e : map.entrySet()) {
            result = result.plus(e.getKey(), e.getValue());
        }
        return result;
    }

    @Override
    public PRelation<L, R> plusAll(Map<? extends L, ? extends R> map) {
        PRelation<L, R> result = this;
        for (Map.Entry<? extends L, ? extends R> e : map.entrySet()) {
            result = result.plus(e.getKey(), e.getValue());
        }
        return result;
    }

    @Override
    public PRelation<L, R> minus(L lhs, R rhs) {
        PSet<R> newRhsSet = this.getRhsSet(lhs).minus(rhs);
        if (newRhsSet.isEmpty()) {
            return new MapSetPRelation<>(this.map.minus(lhs));
        }
        return new MapSetPRelation<>(this.map.plus(lhs, newRhsSet));
    }

    @Override
    public PRelation<L, R> minusAll(PRelation<? extends L, ? extends R> map) {
        PRelation<L, R> result = this;
        for (Map.Entry<? extends L, ? extends R> e : map.entrySet()) {
            result = result.minus(e.getKey(), e.getValue());
        }
        return result;
    }

    @Override
    public PRelation<L, R> minusAll(Map<? extends L, ? extends R> map) {
        PRelation<L, R> result = this;
        for (Map.Entry<? extends L, ? extends R> e : map.entrySet()) {
            result = result.minus(e.getKey(), e.getValue());
        }
        return result;
    }

    @Override
    public int size() {
        return this.map.size();
    }

    @Override
    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    @Override
    public boolean contains(L lhs, R rhs) {
        return this.getRhsSet(lhs).contains(rhs);
    }

    @Override
    public boolean containsLhs(L lhs) {
        return this.map.containsKey(lhs);
    }

    @Override
    public boolean containsRhs(R rhs) {
        return this.rhsSet().contains(rhs);
    }

    @Override
    public PSet<R> getRhsSet(L lhs) {
        PSet<R> rhs = this.map.get(lhs);
        if (rhs == null) {
            rhs = Empty.set();
        }
        return rhs;
    }

    @Override
    public Set<L> lhsSet() {
        return this.map.keySet();
    }

    private PSet<R> cachedRhsSet = null;

    @Override
    public PSet<R> rhsSet() {
        if (cachedRhsSet == null) {
            cachedRhsSet = this.map.entrySet().stream().map(e -> (PSet<R>) e.getValue()).reduce(Empty.set(),
                    (set1, set2) -> set1.plusAll(set2));
        }
        return cachedRhsSet;
    }

    private Set<Map.Entry<L, R>> cachedEntrySet = null;

    @Override
    public Set<Map.Entry<L, R>> entrySet() {
        if (cachedEntrySet == null) {
            cachedEntrySet = this.map.entrySet().stream()
                    .flatMap(e -> e.getValue().stream().map(r -> new Entry<>(e.getKey(), r)))
                    .collect(Collectors.toSet());
        }
        return cachedEntrySet;
    }

    public static class Entry<L, R> implements Map.Entry<L, R> {
        private L lhs;
        private R rhs;

        public Entry(L lhs, R rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
        }

        @Override
        public L getKey() {
            return this.lhs;
        }

        @Override
        public R getValue() {
            return this.rhs;
        }

        @Override
        public R setValue(R value) {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public PRelation<R, L> reverse() {
        PRelation<R, L> result = new MapSetPRelation<>();
        for (Map.Entry<L, R> e : entrySet()) {
            result = result.plus(e.getValue(), e.getKey());
        }
        return result;
    }

    /**
     * Interprets the relation as an edge list for a graph and gives a topological sorted list of its nodes.
     * The used algorithm is due to Kahn (1962): https://en.wikipedia.org/wiki/Topological_sorting#Kahn.27s_algorithm
     * 
     * @param rel The relation / graph
     * @return A list of "vertices" in topological order, or an empty optional if there are cycles in the graph
     */
    public static <E> Optional<List<E>> topoSort(PRelation<E, E> rel) {
        List<E> result = new ArrayList<>();
        // The frontier is initialised with nodes that have no incoming edges.
        Set<E> frontier = new HashSet<>(rel.lhsSet());
        frontier.removeAll(rel.rhsSet());

        // (1) Move the nodes from the frontier (no incoming edges) to the result
        // (2) Remove outgoing edges of each node moved from frontier to result
        // (3) Add nodes that now no longer have incoming edges to frontier
        while (!frontier.isEmpty()) {
            E node = frontier.iterator().next();
            frontier.remove(node);
            result.add(node);
            for (E rhs : rel.getRhsSet(node)) {
                rel = rel.minus(node, rhs);
                if (!rel.containsRhs(rhs)) {
                    frontier.add(rhs);
                }
            }
        }
        // If graph is not empty when the frontier became empty, there must be a cycle in the graph
        if (!rel.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(result);
    }

    /**
     * Interprets the relation as an edge list for a graph and gives a reverse topologically sorted list of its
     * strongly connected components (SCCs).
     * The used algorithm is due to Tarjan (1972): https://en.wikipedia.org/wiki/Tarjan%27s_strongly_connected_components_algorithm
     * 
     * @param rel The relation / graph
     * @return A list of SCCs in reverse topological order
     */
    public static <E> List<Set<E>> revTopoSCCs(PRelation<E, E> rel) {
        // TODO: implement
        throw new RuntimeException("Unimplemented");
    }
}
