package mb.nabl2.terms.matching;

import static mb.nabl2.terms.build.TermBuild.B;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.google.common.collect.Ordering;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;

@SuppressWarnings("unused")
public class TermPatternOrderTest {

    private final ITermVar a = B.newVar("", "a");
    private final ITermVar b = B.newVar("", "b");
    private final ITermVar c = B.newVar("", "c");

    private final String f = "f";
    private final String g = "g";
    private final String h = "h";

    private final ITerm x = B.newString("x");
    private final ITerm y = B.newString("y");
    private final ITerm z = B.newString("z");

    @Test public void testEmptyPatternEqual() {
        TermPattern p1 = new TermPattern();
        TermPattern p2 = new TermPattern();
        assertEquals(0, TermPattern.leftRightOrdering.compare(p1, p2));
    }

    @Test public void testEqualStrings() {
        TermPattern p1 = new TermPattern(y);
        TermPattern p2 = new TermPattern(y);
        assertEquals(0, TermPattern.leftRightOrdering.compare(p1, p2));
    }

    @Test public void testInequalStrings() {
        TermPattern p1 = new TermPattern(x);
        TermPattern p2 = new TermPattern(y);
        assertNotEquals(0, TermPattern.leftRightOrdering.compare(p1, p2));
    }

    @Test public void testInequalAppls() {
        TermPattern p1 = new TermPattern(B.newAppl(f, x, y));
        TermPattern p2 = new TermPattern(B.newAppl(g, a, b, c));
        assertNotEquals(0, TermPattern.leftRightOrdering.compare(p1, p2));
    }

    @Test public void testSmallerLonger() {
        TermPattern p1 = new TermPattern(a);
        TermPattern p2 = new TermPattern(b, c);
        assertNotEquals(0, TermPattern.leftRightOrdering.compare(p1, p2));
    }

    @Test public void testLongerGreaterThanSmaller() {
        TermPattern p1 = new TermPattern(a, b);
        TermPattern p2 = new TermPattern(a);
        assertNotEquals(0, TermPattern.leftRightOrdering.compare(p1, p2));
    }

    @Test public void testVarGreaterThanString() {
        TermPattern p1 = new TermPattern(a);
        TermPattern p2 = new TermPattern(x);
        assertTrue(TermPattern.leftRightOrdering.compare(p1, p2) > 0);
        assertTrue(TermPattern.leftRightOrdering.compare(p2, p1) < 0);
    }

    @Test public void testVarGreaterThanTuple() {
        TermPattern p1 = new TermPattern(a);
        TermPattern p2 = new TermPattern(B.newTuple());
        assertTrue(TermPattern.leftRightOrdering.compare(p1, p2) > 0);
        assertTrue(TermPattern.leftRightOrdering.compare(p2, p1) < 0);
    }

    @Test public void testEmptyTupleGreaterThanProduct() {
        TermPattern p1 = new TermPattern(B.newTuple(x, a));
        TermPattern p2 = new TermPattern(B.newTuple());
        assertTrue(TermPattern.leftRightOrdering.compare(p1, p2) > 0);
        assertTrue(TermPattern.leftRightOrdering.compare(p2, p1) < 0);
    }

    @Test public void testFirstLevelDifference() {
        TermPattern p1 = new TermPattern(a);
        TermPattern p2 = new TermPattern(B.newAppl(f, b));
        List<TermPattern> ps = Arrays.asList(p1, p2);
        List<TermPattern> sps = Ordering.from(TermPattern.leftRightOrdering).immutableSortedCopy(ps);
        assertArrayEquals(new TermPattern[] { p2, p1 }, sps.toArray());
    }

    @Test public void testSecondLevelDifference() {
        TermPattern p1 = new TermPattern(B.newAppl(f, B.EMPTY_LIST));
        TermPattern p2 = new TermPattern(B.newAppl(f, b));
        List<TermPattern> ps = Arrays.asList(p1, p2);
        List<TermPattern> sps = Ordering.from(TermPattern.leftRightOrdering).immutableSortedCopy(ps);
        assertArrayEquals(new TermPattern[] { p1, p2 }, sps.toArray());
    }

    @Test public void testCommonPattern() {
        TermPattern p1 = new TermPattern(x, y);
        TermPattern p2 = new TermPattern(x, b);
        TermPattern p3 = new TermPattern(a, y);
        TermPattern p4 = new TermPattern(a, b);
        List<TermPattern> ps = Arrays.asList(p2, p4, p3, p1);
        List<TermPattern> sps = Ordering.from(TermPattern.leftRightOrdering).immutableSortedCopy(ps);
        assertArrayEquals(new TermPattern[] { p1, p2, p3, p4 }, sps.toArray());
    }

}