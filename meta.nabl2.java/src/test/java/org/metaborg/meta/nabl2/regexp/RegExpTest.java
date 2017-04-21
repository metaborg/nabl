package org.metaborg.meta.nabl2.regexp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.metaborg.meta.nabl2.regexp.impl.FiniteAlphabet;
import org.metaborg.meta.nabl2.regexp.impl.RegExpBuilder;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

public class RegExpTest {

    private static IAlphabet<Integer> alphabet;
    private static IRegExpBuilder<Integer> b;

    @BeforeClass public static void beforeClass() {
        alphabet = new FiniteAlphabet<>(ImmutableSet.of(1, 3, 5, 7, 11));
        b = new RegExpBuilder<>(alphabet);
    }

    @Test public void testConstruct() {
        IRegExp<Integer> re;
        re = b.emptySet();
        re = b.emptyString();
        re = b.complement(re);
        re = b.closure(re);
    }

    @Test public void testCanonicalAnd() {
        IRegExp<Integer> re;
        IRegExp<Integer> canonical;

        re = b.and(b.symbol(1), b.symbol(1));
        canonical = b.symbol(1);
        assertEquals(canonical, re);

        re = b.and(b.symbol(3), b.symbol(1));
        canonical = b.and(b.symbol(1), b.symbol(3));
        assertEquals(canonical, re);

        re = b.and(b.and(b.symbol(1), b.symbol(3)), b.symbol(5));
        canonical = b.and(b.symbol(1), b.and(b.symbol(3), b.symbol(5)));
        assertEquals(canonical, re);

        re = b.and(b.and(b.symbol(1), b.symbol(3)), b.and(b.symbol(5), b.symbol(7)));
        canonical = b.and(b.symbol(1), b.and(b.symbol(3), b.and(b.symbol(5), b.symbol(7))));
        assertEquals(canonical, re);

        re = b.and(b.symbol(1), b.emptySet());
        canonical = b.emptySet();
        assertEquals(canonical, re);

        re = b.and(b.symbol(1), b.complement(b.emptySet()));
        canonical = b.symbol(1);
        assertEquals(canonical, re);

        re = b.and(b.and(b.and(b.symbol(7), b.symbol(5)), b.symbol(3)), b.symbol(1));
        canonical = b.and(b.symbol(1), b.and(b.symbol(3), b.and(b.symbol(5), b.symbol(7))));
        assertEquals(canonical, re);
    }

    @Test public void testCanonicalOr() {
        IRegExp<Integer> re;
        IRegExp<Integer> canonical;

        re = b.or(b.symbol(1), b.symbol(1));
        canonical = b.symbol(1);
        assertEquals(canonical, re);

        re = b.or(b.symbol(3), b.symbol(1));
        canonical = b.or(b.symbol(1), b.symbol(3));
        assertEquals(canonical, re);

        re = b.or(b.or(b.symbol(1), b.symbol(3)), b.symbol(5));
        canonical = b.or(b.symbol(1), b.or(b.symbol(3), b.symbol(5)));
        assertEquals(canonical, re);

        re = b.or(b.or(b.symbol(1), b.symbol(3)), b.or(b.symbol(5), b.symbol(7)));
        canonical = b.or(b.symbol(1), b.or(b.symbol(3), b.or(b.symbol(5), b.symbol(7))));
        assertEquals(canonical, re);

        re = b.or(b.symbol(1), b.emptySet());
        canonical = b.symbol(1);
        assertEquals(canonical, re);

        re = b.or(b.symbol(1), b.complement(b.emptySet()));
        canonical = b.complement(b.emptySet());
        assertEquals(canonical, re);

        re = b.or(b.or(b.or(b.symbol(7), b.symbol(5)), b.symbol(3)), b.symbol(1));
        canonical = b.or(b.symbol(1), b.or(b.symbol(3), b.or(b.symbol(5), b.symbol(7))));
        assertEquals(canonical, re);
    }

    @Test public void testCanonicalAndOr() {
        IRegExp<Integer> re;
        IRegExp<Integer> canonical;

        re = b.and(b.and(b.symbol(3), b.symbol(1)), b.or(b.symbol(7), b.symbol(5)));
        canonical = b.and(b.or(b.symbol(5), b.symbol(7)), b.and(b.symbol(1), b.symbol(3)));
        assertEquals(canonical, re);

        re = b.or(b.and(b.symbol(3), b.symbol(1)), b.or(b.symbol(7), b.symbol(5)));
        canonical = b.or(b.and(b.symbol(1), b.symbol(3)), b.or(b.symbol(5), b.symbol(7)));
        assertEquals(canonical, re);
    }

    @Test public void testSymbolMatching() {
        IRegExp<Integer> re = b.symbol(1);
        IRegExpMatcher<Integer> m = RegExpMatcher.create(re);
        assertFalse(m.isFinal());
        m = m.match(1);
        assertTrue(m.isAccepting());
        assertTrue(m.isFinal());
        m = m.match(3);
        assertFalse(m.isAccepting());
    }

    @Test public void testClosureMatching() {
        IRegExp<Integer> re = b.closure(b.symbol(1));
        IRegExpMatcher<Integer> m = RegExpMatcher.create(re);
        m = m.match(1);
        assertTrue(m.isAccepting());
        m = m.match(Lists.newArrayList(1, 1, 1, 1, 1));
        assertTrue(m.isAccepting());
        m = m.match(3);
        assertFalse(m.isAccepting());
    }

    @Test public void testEmptySetMatching() {
        IRegExp<Integer> re = b.emptySet();
        IRegExpMatcher<Integer> m = RegExpMatcher.create(re);
        assertFalse(m.isAccepting());
    }

    @Test public void testEmptyStringMatching() {
        IRegExp<Integer> re = b.emptyString();
        IRegExpMatcher<Integer> m = RegExpMatcher.create(re);
        assertTrue(m.isAccepting());
        m = m.match(3);
        assertFalse(m.isAccepting());
    }

    @Test public void testConcatMatching() {
        IRegExp<Integer> re = b.concat(b.symbol(1), b.symbol(3));
        IRegExpMatcher<Integer> m = RegExpMatcher.create(re);
        m = m.match(1);
        assertFalse(m.isAccepting());
        m = m.match(3);
        assertTrue(m.isAccepting());
    }

    @Test public void testComplementMatching() {
        IRegExp<Integer> re = b.complement(b.symbol(1));
        IRegExpMatcher<Integer> m = RegExpMatcher.create(re);
        assertTrue(m.isAccepting());
        m = m.match(1);
        assertFalse(m.isAccepting());
        m = m.match(3);
        assertTrue(m.isAccepting());
    }

    @Test public void testAndMatching() {
        IRegExp<Integer> re = b.and(b.symbol(1), b.closure(b.symbol(1)));
        IRegExpMatcher<Integer> m = RegExpMatcher.create(re);
        assertFalse(m.isAccepting());
        m = m.match(1);
        assertTrue(m.isAccepting());
        m = m.match(1);
        assertFalse(m.isAccepting());
    }

    @Test public void testOrMatching() {
        IRegExp<Integer> re = b.closure(b.or(b.symbol(1), b.symbol(3)));
        IRegExpMatcher<Integer> m = RegExpMatcher.create(re);
        assertTrue(m.isAccepting());
        m = m.match(1);
        assertTrue(m.isAccepting());
        m = m.match(3);
        assertTrue(m.isAccepting());
        m = m.match(1);
        assertTrue(m.isAccepting());
        m = m.match(7);
        assertFalse(m.isAccepting());
    }

}