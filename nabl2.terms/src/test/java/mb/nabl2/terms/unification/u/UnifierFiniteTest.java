package mb.nabl2.terms.unification.u;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;
import static mb.nabl2.terms.unification.UnifierTests.assertContains;
import static mb.nabl2.terms.unification.UnifierTests.assertNotContains;
import static mb.nabl2.terms.unification.UnifierTests.assertPresent;
import static mb.nabl2.terms.unification.UnifierTests.assertSame;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;
import org.metaborg.util.collection.CapsuleUtil;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.OccursException;
import mb.nabl2.terms.unification.RigidException;
import mb.nabl2.terms.unification.SpecializedTermFormatter;
import mb.nabl2.terms.unification.TermSize;

@SuppressWarnings("unused")
public class UnifierFiniteTest {

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
        IUnifier.Transient phi = PersistentUnifier.Immutable.of().melt();
        phi.unify(a, a).orElseThrow(() -> new IllegalArgumentException());
        assertTrue(phi.isEmpty());
    }

    @Test(timeout = 10000) public void testNonEmpty() throws OccursException {
        IUnifier.Transient phi = PersistentUnifier.Immutable.of().melt();
        phi.unify(a, x).orElseThrow(() -> new IllegalArgumentException());
        phi.unify(b, y).orElseThrow(() -> new IllegalArgumentException());
        assertFalse(phi.isEmpty());
    }

    @Test(timeout = 10000) public void testVarIdentity() throws OccursException {
        IUnifier.Transient phi = PersistentUnifier.Immutable.of().melt();
        assertTrue(phi.diff(a, a).orElseThrow(() -> new RuntimeException()).isEmpty());
    }

    @Test(timeout = 10000) public void testTermIdentity() throws OccursException {
        IUnifier.Transient phi = PersistentUnifier.Immutable.of().melt();
        assertTrue(phi.diff(B.newAppl(f, a), B.newAppl(f, a)).orElseThrow(() -> new RuntimeException()).isEmpty());
    }

    @Test(timeout = 10000) public void testUnifySameVar() throws OccursException {
        IUnifier.Transient phi = PersistentUnifier.Immutable.of().melt();
        phi.unify(a, a).orElseThrow(() -> new IllegalArgumentException());
        assertFalse(phi.contains(a));
    }

    @Test(timeout = 10000) public void testUnifyTermArgs() throws OccursException {
        IUnifier.Transient phi = PersistentUnifier.Immutable.of().melt();
        phi.unify(a, B.newAppl(f, b)).orElseThrow(() -> new IllegalArgumentException());
        phi.unify(a, B.newAppl(f, x)).orElseThrow(() -> new IllegalArgumentException());
        assertTrue(phi.diff(b, x).orElseThrow(() -> new RuntimeException()).isEmpty());
    }

    @Test(timeout = 10000, expected = OccursException.class) public void testUnifyOccursDirect()
            throws OccursException {
        IUnifier.Transient phi = PersistentUnifier.Immutable.of().melt();
        phi.unify(a, B.newAppl(f, a)).orElseThrow(() -> new IllegalArgumentException());
    }

    @Test(timeout = 10000, expected = OccursException.class) public void testUnifyOccursIndirect()
            throws OccursException {
        IUnifier.Transient phi = PersistentUnifier.Immutable.of().melt();
        phi.unify(a, B.newAppl(f, b)).orElseThrow(() -> new IllegalArgumentException());
        phi.unify(b, B.newAppl(g, a)).orElseThrow(() -> new IllegalArgumentException());
    }

    @Test(timeout = 10000) public void testUnifyMakeEqualReps() throws OccursException {
        IUnifier.Transient phi = PersistentUnifier.Immutable.of().melt();
        phi.unify(a, b).orElseThrow(() -> new IllegalArgumentException());
        assertEquals(phi.findRep(a), phi.findRep(b));
    }

    @Test(timeout = 10000) public void testGround() throws OccursException {
        IUnifier.Transient phi = PersistentUnifier.Immutable.of().melt();
        phi.unify(a, x).orElseThrow(() -> new IllegalArgumentException());
        assertTrue(phi.isGround(a));
    }

    @Test(timeout = 10000) public void testSize() throws OccursException {
        IUnifier.Transient phi = PersistentUnifier.Immutable.of().melt();
        phi.unify(a, B.newAppl(f, x, y)).orElseThrow(() -> new IllegalArgumentException());
        assertEquals(TermSize.of(3), phi.size(a));
    }

    @Test(timeout = 10000) public void testString() throws OccursException {
        IUnifier.Transient phi = PersistentUnifier.Immutable.of().melt();
        phi.unify(a, B.newAppl(f, x, y)).orElseThrow(() -> new IllegalArgumentException());
        assertEquals("f(\"x\",\"y\")", phi.toString(a));
    }

    @Test(timeout = 10000) public void testRemoveInBetweenUnifiedVar() throws OccursException {
        Map.Transient<ITermVar, ITermVar> reps = Map.Transient.of();
        Map.Transient<ITermVar, ITerm> terms = Map.Transient.of();
        reps.__put(a, b);
        reps.__put(b, c);
        terms.__put(d, B.newAppl(f, a));
        IUnifier.Transient phi =
                PersistentUnifier.Immutable.of(true, reps.freeze(), Map.Immutable.of(), terms.freeze()).melt();
        assertTrue(phi.domainSet().contains(b));
        phi.remove(b);
        assertFalse(phi.domainSet().contains(b));
        assertTrue(phi.diff(a, c).orElseThrow(() -> new RuntimeException()).isEmpty());
        assertTrue(phi.diff(d, B.newAppl(f, c)).orElseThrow(() -> new RuntimeException()).isEmpty());
    }

    @Test(timeout = 10000) public void testRemoveFreeVar() throws OccursException {
        Map.Transient<ITermVar, ITermVar> reps = Map.Transient.of();
        Map.Transient<ITermVar, ITerm> terms = Map.Transient.of();
        reps.__put(a, b);
        reps.__put(b, c);
        IUnifier.Transient phi =
                PersistentUnifier.Immutable.of(true, reps.freeze(), Map.Immutable.of(), terms.freeze()).melt();
        phi.remove(c);
        assertFalse(phi.domainSet().contains(c));
        assertTrue(phi.diff(a, b).orElseThrow(() -> new RuntimeException()).isEmpty());
        assertFalse(phi.diff(b, c).orElseThrow(() -> new RuntimeException()).isEmpty());
    }

    @Test(timeout = 10000) public void testRemoveVarWithTerm() throws OccursException {
        Map.Transient<ITermVar, ITermVar> reps = Map.Transient.of();
        Map.Transient<ITermVar, ITerm> terms = Map.Transient.of();
        reps.__put(a, b);
        terms.__put(b, B.newAppl(f, c));
        IUnifier.Transient phi =
                PersistentUnifier.Immutable.of(true, reps.freeze(), Map.Immutable.of(), terms.freeze()).melt();
        assertTrue(phi.domainSet().contains(b));
        phi.remove(b);
        assertFalse(phi.domainSet().contains(b));
        assertTrue(phi.diff(a, B.newAppl(f, c)).orElseThrow(() -> new RuntimeException()).isEmpty());
    }

    @Test(timeout = 10000) public void testRemoveVarsWithTerm() throws OccursException {
        Map.Transient<ITermVar, ITermVar> reps = Map.Transient.of();
        Map.Transient<ITermVar, ITerm> terms = Map.Transient.of();
        reps.__put(a, b);
        reps.__put(b, c);
        terms.__put(c, B.newAppl(f, d));
        IUnifier.Transient phi =
                PersistentUnifier.Immutable.of(true, reps.freeze(), Map.Immutable.of(), terms.freeze()).melt();
        assertTrue(phi.domainSet().contains(a));
        assertTrue(phi.domainSet().contains(b));
        phi.removeAll(Arrays.asList(a, b));
        assertFalse(phi.domainSet().contains(a));
        assertFalse(phi.domainSet().contains(b));
        assertTrue(phi.diff(c, B.newAppl(f, d)).orElseThrow(() -> new RuntimeException()).isEmpty());
    }

    @Test(timeout = 10000) public void testRetain() throws OccursException {
        IUnifier.Transient phi = PersistentUnifier.Immutable.of().melt();
        phi.unify(a, B.newAppl(f, b)).orElseThrow(() -> new IllegalArgumentException());
        phi.unify(b, x).orElseThrow(() -> new IllegalArgumentException());
        phi.retain(a);
        assertTrue(phi.diff(a, B.newAppl(f, x)).orElseThrow(() -> new RuntimeException()).isEmpty());
    }

    @Test(timeout = 10000) public void testEquals() throws OccursException {
        IUnifier.Transient phi = PersistentUnifier.Immutable.of().melt();
        phi.unify(b, x).orElseThrow(() -> new IllegalArgumentException());
        phi.unify(a, B.newAppl(f, b)).orElseThrow(() -> new IllegalArgumentException());
        IUnifier.Transient theta = PersistentUnifier.Immutable.of().melt();
        theta.unify(a, B.newAppl(f, x)).orElseThrow(() -> new IllegalArgumentException());
        theta.unify(b, x).orElseThrow(() -> new IllegalArgumentException());
        assertSame(phi.freeze(), theta.freeze()); // equality on transients is broken
    }

    @Test(timeout = 10000) public void testEquals2() throws OccursException {
        IUnifier.Transient phi = PersistentUnifier.Immutable.of().melt();
        phi.unify(b, c).orElseThrow(() -> new IllegalArgumentException());
        IUnifier.Transient theta = PersistentUnifier.Immutable.of().melt();
        assertNotEquals(phi, theta);
    }

    @Test(timeout = 10000) public void testEquals3() throws OccursException {
        IUnifier.Transient phi = PersistentUnifier.Immutable.of().melt();
        phi.unify(a, B.newAppl(f, b)).orElseThrow(() -> new IllegalArgumentException());
        IUnifier.Transient theta = PersistentUnifier.Immutable.of().melt();
        theta.unify(a, B.newAppl(f, c)).orElseThrow(() -> new IllegalArgumentException());
        assertNotEquals(phi, theta);
    }

    @Test(timeout = 10000) public void testEquals4() throws OccursException {
        IUnifier.Transient phi = PersistentUnifier.Immutable.of().melt();
        phi.unify(a, B.newAppl(f, b)).orElseThrow(() -> new IllegalArgumentException());
        phi.unify(b, B.newAppl(g)).orElseThrow(() -> new IllegalArgumentException());
        IUnifier.Transient theta = PersistentUnifier.Immutable.of().melt();
        theta.unify(a, B.newAppl(f, B.newAppl(g))).orElseThrow(() -> new IllegalArgumentException());
        assertNotEquals(phi, theta);
    }

    @Test(timeout = 10000) public void testEquivalenceClasses() throws OccursException {
        final IUnifier.Immutable phi =
                PersistentUnifier.Immutable.of(true, Map.Immutable.of(a, b), Map.Immutable.of(), Map.Immutable.of());
        final IUnifier.Immutable theta =
                PersistentUnifier.Immutable.of(true, Map.Immutable.of(b, a), Map.Immutable.of(), Map.Immutable.of());
        assertSame(phi, theta);
    }

    @Test(timeout = 10000) public void testRetainRemoveInverse() throws OccursException {
        IUnifier.Transient phi = PersistentUnifier.Immutable.of().melt();
        phi.unify(a, B.newAppl(f, b)).orElseThrow(() -> new IllegalArgumentException());
        phi.unify(b, x).orElseThrow(() -> new IllegalArgumentException());
        IUnifier.Immutable theta = phi.freeze();
        IUnifier.Immutable theta1 = theta.remove(b).unifier();
        IUnifier.Immutable theta2 = theta.retain(a).unifier();
        assertSame(theta1, theta2);
    }

    @Test(timeout = 10000) public void testEntailment() throws OccursException {
        IUnifier.Transient phi = PersistentUnifier.Immutable.of().melt();
        phi.unify(a, B.newAppl(f, b)).orElseThrow(() -> new IllegalArgumentException());
        phi.unify(b, x).orElseThrow(() -> new IllegalArgumentException());
        IUnifier.Transient theta = PersistentUnifier.Immutable.of().melt();
        theta.unify(a, B.newAppl(f, x)).orElseThrow(() -> new IllegalArgumentException());
        theta.unify(b, x).orElseThrow(() -> new IllegalArgumentException());
        theta.unify(b, c).orElseThrow(() -> new IllegalArgumentException());
        theta.remove(c);
        assertSame(phi.freeze(), theta.freeze());
    }

    @Test(timeout = 10000, expected = OccursException.class) public void testRecursive1() throws OccursException {
        IUnifier.Transient phi = PersistentUnifier.Immutable.of().melt();
        phi.unify(B.newTuple(a, a), B.newTuple(a, B.newAppl(f, a)));
    }

    @Test(timeout = 10000, expected = OccursException.class) public void testRecursive2() throws OccursException {
        IUnifier.Transient phi = PersistentUnifier.Immutable.of().melt();
        phi.unify(B.newTuple(a, a), B.newTuple(B.newAppl(f, a), a));
    }

    @Test(timeout = 10000, expected = OccursException.class) public void testRecursive3() throws OccursException {
        IUnifier.Transient phi = PersistentUnifier.Immutable.of().melt();
        phi.unify(B.newTuple(a, B.newAppl(f, a)), B.newTuple(a, a));
    }

    @Test(timeout = 10000, expected = OccursException.class) public void testRecursive4() throws OccursException {
        IUnifier.Transient phi = PersistentUnifier.Immutable.of().melt();
        phi.unify(B.newTuple(B.newAppl(f, a), a), B.newTuple(a, a));
    }

    @Test(timeout = 10000, expected = OccursException.class) public void testRecursive5() throws OccursException {
        IUnifier.Transient phi = PersistentUnifier.Immutable.of().melt();
        phi.unify(a, B.newAppl(f, b));
        phi.unify(b, B.newAppl(f, a));
    }

    @Test(timeout = 10000, expected = OccursException.class) public void testRecursive6() throws OccursException {
        IUnifier.Transient phi = PersistentUnifier.Immutable.of().melt();
        phi.unify(B.newTuple(a, B.newAppl(f, a)), B.newTuple(B.newAppl(f, b), a));
    }

    @Test(timeout = 10000, expected = OccursException.class) public void testRecursive7() throws OccursException {
        IUnifier.Transient phi = PersistentUnifier.Immutable.of().melt();
        phi.unify(B.newTuple(B.newAppl(f, a), a), B.newTuple(b, B.newAppl(f, b)));
    }

    @Test(timeout = 10000) public void testVarSets1() throws OccursException {
        IUnifier.Transient phi = PersistentUnifier.Immutable.of().melt();

        assertPresent(phi.unify(a, B.newAppl(f, b)));
        assertEquals(Set.Immutable.of(a, b), phi.varSet());
        assertEquals(Set.Immutable.of(a), phi.domainSet());
        assertEquals(Set.Immutable.of(b), phi.rangeSet());

        assertPresent(phi.unify(b, B.newAppl(f, c)));
        assertEquals(CapsuleUtil.toSet(a, b, c), phi.varSet());
        assertEquals(Set.Immutable.of(a, b), phi.domainSet());
        assertEquals(Set.Immutable.of(c), phi.rangeSet());

        phi.remove(b);
        assertEquals(Set.Immutable.of(a, c), phi.varSet());
        assertEquals(Set.Immutable.of(a), phi.domainSet());
        assertEquals(Set.Immutable.of(c), phi.rangeSet());
    }

    @Test(timeout = 10000) public void testVarSets2() throws OccursException {
        IUnifier.Transient phi = PersistentUnifier.Immutable.of().melt();

        assertPresent(phi.unify(a, B.newAppl(f, b)));
        assertEquals(Set.Immutable.of(a, b), phi.varSet());
        assertEquals(Set.Immutable.of(a), phi.domainSet());
        assertEquals(Set.Immutable.of(b), phi.rangeSet());

        assertPresent(phi.unify(c, B.newAppl(f, d)));
        assertEquals(CapsuleUtil.toSet(a, b, c, d), phi.varSet());
        assertEquals(CapsuleUtil.toSet(a, c), phi.domainSet());
        assertEquals(CapsuleUtil.toSet(b, d), phi.rangeSet());

        assertPresent(phi.unify(b, d));
        assertEquals(CapsuleUtil.toSet(a, b, c, d), phi.varSet());
        assertTrue((phi.domainSet().contains(b) && phi.rangeSet().contains(d))
                || (phi.domainSet().contains(d) && phi.rangeSet().contains(b)));
    }

    @Test(timeout = 10000) public void testUnifyFreeRigidVarFreeVar() throws OccursException, RigidException {
        IUnifier.Transient phi = PersistentUnifier.Immutable.of().melt();
        assertPresent(phi.unify(a, b, Set.Immutable.of(a)::contains));
    }

    @Test(timeout = 10000) public void testUnifyFreeRigidVarFreeRigidVar() throws OccursException, RigidException {
        IUnifier.Transient phi = PersistentUnifier.Immutable.of().melt();
        try {
            phi.unify(a, b, Set.Immutable.of(a, b)::contains);
            throw new AssertionError("Expected RigidException.");
        } catch(RigidException ex) {
            assertContains(a, ex.vars());
            assertContains(b, ex.vars());
        }
    }

    @Test(timeout = 10000) public void testUnifyFreeRigidVarBoundRigidVar() throws OccursException, RigidException {
        IUnifier.Transient phi = PersistentUnifier.Immutable.of().melt();
        assertPresent(phi.unify(b, x));
        try {
            phi.unify(a, b, Set.Immutable.of(a, b)::contains);
            throw new AssertionError("Expected RigidException.");
        } catch(RigidException ex) {
            assertContains(a, ex.vars());
            assertNotContains(b, ex.vars());
        }
    }


    @Test(timeout = 10000) public void testUnifyBoundRigidVarBoundRigidVar() throws OccursException, RigidException {
        IUnifier.Transient phi = PersistentUnifier.Immutable.of().melt();
        assertPresent(phi.unify(a, x));
        assertPresent(phi.unify(b, x));
        assertPresent(phi.unify(a, b, Set.Immutable.of(a, b)::contains));

    }

    @Test(timeout = 10000) public void testUnifyBoundVarBoundRigidVar() throws OccursException, RigidException {
        IUnifier.Transient phi = PersistentUnifier.Immutable.of().melt();
        assertPresent(phi.unify(a, x));
        assertPresent(phi.unify(b, x));
        assertPresent(phi.unify(a, b, Set.Immutable.of(b)::contains));

    }

    @Test(timeout = 10000) public void testUnifyUnifiedFreeRigidVar() throws OccursException, RigidException {
        IUnifier.Transient phi = PersistentUnifier.Immutable
                .of(true, Map.Immutable.of(a, b), Map.Immutable.of(), Map.Immutable.of()).melt();
        try {
            phi.unify(a, x, Set.Immutable.of(a, b)::contains);
            throw new AssertionError("Expected RigidException.");
        } catch(RigidException ex) {
            assertContains(b, ex.vars());
            assertNotContains(a, ex.vars());
        }
    }

    @Test(timeout = 10000) public void testUnifyUnifiedBoundRigidVar() throws OccursException, RigidException {
        IUnifier.Transient phi = PersistentUnifier.Immutable
                .of(true, Map.Immutable.of(), Map.Immutable.of(), Map.Immutable.of(a, x)).melt();
        try {
            phi.unify(a, b, Set.Immutable.of(a, b)::contains);
            throw new AssertionError("Expected RigidException.");
        } catch(RigidException ex) {
            assertContains(b, ex.vars());
            assertNotContains(a, ex.vars());
        }
    }

    @Test(timeout = 10000) public void testSpecializedToString() throws OccursException {
        SpecializedTermFormatter stf = (t, u, fmt) -> M.appl1(f, M.term(), (t0, t1) -> {
            return "`f`(" + fmt.format(t1) + ")";
        }).match(t, u);
        IUnifier.Transient phi = PersistentUnifier.Immutable.of(false).melt();
        assertPresent(phi.unify(a, B.newAppl(f, b)));
        assertPresent(phi.unify(b, B.newAppl(g, B.newAppl(f, c))));
        assertEquals("`f`(g(`f`(?c)))", phi.toString(a, stf));
    }

}
