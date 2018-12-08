package mb.nabl2.terms.unification;

import static mb.nabl2.terms.build.TermBuild.B;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;

@SuppressWarnings("unused")
public class PersistentUnifierInfiniteTest {

    private final ITermVar a = B.newVar("", "a");
    private final ITermVar b = B.newVar("", "b");
    private final ITermVar c = B.newVar("", "c");

    private final String f = "f";
    private final String g = "g";
    private final String h = "h";

    private final ITerm x = B.newString("x");
    private final ITerm y = B.newString("y");
    private final ITerm z = B.newString("z");

    @Test public void testCyclicVarFree() throws OccursException {
        IUnifier.Transient phi = PersistentUnifier.Transient.of(false);
        phi.unify(a, B.newAppl(f, a)).orElseThrow(() -> new IllegalArgumentException());
        assertFalse(phi.freeVarSet().contains(a));
    }

    @Test public void testCyclicSize() throws OccursException {
        IUnifier.Transient phi = PersistentUnifier.Transient.of(false);
        phi.unify(a, B.newAppl(f, a)).orElseThrow(() -> new IllegalArgumentException());
        assertEquals(TermSize.INF, phi.size(a));
    }

    @Test public void testCyclicGround() throws OccursException {
        IUnifier.Transient phi = PersistentUnifier.Transient.of(false);
        phi.unify(a, B.newAppl(f, a)).orElseThrow(() -> new IllegalArgumentException());
        assertTrue(phi.isGround(a));
    }

    @Test public void testCyclicEquals() throws OccursException {
        IUnifier.Transient phi = PersistentUnifier.Transient.of(false);
        phi.unify(a, B.newAppl(f, a)).orElseThrow(() -> new IllegalArgumentException());
        IUnifier.Transient theta = PersistentUnifier.Transient.of(false);
        theta.unify(a, B.newAppl(f, a)).orElseThrow(() -> new IllegalArgumentException());
        assertEquals(phi, theta);
    }

}