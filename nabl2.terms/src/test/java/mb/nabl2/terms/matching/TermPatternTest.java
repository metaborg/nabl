package mb.nabl2.terms.matching;

import static mb.nabl2.terms.build.TermBuild.B;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.matching.IPattern.MatchResult;

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
        final MatchResult result = pattern.match(b);
        assertTrue(result.unifier().areEqual(b, result.substitution().apply(a)));
    }

    @Test public void testMatchVarTerm() throws MatchException {
        final IPattern pattern = new TermPattern(a);
        final MatchResult result = pattern.match(B.newAppl(g, x, b));
        assertTrue(result.unifier().areEqual(B.newAppl(g, x, b), result.substitution().apply(a)));
    }

    @Test public void testMatchTerms() throws MatchException {
        final IPattern pattern = new TermPattern(B.newAppl(g, a, b));
        final MatchResult result = pattern.match(B.newAppl(g, B.newList(x), y));
        assertTrue(result.unifier().areEqual(B.newList(x), result.substitution().apply(a)));
        assertTrue(result.unifier().areEqual(y, result.substitution().apply(b)));
    }

    @Test(expected = MatchException.class) public void testMatchFail() throws MatchException {
        final IPattern pattern = new TermPattern(B.newAppl(g, a));
        pattern.match(b);
    }

}