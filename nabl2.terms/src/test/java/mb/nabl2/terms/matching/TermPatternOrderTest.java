package mb.nabl2.terms.matching;

import static mb.nabl2.terms.matching.TermPattern.P;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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
        assertEquivalentPatterns(y, y);
    }

    @Test public void testInequalStrings() {
        assertIncomparablePatterns(x, y);
    }

    @Test public void testInequalAppls() {
        Pattern p1 = P.newAppl(f, x, y);
        Pattern p2 = P.newAppl(g, a, b, c);
        assertIncomparablePatterns(p1, p2);
    }

    @Test public void testTuplesOfDifferentLengthAreIncomparable() {
        Pattern p1 = P.newTuple();
        Pattern p2 = P.newTuple(b, c);
        assertIncomparablePatterns(p1, p2);
    }

    @Test public void testVarGreaterThanString() {
        Pattern p1 = a;
        Pattern p2 = x;
        assertFirstPatternMoreGeneralThanSecond(p1, p2);
    }

    @Test public void testVarGreaterThanTuple() {
        Pattern p1 = a;
        Pattern p2 = P.newTuple();
        assertFirstPatternMoreGeneralThanSecond(p1, p2);
    }

    @Test public void testEmptyTupleAndProductUnordered() {
        Pattern p1 = P.newTuple(x, a);
        Pattern p2 = P.newTuple();
        assertIncomparablePatterns(p1, p2);
    }

    @Test public void testFirstLevelDifference() {
        Pattern p1 = a;
        Pattern p2 = P.newAppl(f, b);
        List<Pattern> ps = Arrays.asList(p1, p2);
        List<Pattern> sps = Ordering.from(Pattern.leftRightOrdering.asComparator()).immutableSortedCopy(ps);
        assertArrayEquals(new Pattern[] { p2, p1 }, sps.toArray());
    }

    @Test public void testSecondLevelDifference() {
        Pattern p1 = P.newAppl(f, P.newNil());
        Pattern p2 = P.newAppl(f, b);
        List<Pattern> ps = Arrays.asList(p1, p2);
        List<Pattern> sps = Ordering.from(Pattern.leftRightOrdering.asComparator()).immutableSortedCopy(ps);
        assertArrayEquals(new Pattern[] { p1, p2 }, sps.toArray());
    }

    @Test public void testCommonPattern() {
        Pattern p1 = P.newTuple(x, y);
        Pattern p2 = P.newTuple(x, b);
        Pattern p3 = P.newTuple(a, y);
        Pattern p4 = P.newTuple(a, b);
        List<Pattern> ps = Arrays.asList(p2, p4, p3, p1);
        List<Pattern> sps = Ordering.from(Pattern.leftRightOrdering.asComparator()).immutableSortedCopy(ps);
        assertArrayEquals(new Pattern[] { p1, p2, p3, p4 }, sps.toArray());
    }

    @Test public void testSameNonlinear() {
        Pattern p1 = P.newTuple(a, a);
        Pattern p2 = P.newTuple(b, b);
        assertEquivalentPatterns(p1, p2);
    }

    @Test public void testNonlinearAndLinear() {
        Pattern p1 = P.newTuple(a, a);
        Pattern p2 = P.newTuple(b, c);
        assertFirstPatternMoreSpecificThanSecond(p1, p2);
    }

    @Test public void testNonlinearAs() {
        Pattern p1 = P.newTuple(P.newAs("x", x), P.newVar("x"));
        Pattern p2 = P.newTuple(P.newVar("x"), P.newVar("x"));
        assertFirstPatternMoreSpecificThanSecond(p1, p2);
    }

    @Test public void testNonLinearString() {
        Pattern p1 = P.newTuple(P.newVar("x"), P.newVar("x"));
        Pattern p2 = P.newTuple(P.newVar("x"), x);
        assertFirstPatternMoreGeneralThanSecond(p1, p2);
    }

    @Test public void testNonLinearAsString() {
        Pattern p1 = P.newTuple(P.newVar("x"), P.newVar("x"));
        Pattern p2 = P.newTuple(P.newVar("x"), P.newAs("y", x));
        assertFirstPatternMoreGeneralThanSecond(p1, p2);
    }

    //----------------------------------------------------------------------

    private static void assertEquivalentPatterns(Pattern p1, Pattern p2) {
        final Optional<Integer> c1 = Pattern.leftRightOrdering.compare(p1, p2);
        final Optional<Integer> c2 = Pattern.leftRightOrdering.compare(p2, p1);
        assertTrue("Patterns not equal", c1.map(c -> c == 0).orElse(false));
        assertTrue("Assymetric order", c2.map(c -> c == 0).orElse(false));
    }

    private static void assertFirstPatternMoreSpecificThanSecond(Pattern p1, Pattern p2) {
        final Optional<Integer> c1 = Pattern.leftRightOrdering.compare(p1, p2);
        final Optional<Integer> c2 = Pattern.leftRightOrdering.compare(p2, p1);
        assertTrue("Pattern not smaller", c1.map(c -> c < 0).orElse(false));
        assertTrue("Asymmetric order", c2.map(c -> c > 0).orElse(false));
    }

    private static void assertFirstPatternMoreGeneralThanSecond(Pattern p1, Pattern p2) {
        final Optional<Integer> c1 = Pattern.leftRightOrdering.compare(p1, p2);
        final Optional<Integer> c2 = Pattern.leftRightOrdering.compare(p2, p1);
        assertTrue("Pattern not larger", c1.map(c -> c > 0).orElse(false));
        assertTrue("Asymmetric order", c2.map(c -> c < 0).orElse(false));
    }

    private static void assertIncomparablePatterns(Pattern p1, Pattern p2) {
        final Optional<Integer> c1 = Pattern.leftRightOrdering.compare(p1, p2);
        final Optional<Integer> c2 = Pattern.leftRightOrdering.compare(p2, p1);
        assertFalse("Patterns not incomparable", c1.isPresent());
        assertFalse("Assymetric order", c2.isPresent());
    }

}
