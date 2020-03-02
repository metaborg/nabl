package mb.nabl2.terms.unification.u;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.unification.UnifierTests.assertSame;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.OccursException;
import mb.nabl2.terms.unification.TermSize;

@SuppressWarnings("unused")
public class UnifierInfiniteTest {

    private final ITermVar a = B.newVar("", "a");
    private final ITermVar b = B.newVar("", "b");
    private final ITermVar c = B.newVar("", "c");

    private final String f = "f";
    private final String g = "g";
    private final String h = "h";

    private final ITerm x = B.newString("x");
    private final ITerm y = B.newString("y");
    private final ITerm z = B.newString("z");

    @Test(timeout = 10000) public void testCyclicVarFree() throws OccursException {
        IUnifier.Transient phi = PersistentUnifier.Immutable.of(false).melt();
        phi.unify(a, B.newAppl(f, a)).orElseThrow(() -> new IllegalArgumentException());
        assertFalse(phi.freeVarSet().contains(a));
    }

    @Test(timeout = 10000) public void testCyclicSize() throws OccursException {
        IUnifier.Transient phi = PersistentUnifier.Immutable.of(false).melt();
        phi.unify(a, B.newAppl(f, a)).orElseThrow(() -> new IllegalArgumentException());
        assertEquals(TermSize.INF, phi.size(a));
    }

    @Test(timeout = 10000) public void testCyclicGround() throws OccursException {
        IUnifier.Transient phi = PersistentUnifier.Immutable.of(false).melt();
        phi.unify(a, B.newAppl(f, a)).orElseThrow(() -> new IllegalArgumentException());
        assertTrue(phi.isGround(a));
    }

    @Test(timeout = 10000) public void testCyclicEquals() throws OccursException {
        IUnifier.Transient phi = PersistentUnifier.Immutable.of(false).melt();
        phi.unify(a, B.newAppl(f, a)).orElseThrow(() -> new IllegalArgumentException());
        IUnifier.Transient theta = PersistentUnifier.Immutable.of(false).melt();
        theta.unify(a, B.newAppl(f, a)).orElseThrow(() -> new IllegalArgumentException());
        assertSame(phi.freeze(), theta.freeze()); // equality on transients is broken
    }

    @Test(timeout = 10000) public void testCyclicToString() throws OccursException {
        IUnifier.Transient phi = PersistentUnifier.Immutable.of(false).melt();
        phi.unify(a, B.newAppl(f, a, b)).orElseThrow(() -> new IllegalArgumentException());
        phi.unify(b, B.newAppl(g, a)).orElseThrow(() -> new IllegalArgumentException());
        assertEquals("μX0.f(X0,g(X0))", phi.toString(a));
        assertEquals("μX1.g(μX0.f(X0,X1))", phi.toString(b));
    }

}