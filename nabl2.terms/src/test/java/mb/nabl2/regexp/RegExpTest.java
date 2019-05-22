package mb.nabl2.regexp;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Lists;

import mb.nabl2.regexp.impl.RegExpBuilder;

public class RegExpTest {

    private static IRegExpBuilder<Integer> b;

    @BeforeClass public static void beforeClass() {
        b = new RegExpBuilder<>();
    }

    @Test public void testConstruct() {
        IRegExp<Integer> re;
        re = b.emptySet();
        re = b.emptyString();
        re = b.complement(re);
        re = b.closure(re);
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
        assertFalse(m.isFinal());
        assertFalse(m.isAccepting());
        m = m.match(3);
        assertTrue(m.isFinal());
        assertTrue(m.isAccepting());
    }

    @Test public void testNestedConcatMatching() {
        IRegExp<Integer> re = b.concat(b.concat(b.symbol(1), b.symbol(3)), b.symbol(5));
        IRegExpMatcher<Integer> m = RegExpMatcher.create(re);
        m = m.match(1);
        assertFalse(m.isFinal());
        assertFalse(m.isAccepting());
        m = m.match(3);
        assertFalse(m.isFinal());
        assertFalse(m.isAccepting());
        m = m.match(5);
        assertTrue(m.isFinal());
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