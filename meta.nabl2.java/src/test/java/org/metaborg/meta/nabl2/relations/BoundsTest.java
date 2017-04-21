package org.metaborg.meta.nabl2.relations;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.metaborg.meta.nabl2.relations.terms.Relation;

public class BoundsTest {

    @Test public void testLub1() throws RelationException {
        Relation<Integer> r = new Relation<>(RelationDescription.PARTIAL_ORDER);
        int lub = r.leastUpperBound(1, 1).orElse(Integer.MIN_VALUE);
        assertEquals(1, lub);
    }

    @Test public void testLub2() throws RelationException {
        Relation<Integer> r = new Relation<>(RelationDescription.PARTIAL_ORDER);
        r.add(1, 2);
        int lub = r.leastUpperBound(1, 2).orElse(Integer.MIN_VALUE);
        assertEquals(2, lub);
    }

    @Test public void testLub3() throws RelationException {
        Relation<Integer> r = new Relation<>(RelationDescription.PARTIAL_ORDER);
        r.add(1, 3);
        r.add(2, 3);
        int lub = r.leastUpperBound(3, 2).orElse(Integer.MIN_VALUE);
        assertEquals(3, lub);
    }

    @Test public void testLub4() throws RelationException {
        Relation<Integer> r = new Relation<>(RelationDescription.PARTIAL_ORDER);
        r.add(1, 2);
        r.add(3, 4);
        int lub = r.leastUpperBound(1, 3).orElse(Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, lub);
    }

    @Test public void testLub5() throws RelationException {
        Relation<Integer> r = new Relation<>(RelationDescription.PARTIAL_ORDER);
        r.add(3, 1);
        r.add(5, 3);
        r.add(2, 1);
        r.add(4, 2);
        int lub = r.leastUpperBound(4, 5).orElse(Integer.MIN_VALUE);
        assertEquals(1, lub);
    }

    @Test public void testLub6() throws RelationException {
        Relation<Integer> r = new Relation<>(RelationDescription.PARTIAL_ORDER);
        r.add(3, 1);
        r.add(5, 3);
        r.add(2, 1);
        r.add(4, 2);
        int lub = r.leastUpperBound(3, 5).orElse(Integer.MIN_VALUE);
        assertEquals(3, lub);
    }

    @Test public void testLub7() throws RelationException {
        Relation<Integer> r = new Relation<>(RelationDescription.PARTIAL_ORDER);
        r.add(3, 1);
        r.add(3, 2);
        r.add(4, 1);
        r.add(4, 2);
        int lub = r.leastUpperBound(3, 4).orElse(Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, lub);
    }

}