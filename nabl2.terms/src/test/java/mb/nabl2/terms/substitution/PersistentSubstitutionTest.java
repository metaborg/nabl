package mb.nabl2.terms.substitution;

import static mb.nabl2.terms.build.TermBuild.B;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;

@SuppressWarnings("unused")
public class PersistentSubstitutionTest {

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
        final ISubstitution.Transient phi = PersistentSubstitution.Transient.of();
        phi.match(a, b);
        assertEquals(b, phi.apply(a));
    }

    @Test public void testMatchVarTerm() throws MatchException {
        final ISubstitution.Transient phi = PersistentSubstitution.Transient.of();
        phi.match(a, B.newAppl(g, x, b));
        assertEquals(B.newAppl(g, x, b), phi.apply(a));
    }

    @Test public void testMatchTerms() throws MatchException {
        final ISubstitution.Transient phi = PersistentSubstitution.Transient.of();
        phi.match(B.newAppl(g, a, b), B.newAppl(g, B.newList(x), y));
        assertEquals(B.newList(x), phi.apply(a));
        assertEquals(y, phi.apply(b));
    }

    @Test(expected = MatchException.class) public void testMatchFail() throws MatchException {
        final ISubstitution.Transient phi = PersistentSubstitution.Transient.of();
        phi.match(B.newAppl(g, a), b);
    }

}