package org.metaborg.meta.nabl2.solver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import io.usethesource.capsule.Set.Immutable;

public class DependenciesTest {

    @Test public void testEmpty() {
        Dependencies.Transient<Integer> deps = Dependencies.Transient.of();
        assertTrue(deps.getTopoSortedComponents().isEmpty());
        assertTrue(deps.getDirectDependencies(1).isEmpty());
        assertTrue(deps.getAllDependencies(1).isEmpty());
        assertTrue(deps.getDirectDependents(1).isEmpty());
        assertTrue(deps.getAllDependents(1).isEmpty());
    }

    @Test public void testOneSCC() {
        Dependencies.Transient<Integer> deps = Dependencies.Transient.of();
        deps.add(1, 2);
        deps.add(2, 3);
        deps.add(3, 1);
        List<Immutable<Integer>> components = deps.getTopoSortedComponents();
        assertEquals(1, components.size());
    }

    @Test public void testTwoSCC() {
        Dependencies.Transient<Integer> deps = Dependencies.Transient.of();
        deps.add(1, 2);
        deps.add(2, 3);
        deps.add(3, 1);
        deps.add(2, 4);
        List<Immutable<Integer>> components = deps.getTopoSortedComponents();
        assertEquals(2, components.size());
        assertEquals(3, components.get(0).size());
        assertEquals(1, components.get(1).size());
    }

    @Test public void testThreeSCC() {
        Dependencies.Transient<Integer> deps = Dependencies.Transient.of();
        deps.add(1, 2);
        deps.add(0, 1);
        deps.add(2, 3);
        deps.add(3, 1);
        deps.add(2, 4);
        List<Immutable<Integer>> components = deps.getTopoSortedComponents();
        assertEquals(3, components.size());
        assertEquals(1, components.get(0).size());
        assertEquals(3, components.get(1).size());
        assertEquals(1, components.get(2).size());
    }
}