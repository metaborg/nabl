package mb.nabl2.terms.unification;

import static mb.nabl2.terms.build.TermBuild.B;
import static org.junit.Assert.*;

import org.junit.Test;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;

@SuppressWarnings("unused")
public class PersistentUnifierFiniteTest {

    private final ITermVar a = B.newVar("", "a");
    private final ITermVar b = B.newVar("", "b");
    private final ITermVar c = B.newVar("", "c");

    private final String f = "f";
    private final String g = "g";
    private final String h = "h";

    private final ITerm x = B.newString("x");
    private final ITerm y = B.newString("y");
    private final ITerm z = B.newString("z");

    @Test public void testEmpty() throws UnificationException {
        final IUnifier.Transient phi = PersistentUnifier.Transient.of();
        phi.unify(a, a);
        assertTrue(phi.isEmpty());
        assertEquals(0, phi.size());
    }

    @Test public void testNonEmpty() throws UnificationException {
        final IUnifier.Transient phi = PersistentUnifier.Transient.of();
        phi.unify(a, x);
        phi.unify(b, y);
        assertFalse(phi.isEmpty());
        assertEquals(2, phi.size());
    }

    @Test public void testVarIdentity() throws UnificationException {
        final IUnifier.Transient phi = PersistentUnifier.Transient.of();
        assertTrue(phi.areEqual(a, a));
    }

    @Test public void testTermIdentity() throws UnificationException {
        final IUnifier.Transient phi = PersistentUnifier.Transient.of();
        assertTrue(phi.areEqual(B.newAppl(f, a), B.newAppl(f, a)));
    }

    @Test public void testUnifySameVar() throws UnificationException {
        final IUnifier.Transient phi = PersistentUnifier.Transient.of();
        phi.unify(a, a);
        assertFalse(phi.contains(a));
    }

    @Test public void testUnifyTermArgs() throws UnificationException {
        final IUnifier.Transient phi = PersistentUnifier.Transient.of();
        phi.unify(a, B.newAppl(f, b));
        phi.unify(a, B.newAppl(f, x));
        assertTrue(phi.areEqual(b, x));
    }

    @Test(expected = UnificationException.class) public void testUnifyOccursDirect() throws UnificationException {
        final IUnifier.Transient phi = PersistentUnifier.Transient.of();
        phi.unify(a, B.newAppl(f, a));
    }

    @Test(expected = UnificationException.class) public void testUnifyOccursIndirect() throws UnificationException {
        IUnifier.Transient phi = PersistentUnifier.Transient.of();
        phi.unify(a, B.newAppl(f, b));
        phi.unify(b, B.newAppl(g, a));
    }

    @Test public void testMatchVars() throws MatchException {
        final IUnifier.Transient phi = PersistentUnifier.Transient.of();
        phi.match(a, b);
        assertEquals(b, phi.findTerm(a));
    }

    @Test public void testMatchVarTerm() throws MatchException {
        final IUnifier.Transient phi = PersistentUnifier.Transient.of();
        phi.match(a, B.newAppl(g, x, b));
        assertEquals(B.newAppl(g, x, b), phi.findTerm(a));
    }

    @Test public void testMatchTerms() throws MatchException {
        final IUnifier.Transient phi = PersistentUnifier.Transient.of();
        phi.match(B.newAppl(g, a, b), B.newAppl(g, B.newList(x), y));
        assertEquals(B.newList(x), phi.findTerm(a));
        assertEquals(y, phi.findTerm(b));
    }

    @Test(expected = MatchException.class) public void testMatchFail() throws MatchException {
        final IUnifier.Transient phi = PersistentUnifier.Transient.of();
        phi.match(B.newAppl(g, a), b);
    }

    @Test public void testUnifyMakeEqualReps() throws UnificationException {
        IUnifier.Transient phi = PersistentUnifier.Transient.of();
        phi.unify(a, b);
        assertEquals(phi.findRep(a), phi.findRep(b));
    }

    @Test public void testGround() throws UnificationException {
        IUnifier.Transient phi = PersistentUnifier.Transient.of();
        phi.unify(a, x);
        assertTrue(phi.isGround(a));
    }

    @Test public void testSize() throws UnificationException {
        IUnifier.Transient phi = PersistentUnifier.Transient.of();
        phi.unify(a, B.newAppl(f, x, y));
        assertEquals(TermSize.of(3), phi.size(a));
    }

    @Test public void testString() throws UnificationException {
        IUnifier.Transient phi = PersistentUnifier.Transient.of();
        phi.unify(a, B.newAppl(f, x, y));
        assertEquals("f(\"x\",\"y\")", phi.toString(a));
    }

    @Test public void testRemove() throws UnificationException {
        IUnifier.Transient phi = PersistentUnifier.Transient.of();
        phi.unify(a, B.newAppl(f, b));
        phi.unify(b, x);
        phi.remove(b);
        assertTrue(phi.areEqual(a, B.newAppl(f, x)));
    }

    @Test public void testRetain() throws UnificationException {
        IUnifier.Transient phi = PersistentUnifier.Transient.of();
        phi.unify(a, B.newAppl(f, b));
        phi.unify(b, x);
        phi.retain(a);
        assertTrue(phi.areEqual(a, B.newAppl(f, x)));
    }

    @Test public void testEquals() throws UnificationException {
        IUnifier.Transient phi = PersistentUnifier.Transient.of();
        phi.unify(a, B.newAppl(f, b));
        phi.unify(b, x);
        IUnifier.Transient theta = PersistentUnifier.Transient.of();
        theta.unify(a, B.newAppl(f, x));
        theta.unify(b, x);
        assertEquals(phi, theta);
    }

    @Test public void testRetainRemoveInverse() throws UnificationException {
        IUnifier.Transient phi = PersistentUnifier.Transient.of();
        phi.unify(a, B.newAppl(f, b));
        phi.unify(b, x);
        IUnifier.Immutable theta = phi.freeze();
        IUnifier.Immutable theta1 = theta.remove(b).unifier();
        IUnifier.Immutable theta2 = theta.retain(a).unifier();
        assertEquals(theta1, theta2);
    }

    @Test public void testEntailment() throws UnificationException {
        IUnifier.Transient phi = PersistentUnifier.Transient.of();
        phi.unify(a, B.newAppl(f, b));
        phi.unify(b, x);
        IUnifier.Transient theta = PersistentUnifier.Transient.of();
        theta.unify(a, B.newAppl(f, x));
        theta.unify(b, x);
        theta.unify(b, c);
        theta.remove(c);
        assertEquals(phi, theta);
    }

}