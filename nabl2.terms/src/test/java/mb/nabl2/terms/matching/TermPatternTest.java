package mb.nabl2.terms.matching;

import static mb.nabl2.terms.build.TermBuild.B;
import static org.junit.Assert.assertEquals;

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

    @Test public void testMatchVars() throws MatchException {
        final IPattern pattern = new TermPattern(a);
        final ISubstitution.Immutable result = pattern.match(b);
        assertEquals(b, result.apply(a));
    }

    @Test public void testMatchVarTerm() throws MatchException {
        final IPattern pattern = new TermPattern(a);
        final ISubstitution.Immutable result = pattern.match(B.newAppl(g, x, b));
        assertEquals(B.newAppl(g, x, b), result.apply(a));
    }

    @Test public void testMatchTerms() throws MatchException {
        final IPattern pattern = new TermPattern(B.newAppl(g, a, b));
        final ISubstitution.Immutable result = pattern.match(B.newAppl(g, B.newList(x), y));
        assertEquals(B.newList(x), result.apply(a));
        assertEquals(y, result.apply(b));
    }

    @Test(expected = MatchException.class) public void testMatchFail() throws MatchException {
        final IPattern pattern = new TermPattern(B.newAppl(g, a));
        pattern.match(b);
    }

}