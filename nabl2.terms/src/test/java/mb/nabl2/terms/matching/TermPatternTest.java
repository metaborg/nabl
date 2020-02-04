package mb.nabl2.terms.matching;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermPattern.P;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.ISubstitution;

@SuppressWarnings("unused")
public class TermPatternTest {

    private final ITermVar a = B.newVar("", "a");
    private final ITermVar b = B.newVar("", "b");
    private final ITermVar c = B.newVar("", "c");

    private final String f = "f";
    private final String g = "g";
    private final String h = "h";

    private final ITerm x = B.newString("x");
    private final ITerm y = B.newString("y");
    private final ITerm z = B.newString("z");

    @Test public void testMatchVars() {
        final Pattern pattern = P.newVar(a);
        final ISubstitution.Immutable result = pattern.match(b).get();
        assertEquals(b, result.apply(a));
    }

    @Test public void testMatchVarTerm() {
        final Pattern pattern = P.newVar(a);
        final ISubstitution.Immutable result = pattern.match(B.newAppl(g, x, b)).get();
        assertEquals(B.newAppl(g, x, b), result.apply(a));
    }

    @Test public void testMatchTerms() {
        final Pattern pattern = P.newAppl(g, P.newVar(a), P.newVar(b));
        final ISubstitution.Immutable result = pattern.matchOrThrow(B.newAppl(g, B.newList(x), y)).get();
        assertEquals(B.newList(x), result.apply(a));
        assertEquals(y, result.apply(b));
    }

    @Test public void testMatchFail() {
        final Pattern pattern = P.newAppl(g, P.newVar(a));
        assertFalse(pattern.match(b).isPresent());
    }

    @Test public void testMatchStrings() {
        assertTrue(P.newString(f).match(B.newString(f)).isPresent());
    }

    @Test public void testMismatchStrings() {
        assertFalse(P.newString(f).match(B.newString(g)).isPresent());
    }

    @Test public void testMatchInts() {
        assertTrue(P.newInt(42).match(B.newInt(42)).isPresent());
    }

    @Test public void testMismatchInts() {
        assertFalse(P.newInt(42).match(B.newInt(1)).isPresent());
    }

    @Test public void testMatchNils() {
        assertTrue(P.newNil().matchOrThrow(B.newNil()).isPresent());
    }

    @Test public void testMismatchNilAndList() {
        assertFalse(P.newNil().matchOrThrow(B.newList(x)).isPresent());
    }

    @Test public void testMatchGroundAppls() {
        Pattern pattern = P.newAppl(f, P.newNil());
        assertTrue(pattern.matchOrThrow(B.newAppl(f, B.newNil())).isPresent());
    }

    @Test public void testMatchWildcardWithNil() {
        Pattern pattern = P.newAppl(f, P.newWld());
        assertTrue(pattern.matchOrThrow(B.newAppl(f, B.newNil())).isPresent());
    }

}