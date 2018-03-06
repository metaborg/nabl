package mb.nabl2.solver;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import io.usethesource.capsule.Set;
import mb.nabl2.util.collections.HashTrieRelation2;
import mb.nabl2.util.collections.IRelation2;
import mb.nabl2.util.collections.IndexedDeque;

public abstract class Dependencies<E> {

    protected Dependencies() {
    }

    protected abstract IRelation2<E, E> dependencies();

    public abstract Dependencies<E> inverse();


    public boolean contains(E node) {
        return dependencies().containsKey(node) || dependencies().containsValue(node);
    }

    public boolean contains(E from, E to) {
        return dependencies().containsEntry(from, to);
    }

    public java.util.Set<E> nodeSet() {
        return Sets.union(dependencies().keySet(), dependencies().valueSet());
    }


    public java.util.Set<E> getDirectDependencies(E node) {
        return dependencies().get(node);
    }

    public Set.Immutable<E> getAllDependencies(@SuppressWarnings("unchecked") E... nodes) {
        return getAllDependencies(Arrays.asList(nodes));
    }

    public Set.Immutable<E> getAllDependencies(Iterable<? extends E> nodes) {
        final Set.Transient<E> deps = Set.Transient.of();
        for(E node : nodes) {
            allDependencies(node, deps);
        }
        for(E node : nodes) {
            deps.__remove(node);
        }
        return deps.freeze();
    }

    private void allDependencies(E current, Set.Transient<E> deps) {
        if(!deps.contains(current)) {
            deps.__insert(current);
            dependencies().get(current).stream().forEach(next -> {
                allDependencies(next, deps);
            });
        }
    }


    public java.util.Set<E> getDirectDependents(E node) {
        return dependencies().inverse().get(node);
    }

    public Set.Immutable<E> getAllDependents(@SuppressWarnings("unchecked") E... nodes) {
        return getAllDependents(Arrays.asList(nodes));
    }

    public Set.Immutable<E> getAllDependents(Iterable<? extends E> nodes) {
        final Set.Transient<E> deps = Set.Transient.of();
        for(E node : nodes) {
            allDependents(node, deps);
        }
        for(E node : nodes) {
            deps.__remove(node);
        }
        return deps.freeze();
    }

    private void allDependents(E current, Set.Transient<E> deps) {
        if(!deps.contains(current)) {
            deps.__insert(current);
            dependencies().inverse().get(current).stream().forEach(next -> {
                allDependents(next, deps);
            });
        }
    }


    public TopoSortedComponents<E> getTopoSortedComponents() {
        return getTopoSortedComponents(nodeSet());
    }

    public TopoSortedComponents<E> getTopoSortedComponents(Iterable<? extends E> nodes) {
        final ImmutableList.Builder<Set.Immutable<E>> components = ImmutableList.builder();
        final AtomicInteger index = new AtomicInteger(0);
        final Deque<Node> stack = new IndexedDeque<>();
        final Map<E, Node> visited = Maps.newHashMap();
        for(E current : nodes) {
            if(!visited.containsKey(current)) {
                strongconnect(current, index, stack, visited, components);
            }
        }
        return new TopoSortedComponents<>(components.build());
    }

    public static class TopoSortedComponents<E> {

        private final ImmutableList<Set.Immutable<E>> components;
        private final Map<E, Set.Immutable<E>> index;

        private TopoSortedComponents(List<Set.Immutable<E>> components) {
            this.components = ImmutableList.copyOf(components);
            this.index = Maps.newHashMap();
            for(Set.Immutable<E> component : components) {
                for(E elem : component) {
                    index.put(elem, component);
                }
            }
        }

        public java.util.Set<E> nodes() {
            return index.keySet();
        }

        public ImmutableList<Set.Immutable<E>> components() {
            return components;
        }

        public Set.Immutable<E> component(E elem) {
            return index.get(elem);
        }

    }

    private Node strongconnect(E current, AtomicInteger index, Deque<Node> stack, Map<E, Node> visited,
            ImmutableList.Builder<Set.Immutable<E>> components) {
        Node v;
        visited.put(current, v = new Node(current, index.getAndIncrement()));
        stack.push(v);
        for(E next : dependencies().get(current)) {
            Node w = visited.get(next);
            if(w == null) {
                w = strongconnect(next, index, stack, visited, components);
                v.update(w.lowlink());
            } else if(stack.contains(w)) {
                v.update(w.index());
            }
        }
        if(v.isRoot()) {
            final Set.Transient<E> component = Set.Transient.of();
            Node w;
            do {
                w = stack.pop();
                component.__insert(w.elem());
            } while(!w.elem().equals(v.elem()));
            components.add(component.freeze());
        }
        return v;
    }

    private class Node {

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


    @Override public String toString() {
        return "Dependencies" + dependencies().toString();
    }


    public static class Immutable<E> extends Dependencies<E> implements Serializable {
        private static final long serialVersionUID = 42L;

        private final IRelation2.Immutable<E, E> dependencies;

        public Immutable(IRelation2.Immutable<E, E> dependencies) {
            this.dependencies = dependencies;
        }

        @Override protected IRelation2<E, E> dependencies() {
            return dependencies;
        }

        @Override public Dependencies.Immutable<E> inverse() {
            return new Immutable<>(dependencies.inverse());
        }

        public Dependencies.Transient<E> melt() {
            return new Dependencies.Transient<>(dependencies.melt());
        }

        public static <E> Dependencies.Immutable<E> of() {
            return new Immutable<>(HashTrieRelation2.Immutable.of());
        }

    }

    public static class Transient<E> extends Dependencies<E> {

        private final IRelation2.Transient<E, E> dependencies;

        public Transient(IRelation2.Transient<E, E> dependencies) {
            this.dependencies = dependencies;
        }

        @Override protected IRelation2<E, E> dependencies() {
            return dependencies;
        }

        @Override public Dependencies.Transient<E> inverse() {
            return new Transient<>(dependencies.inverse());
        }

        public int size() {
            return dependencies.size();
        }

        public boolean add(E from, E to) {
            return dependencies.put(from, to);
        }

        public boolean addAll(E from, Iterable<? extends E> to) {
            return dependencies.putAll(from, to);
        }

        public boolean addAll(Dependencies<E> other) {
            return dependencies.putAll(other.dependencies());
        }

        public boolean remove(E node) {
            boolean change = false;
            change |= !dependencies.removeKey(node).isEmpty();
            change |= !dependencies.removeValue(node).isEmpty();
            return change;
        }

        public boolean removeAll(Iterable<? extends E> nodes) {
            boolean change = false;
            for(E node : nodes) {
                change |= remove(node);
            }
            return change;
        }

        public boolean remove(E from, E to) {
            return dependencies.removeEntry(from, to);
        }

        public Dependencies.Immutable<E> freeze() {
            return new Dependencies.Immutable<>(dependencies.freeze());
        }

        public static <E> Dependencies.Transient<E> of() {
            return new Transient<>(HashTrieRelation2.Transient.of());
        }

    }

}