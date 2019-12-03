package mb.nabl2.terms.unification.ud;

import static mb.nabl2.terms.build.TermBuild.B;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.junit.Test;
import org.metaborg.util.iterators.Iterables2;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.OccursException;
import mb.nabl2.terms.unification.TermSize;

@SuppressWarnings("unused")
public class UniDisunifierFiniteTest {

    private final ITermVar a = B.newVar("", "a");
    private final ITermVar b = B.newVar("", "b");
    private final ITermVar c = B.newVar("", "c");
    private final ITermVar d = B.newVar("", "d");

    private final String f = "f";
    private final String g = "g";
    private final String h = "h";

    private final ITerm x = B.newString("x");
    private final ITerm y = B.newString("y");
    private final ITerm z = B.newString("z");

    @Test(timeout = 10000) public void testEmpty() throws OccursException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();
        phi.unify(a, a).orElseThrow(() -> new IllegalArgumentException());
        assertTrue(phi.isEmpty());
    }

    @Test(timeout = 10000) public void testNonEmpty() throws OccursException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();
        phi.unify(a, x).orElseThrow(() -> new IllegalArgumentException());
        phi.unify(b, y).orElseThrow(() -> new IllegalArgumentException());
        assertFalse(phi.isEmpty());
    }

    @Test(timeout = 10000) public void testVarIdentity() throws OccursException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();
        assertTrue(phi.diff(a, a).orElseThrow(() -> new RuntimeException()).isEmpty());
    }

    @Test(timeout = 10000) public void testTermIdentity() throws OccursException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();
        assertTrue(phi.diff(B.newAppl(f, a), B.newAppl(f, a)).orElseThrow(() -> new RuntimeException()).isEmpty());
    }

    @Test(timeout = 10000) public void testUnifySameVar() throws OccursException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();
        phi.unify(a, a).orElseThrow(() -> new IllegalArgumentException());
        assertFalse(phi.contains(a));
    }

    @Test(timeout = 10000) public void testUnifyTermArgs() throws OccursException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();
        phi.unify(a, B.newAppl(f, b)).orElseThrow(() -> new IllegalArgumentException());
        phi.unify(a, B.newAppl(f, x)).orElseThrow(() -> new IllegalArgumentException());
        assertTrue(phi.diff(b, x).orElseThrow(() -> new RuntimeException()).isEmpty());
    }

    @Test(timeout = 10000, expected = OccursException.class) public void testUnifyOccursDirect()
            throws OccursException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();
        phi.unify(a, B.newAppl(f, a)).orElseThrow(() -> new IllegalArgumentException());
    }

    @Test(timeout = 10000, expected = OccursException.class) public void testUnifyOccursIndirect()
            throws OccursException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();
        phi.unify(a, B.newAppl(f, b)).orElseThrow(() -> new IllegalArgumentException());
        phi.unify(b, B.newAppl(g, a)).orElseThrow(() -> new IllegalArgumentException());
    }

    @Test(timeout = 10000) public void testUnifyMakeEqualReps() throws OccursException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();
        phi.unify(a, b).orElseThrow(() -> new IllegalArgumentException());
        assertEquals(phi.findRep(a), phi.findRep(b));
    }

    @Test(timeout = 10000) public void testGround() throws OccursException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();
        phi.unify(a, x).orElseThrow(() -> new IllegalArgumentException());
        assertTrue(phi.isGround(a));
    }

    @Test(timeout = 10000) public void testSize() throws OccursException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();
        phi.unify(a, B.newAppl(f, x, y)).orElseThrow(() -> new IllegalArgumentException());
        assertEquals(TermSize.of(3), phi.size(a));
    }

    @Test(timeout = 10000) public void testString() throws OccursException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();
        phi.unify(a, B.newAppl(f, x, y)).orElseThrow(() -> new IllegalArgumentException());
        assertEquals("f(\"x\",\"y\")", phi.toString(a));
    }

    @Test(timeout = 10000) public void testRemoveUnifiedVar() throws OccursException {
        Map.Transient<ITermVar, ITermVar> reps = Map.Transient.of();
        Map.Transient<ITermVar, ITerm> terms = Map.Transient.of();
        reps.__put(a, b);
        reps.__put(b, c);
        terms.__put(d, B.newAppl(f, a));
        IUniDisunifier.Transient phi = new PersistentUniDisunifier.Immutable(true, reps.freeze(), Map.Immutable.of(),
                terms.freeze(), Set.Immutable.of()).melt();
        phi.remove(b);
        assertTrue(phi.diff(a, c).orElseThrow(() -> new RuntimeException()).isEmpty());
        assertTrue(phi.diff(d, B.newAppl(f, c)).orElseThrow(() -> new RuntimeException()).isEmpty());
    }

    @Test(timeout = 10000) public void testRemoveFreeVar() throws OccursException {
        Map.Transient<ITermVar, ITermVar> reps = Map.Transient.of();
        Map.Transient<ITermVar, ITerm> terms = Map.Transient.of();
        reps.__put(a, b);
        reps.__put(b, c);
        IUniDisunifier.Transient phi = new PersistentUniDisunifier.Immutable(true, reps.freeze(), Map.Immutable.of(),
                terms.freeze(), Set.Immutable.of()).melt();
        phi.remove(c);
        assertTrue(phi.diff(a, b).orElseThrow(() -> new RuntimeException()).isEmpty());
        assertFalse(phi.diff(b, c).orElseThrow(() -> new RuntimeException()).isEmpty());
    }

    @Test(timeout = 10000) public void testRemoveVarWithTerm() throws OccursException {
        Map.Transient<ITermVar, ITermVar> reps = Map.Transient.of();
        Map.Transient<ITermVar, ITerm> terms = Map.Transient.of();
        reps.__put(a, b);
        terms.__put(b, B.newAppl(f, c));
        IUniDisunifier.Transient phi = new PersistentUniDisunifier.Immutable(true, reps.freeze(), Map.Immutable.of(),
                terms.freeze(), Set.Immutable.of()).melt();
        phi.remove(b);
        assertTrue(phi.diff(a, B.newAppl(f, c)).orElseThrow(() -> new RuntimeException()).isEmpty());
    }

    @Test(timeout = 10000) public void testRetain() throws OccursException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();
        phi.unify(a, B.newAppl(f, b)).orElseThrow(() -> new IllegalArgumentException());
        phi.unify(b, x).orElseThrow(() -> new IllegalArgumentException());
        phi.retain(a);
        assertTrue(phi.diff(a, B.newAppl(f, x)).orElseThrow(() -> new RuntimeException()).isEmpty());
    }

    @Test(timeout = 10000) public void testEquals() throws OccursException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();
        phi.unify(b, x).orElseThrow(() -> new IllegalArgumentException());
        phi.unify(a, B.newAppl(f, b)).orElseThrow(() -> new IllegalArgumentException());
        IUniDisunifier.Transient theta = PersistentUniDisunifier.Immutable.of().melt();
        theta.unify(a, B.newAppl(f, x)).orElseThrow(() -> new IllegalArgumentException());
        theta.unify(b, x).orElseThrow(() -> new IllegalArgumentException());
        assertEquals(phi.freeze(), theta.freeze()); // equality on transients is broken
    }

    @Test(timeout = 10000) public void testEquals2() throws OccursException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();
        phi.unify(b, c).orElseThrow(() -> new IllegalArgumentException());
        IUniDisunifier.Transient theta = PersistentUniDisunifier.Immutable.of().melt();
        assertNotEquals(phi, theta);
    }

    @Test(timeout = 10000) public void testEquals3() throws OccursException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();
        phi.unify(a, B.newAppl(f, b)).orElseThrow(() -> new IllegalArgumentException());
        IUniDisunifier.Transient theta = PersistentUniDisunifier.Immutable.of().melt();
        theta.unify(a, B.newAppl(f, c)).orElseThrow(() -> new IllegalArgumentException());
        assertNotEquals(phi, theta);
    }

    @Test(timeout = 10000) public void testEquals4() throws OccursException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();
        phi.unify(a, B.newAppl(f, b)).orElseThrow(() -> new IllegalArgumentException());
        phi.unify(b, B.newAppl(g)).orElseThrow(() -> new IllegalArgumentException());
        IUniDisunifier.Transient theta = PersistentUniDisunifier.Immutable.of().melt();
        theta.unify(a, B.newAppl(f, B.newAppl(g))).orElseThrow(() -> new IllegalArgumentException());
        assertNotEquals(phi, theta);
    }

    @Test(timeout = 10000) public void testEquivalenceClasses() throws OccursException {
        final IUniDisunifier.Immutable phi = new PersistentUniDisunifier.Immutable(true, Map.Immutable.of(a, b),
                Map.Immutable.of(), Map.Immutable.of(), Set.Immutable.of());
        final IUniDisunifier.Immutable theta = new PersistentUniDisunifier.Immutable(true, Map.Immutable.of(b, a),
                Map.Immutable.of(), Map.Immutable.of(), Set.Immutable.of());
        assertEquals(phi, theta);
    }

    @Test(timeout = 10000) public void testRetainRemoveInverse() throws OccursException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();
        phi.unify(a, B.newAppl(f, b)).orElseThrow(() -> new IllegalArgumentException());
        phi.unify(b, x).orElseThrow(() -> new IllegalArgumentException());
        IUniDisunifier.Immutable theta = phi.freeze();
        IUniDisunifier.Immutable theta1 = theta.remove(b).unifier();
        IUniDisunifier.Immutable theta2 = theta.retain(a).unifier();
        assertEquals(theta1, theta2);
    }

    @Test(timeout = 10000) public void testEntailment() throws OccursException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();
        phi.unify(a, B.newAppl(f, b)).orElseThrow(() -> new IllegalArgumentException());
        phi.unify(b, x).orElseThrow(() -> new IllegalArgumentException());
        IUniDisunifier.Transient theta = PersistentUniDisunifier.Immutable.of().melt();
        theta.unify(a, B.newAppl(f, x)).orElseThrow(() -> new IllegalArgumentException());
        theta.unify(b, x).orElseThrow(() -> new IllegalArgumentException());
        theta.unify(b, c).orElseThrow(() -> new IllegalArgumentException());
        theta.remove(c);
        assertEquals(phi.freeze(), theta.freeze());
    }

    @Test(/*timeout = 10000*/) public void testVariableDisunify() throws OccursException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();
        assertTrue(phi.disunify(a, b));
        assertAbsent(phi.diff(a, b));
    }

    @Test(timeout = 10000) public void testVariableDisunifyAndTransitiveUnify() throws OccursException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();
        assertTrue(phi.disunify(a, b));
        phi.unify(a, c).orElseThrow(() -> new RuntimeException());
        assertAbsent(phi.unify(c, b));
    }

    @Test(timeout = 10000) public void testVariableTransitiveUnifyAndDisunify() throws OccursException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();
        phi.unify(a, c).orElseThrow(() -> new RuntimeException());
        phi.unify(c, b).orElseThrow(() -> new RuntimeException());
        assertFalse(phi.disunify(a, b));
    }

    @Test(timeout = 10000) public void testVariableUnifyDisunifyUnify() throws OccursException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();
        phi.unify(a, c).orElseThrow(() -> new RuntimeException());
        assertTrue(phi.disunify(a, b));
        assertAbsent(phi.unify(c, b));
    }

    @Test(timeout = 10000, expected = OccursException.class) public void testRecursive1() throws OccursException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();
        phi.unify(B.newTuple(a, a), B.newTuple(a, B.newAppl(f, a)));
    }

    @Test(timeout = 10000, expected = OccursException.class) public void testRecursive2() throws OccursException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();
        phi.unify(B.newTuple(a, a), B.newTuple(B.newAppl(f, a), a));
    }

    @Test(timeout = 10000, expected = OccursException.class) public void testRecursive3() throws OccursException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();
        phi.unify(B.newTuple(a, B.newAppl(f, a)), B.newTuple(a, a));
    }

    @Test(timeout = 10000, expected = OccursException.class) public void testRecursive4() throws OccursException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();
        phi.unify(B.newTuple(B.newAppl(f, a), a), B.newTuple(a, a));
    }

    @Test(timeout = 10000, expected = OccursException.class) public void testRecursive5() throws OccursException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();
        phi.unify(a, B.newAppl(f, b));
        phi.unify(b, B.newAppl(f, a));
    }

    @Test(timeout = 10000, expected = OccursException.class) public void testRecursive6() throws OccursException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();
        phi.unify(B.newTuple(a, B.newAppl(f, a)), B.newTuple(B.newAppl(f, b), a));
    }

    @Test(timeout = 10000, expected = OccursException.class) public void testRecursive7() throws OccursException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();
        phi.unify(B.newTuple(B.newAppl(f, a), a), B.newTuple(b, B.newAppl(f, b)));
    }

    @Test(timeout = 10000) public void testDisunifyVariablesWithSameTerms() throws OccursException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();
        phi.unify(a, x).orElseThrow(() -> new RuntimeException());
        phi.unify(b, x).orElseThrow(() -> new RuntimeException());
        assertFalse(phi.disunify(a, b));
    }

    @Test(/*timeout = 10000*/) public void testRemoveDisunifiedVar() throws OccursException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();
        assertTrue(phi.disunify(a, b));
        assertPresent(phi.unify(a, c));
        phi.remove(c);
        assertAbsent(phi.unify(a, b));
    }

    @Test(timeout = 10000) public void testUniversalDisequality() throws OccursException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();
        assertTrue(phi.disunify(Iterables2.singleton(a), b, B.newAppl(f, a)));
        assertAbsent(phi.unify(b, B.newAppl(f, B.newInt(7))));
        assertAbsent(phi.unify(b, B.newAppl(f, c)));
        assertAbsent(phi.unify(b, B.newAppl(f, a)));
    }

    @Test(timeout = 10000) public void testUniversalDisequalityViaVarVarUnify() throws OccursException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();
        assertTrue(phi.disunify(Iterables2.singleton(a), b, B.newAppl(f, a)));
        assertPresent(phi.unify(b, c));
        assertAbsent(phi.unify(c, B.newAppl(f, x)));
    }

    private static <X> void assertPresent(Optional<X> opt) {
        assertTrue(opt.isPresent());
    }

    private static <X> void assertAbsent(Optional<X> opt) {
        assertFalse(opt.isPresent());
    }

}