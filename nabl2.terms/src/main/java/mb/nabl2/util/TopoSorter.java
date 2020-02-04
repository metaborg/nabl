package mb.nabl2.util;

import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.metaborg.util.functions.Function1;
import org.metaborg.util.functions.Predicate1;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import io.usethesource.capsule.Set;
import io.usethesource.capsule.Set.Immutable;
import mb.nabl2.util.collections.IndexedDeque;

public class TopoSorter {

    private TopoSorter() {
    }

    /**
     * Returns a topologically sorted list of strongly-connected components, as determined by the next relation.
     * 
     * @param nodes
     * @param nextNodes
     * @param implicitAdd
     *            Implicitly add next nodes not in the initial set of nodes
     * @return
     */
    public static <E> TopoSortedComponents<E> toposort(Iterable<? extends E> nodes,
            Function1<E, Iterable<? extends E>> nextNodes, boolean implicitAdd) {
        final java.util.Set<? extends E> nodeSet = ImmutableSet.copyOf(nodes);
        final ImmutableList.Builder<Set.Immutable<E>> components = ImmutableList.builder();
        final AtomicInteger index = new AtomicInteger(0);
        final Deque<Node<E>> stack = new IndexedDeque<>();
        final Map<E, Node<E>> visited = Maps.newHashMap();
        final Predicate1<E> include = n -> implicitAdd || nodeSet.contains(n);
        for(E current : nodeSet) {
            if(!visited.containsKey(current)) {
                strongconnect(current, index, stack, visited, components, nextNodes, include);
            }
        }
        return new TopoSortedComponents<>(components.build().reverse());
    }

    private static <E> Node<E> strongconnect(E current, AtomicInteger index, Deque<Node<E>> stack,
            Map<E, Node<E>> visited, ImmutableList.Builder<Set.Immutable<E>> components,
            Function1<E, Iterable<? extends E>> nextNodes, Predicate1<E> include) {
        Node<E> v;
        visited.put(current, v = new Node<>(current, index.getAndIncrement()));
        stack.push(v);
        for(E next : nextNodes.apply(current)) {
            if(!include.test(next)) {
                continue;
            }
            Node<E> w = visited.get(next);
            if(w == null) {
                w = strongconnect(next, index, stack, visited, components, nextNodes, include);
                v.update(w.lowlink());
            } else if(stack.contains(w)) {
                v.update(w.index());
            }
        }
        if(v.isRoot()) {
            final Set.Transient<E> component = Set.Transient.of();
            Node<E> w;
            do {
                w = stack.pop();
                component.__insert(w.elem());
            } while(!w.elem().equals(v.elem()));
            components.add(component.freeze());
        }
        return v;
    }

    private static class Node<E> {

        private final E elem;
        private final int index;
        private int lowlink;

        public Node(E elem, int index) {
            this.elem = elem;
            this.index = index;
            this.lowlink = index;
        }

        public E elem() {
            return elem;
        }

        public int index() {
            return index;
        }

        public int lowlink() {
            return lowlink;
        }

        public int update(int lowlink) {
            return(this.lowlink = Math.min(this.lowlink, lowlink));
        }

        public boolean isRoot() {
            return this.lowlink == this.index;
        }

        @Override public String toString() {
            return elem.toString() + "(" + index + ", " + lowlink + ")";
        }

    }

    public static class TopoSortedComponents<E> implements Iterable<Set.Immutable<E>> {

        private final ImmutableList<Set.Immutable<E>> components;
        private final Map<E, Set.Immutable<E>> index;

        private TopoSortedComponents(List<Set.Immutable<E>> components) {
            this(ImmutableList.copyOf(components), index(components));
        }

        private static <E> Map<E, Set.Immutable<E>> index(List<Set.Immutable<E>> components) {
            ImmutableMap.Builder<E, Set.Immutable<E>> index = ImmutableMap.builder();
            for(Set.Immutable<E> component : components) {
                for(E elem : component) {
                    index.put(elem, component);
                }
            }
            return index.build();
        }

        private TopoSortedComponents(ImmutableList<Set.Immutable<E>> components, Map<E, Set.Immutable<E>> index) {
            this.components = components;
            this.index = index;
        }

        public java.util.Set<E> nodes() {
            return index.keySet();
        }

        @Override public Iterator<Immutable<E>> iterator() {
            return components.iterator();
        }

        public ImmutableList<Set.Immutable<E>> components() {
            return components;
        }

        public Set.Immutable<E> component(E elem) {
            return index.get(elem);
        }

        public TopoSortedComponents<E> reverse() {
            return new TopoSortedComponents<>(components.reverse(), index);
        }

    }

}