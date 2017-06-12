package org.metaborg.meta.nabl2.solver;

import java.io.Serializable;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.metaborg.meta.nabl2.util.collections.HashTrieRelation2;
import org.metaborg.meta.nabl2.util.collections.IRelation2;
import org.metaborg.meta.nabl2.util.collections.IndexedDeque;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import io.usethesource.capsule.Set;

public abstract class Dependencies<E> {

    private final IRelation2<E, E> dependencies;

    public Dependencies(IRelation2<E, E> dependencies) {
        this.dependencies = dependencies;
    }


    public Set.Immutable<E> getDirectDependencies(E node) {
        return dependencies.get(node);
    }

    public Set.Immutable<E> getAllDependencies(E node) {
        final Set.Transient<E> deps = Set.Transient.of();
        allDependencies(node, deps);
        return deps.freeze();
    }

    private void allDependencies(E current, Set.Transient<E> deps) {
        if(!deps.contains(current)) {
            dependencies.get(current).stream().forEach(next -> {
                deps.__insert(next);
                allDependencies(next, deps);
            });
        }
    }


    public Set.Immutable<E> getDirectDependents(E node) {
        return dependencies.inverse().get(node);
    }

    public Set.Immutable<E> getAllDependents(E node) {
        final Set.Transient<E> deps = Set.Transient.of();
        allDependents(node, deps);
        return deps.freeze();
    }

    private void allDependents(E current, Set.Transient<E> deps) {
        if(!deps.contains(current)) {
            dependencies.inverse().get(current).stream().forEach(next -> {
                deps.__insert(next);
                allDependencies(next, deps);
            });
        }
    }


    public List<Set.Immutable<E>> getTopoSortedComponents() {
        final ImmutableList.Builder<Set.Immutable<E>> components = ImmutableList.builder();
        final AtomicInteger index = new AtomicInteger(0);
        final Deque<Node> stack = new IndexedDeque<>();
        final Map<E, Node> visited = Maps.newHashMap();
        for(E current : Sets.union(dependencies.keySet(), dependencies.valueSet())) {
            if(!visited.containsKey(current)) {
                strongconnect(current, index, stack, visited, components);
            }
        }
        return components.build().reverse();
    }

    private Node strongconnect(E current, AtomicInteger index, Deque<Node> stack, Map<E, Node> visited,
            ImmutableList.Builder<Set.Immutable<E>> components) {
        Node v;
        visited.put(current, v = new Node(current, index.getAndIncrement()));
        stack.push(v);
        for(E next : dependencies.get(current)) {
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
        return "Dependencies" + dependencies.toString();
    }


    public static class Immutable<E> extends Dependencies<E> implements Serializable {
        private static final long serialVersionUID = 42L;

        private final IRelation2.Immutable<E, E> dependencies;

        public Immutable(IRelation2.Immutable<E, E> dependencies) {
            super(dependencies);
            this.dependencies = dependencies;
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
            super(dependencies);
            this.dependencies = dependencies;
        }

        public boolean add(E from, E to) {
            return dependencies.put(from, to);
        }

        public boolean remove(E node) {
            boolean change = false;
            change |= !dependencies.removeKey(node).isEmpty();
            change |= !dependencies.removeValue(node).isEmpty();
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