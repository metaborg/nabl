package mb.nabl2.terms.matching;

import static mb.nabl2.terms.matching.TermPattern.P;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
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
        assertEquals(0, Pattern.leftRightOrdering.compare(y, y));
    }

    @Test public void testInequalStrings() {
        assertNotEquals(0, Pattern.leftRightOrdering.compare(x, y));
    }

    @Test public void testInequalAppls() {
        Pattern p1 = P.newAppl(f, x, y);
        Pattern p2 = P.newAppl(g, a, b, c);
        assertNotEquals(0, Pattern.leftRightOrdering.compare(p1, p2));
    }

    @Test public void testSmallerLonger() {
        Pattern p1 = a;
        Pattern p2 = P.newTuple(b, c);
        assertNotEquals(0, Pattern.leftRightOrdering.compare(p1, p2));
    }

    @Test public void testLongerGreaterThanSmaller() {
        Pattern p1 = P.newTuple(a, b);
        Pattern p2 = a;
        assertNotEquals(0, Pattern.leftRightOrdering.compare(p1, p2));
    }

    @Test public void testVarGreaterThanString() {
        Pattern p1 = a;
        Pattern p2 = x;
        assertTrue(Pattern.leftRightOrdering.compare(p1, p2) > 0);
        assertTrue(Pattern.leftRightOrdering.compare(p2, p1) < 0);
    }

    @Test public void testVarGreaterThanTuple() {
        Pattern p1 = a;
        Pattern p2 = P.newTuple();
        assertTrue(Pattern.leftRightOrdering.compare(p1, p2) > 0);
        assertTrue(Pattern.leftRightOrdering.compare(p2, p1) < 0);
    }

    @Test public void testEmptyTupleGreaterThanProduct() {
        Pattern p1 = P.newTuple(x, a);
        Pattern p2 = P.newTuple();
        assertTrue(Pattern.leftRightOrdering.compare(p1, p2) > 0);
        assertTrue(Pattern.leftRightOrdering.compare(p2, p1) < 0);
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

}