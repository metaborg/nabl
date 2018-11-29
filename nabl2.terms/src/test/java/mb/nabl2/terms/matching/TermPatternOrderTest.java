package mb.nabl2.terms.matching;

import static mb.nabl2.terms.matching.TermPattern.P;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.google.common.collect.Ordering;

@SuppressWarnings("unused")
public class TermPatternOrderTest {

    private final Pattern a = P.newVar("a");
    private final Pattern b = P.newVar("b");
    private final Pattern c = P.newVar("c");

    private final String f = "f";
    private final String g = "g";
    private final String h = "h";

    private final Pattern x = P.newString("x");
    private final Pattern y = P.newString("y");
    private final Pattern z = P.newString("z");

    @Test public void testEqualStrings() {
        assertEqualPatterns(y, y);
    }

    @Test public void testInequalStrings() {
        assertInequalPatterns(x, y);
    }

    @Test public void testInequalAppls() {
        Pattern p1 = P.newAppl(f, x, y);
        Pattern p2 = P.newAppl(g, a, b, c);
        assertInequalPatterns(p1, p2);
    }

    @Test public void testSmallerLonger() {
        Pattern p1 = a;
        Pattern p2 = P.newTuple(b, c);
        assertInequalPatterns(p1, p2);
    }

    @Test public void testLongerGreaterThanSmaller() {
        Pattern p1 = P.newTuple(a, b);
        Pattern p2 = a;
        assertInequalPatterns(p1, p2);
    }

    @Test public void testVarGreaterThanString() {
        Pattern p1 = a;
        Pattern p2 = x;
        assertLargerPattern(p1, p2);
    }

    @Test public void testVarGreaterThanTuple() {
        Pattern p1 = a;
        Pattern p2 = P.newTuple();
        assertLargerPattern(p1, p2);
    }

    @Test public void testEmptyTupleGreaterThanProduct() {
        Pattern p1 = P.newTuple(x, a);
        Pattern p2 = P.newTuple();
        assertLargerPattern(p1, p2);
    }

    @Test public void testFirstLevelDifference() {
        Pattern p1 = a;
        Pattern p2 = P.newAppl(f, b);
        List<Pattern> ps = Arrays.asList(p1, p2);
        List<Pattern> sps = Ordering.from(Pattern.leftRightOrdering).immutableSortedCopy(ps);
        assertArrayEquals(new Pattern[] { p2, p1 }, sps.toArray());
    }

    @Test public void testSecondLevelDifference() {
        Pattern p1 = P.newAppl(f, P.EMPTY_LIST);
        Pattern p2 = P.newAppl(f, b);
        List<Pattern> ps = Arrays.asList(p1, p2);
        List<Pattern> sps = Ordering.from(Pattern.leftRightOrdering).immutableSortedCopy(ps);
        assertArrayEquals(new Pattern[] { p1, p2 }, sps.toArray());
    }

    @Test public void testCommonPattern() {
        Pattern p1 = P.newTuple(x, y);
        Pattern p2 = P.newTuple(x, b);
        Pattern p3 = P.newTuple(a, y);
        Pattern p4 = P.newTuple(a, b);
        List<Pattern> ps = Arrays.asList(p2, p4, p3, p1);
        List<Pattern> sps = Ordering.from(Pattern.leftRightOrdering).immutableSortedCopy(ps);
        assertArrayEquals(new Pattern[] { p1, p2, p3, p4 }, sps.toArray());
    }

    @Test public void testSameNonlinear() {
        Pattern p1 = P.newTuple(a, a);
        Pattern p2 = P.newTuple(b, b);
        assertEqualPatterns(p1, p2);
    }

    @Test public void testNonlinearAndLinear() {
        Pattern p1 = P.newTuple(a, a);
        Pattern p2 = P.newTuple(b, c);
        assertSmallerPattern(p1, p2);
    }

    @Test public void testNonlinearAs() {
        Pattern p1 = P.newTuple(P.newAs("x", x), P.newVar("x"));
        Pattern p2 = P.newTuple(P.newVar("x"), P.newVar("x"));
        assertSmallerPattern(p1, p2);
    }

    //----------------------------------------------------------------------

    private static void assertEqualPatterns(Pattern p1, Pattern p2) {
        final int c1 = Pattern.leftRightOrdering.compare(p1, p2);
        final int c2 = Pattern.leftRightOrdering.compare(p2, p1);
        assertTrue("Patterns not equal", c1 == 0);
        assertTrue("Assymetric order", c2 == 0);
    }

    private static void assertInequalPatterns(Pattern p1, Pattern p2) {
        final int c1 = Pattern.leftRightOrdering.compare(p1, p2);
        final int c2 = Pattern.leftRightOrdering.compare(p2, p1);
        assertTrue("Patterns not inequal", c1 != 0);
        assertTrue("Asymmetric order", c2 != 0);
    }

    private static void assertSmallerPattern(Pattern p1, Pattern p2) {
        final int c1 = Pattern.leftRightOrdering.compare(p1, p2);
        final int c2 = Pattern.leftRightOrdering.compare(p2, p1);
        assertTrue("Pattern not smaller", c1 < 0);
        assertTrue("Asymmetric order", c2 > 0);
    }

    private static void assertLargerPattern(Pattern p1, Pattern p2) {
        final int c1 = Pattern.leftRightOrdering.compare(p1, p2);
        final int c2 = Pattern.leftRightOrdering.compare(p2, p1);
        assertTrue("Pattern not larger", c1 > 0);
        assertTrue("Asymmetric order", c2 < 0);
    }
}