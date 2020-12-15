package mb.nabl2.terms.unification.ud;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.unification.UnifierTests.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.metaborg.util.iterators.Iterables2;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.OccursException;
import mb.nabl2.terms.unification.RigidException;
import mb.nabl2.terms.unification.TermSize;
import mb.nabl2.util.CapsuleUtil;

@SuppressWarnings("unused")
public class UniDisunifierFiniteTest {

    private final ITermVar a = B.newVar("", "a");
    private final ITermVar b = B.newVar("", "b");
    private final ITermVar c = B.newVar("", "c");
    private final ITermVar d = B.newVar("", "d");
    private final ITermVar u = B.newVar("", "u");
    private final ITermVar v = B.newVar("", "v");
    private final ITermVar w = B.newVar("", "w");

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
        assertSame(phi.freeze(), theta.freeze()); // equality on transients is broken
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
        assertSame(phi, theta);
    }

    @Test(timeout = 10000) public void testRetainRemoveInverse() throws OccursException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();
        phi.unify(a, B.newAppl(f, b)).orElseThrow(() -> new IllegalArgumentException());
        phi.unify(b, x).orElseThrow(() -> new IllegalArgumentException());
        IUniDisunifier.Immutable theta = phi.freeze();
        IUniDisunifier.Immutable theta1 = theta.remove(b).unifier();
        IUniDisunifier.Immutable theta2 = theta.retain(a).unifier();
        assertSame(theta1, theta2);
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
        assertSame(phi.freeze(), theta.freeze());
    }

    @Test(timeout = 10000) public void testVariableDisunify() throws OccursException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();
        assertPresent(phi.disunify(a, b));
        assertAbsent(phi.diff(a, b));
    }

    @Test(timeout = 10000) public void testVariableDisunifyAndTransitiveUnify() throws OccursException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();
        assertPresent(phi.disunify(a, b));
        phi.unify(a, c).orElseThrow(() -> new RuntimeException());
        assertAbsent(phi.unify(c, b));
    }

    @Test(timeout = 10000) public void testVariableTransitiveUnifyAndDisunify() throws OccursException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();
        phi.unify(a, c).orElseThrow(() -> new RuntimeException());
        phi.unify(c, b).orElseThrow(() -> new RuntimeException());
        assertAbsent(phi.disunify(a, b));
    }

    @Test(timeout = 10000) public void testVariableUnifyDisunifyUnify() throws OccursException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();
        phi.unify(a, c).orElseThrow(() -> new RuntimeException());
        assertPresent(phi.disunify(a, b));
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
        assertAbsent(phi.disunify(a, b));
    }

    @Test(timeout = 10000) public void testRemoveDisunifiedVar() throws OccursException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();
        assertPresent(phi.disunify(a, b));
        assertPresent(phi.unify(a, c));
        phi.remove(c);
        assertAbsent(phi.unify(a, b));
    }

    @Test(timeout = 10000) public void testUniversalDisequality() throws OccursException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();
        assertPresent(phi.disunify(Iterables2.singleton(a), b, B.newAppl(f, a)));
        assertAbsent(phi.unify(b, B.newAppl(f, B.newInt(7))));
        assertAbsent(phi.unify(b, B.newAppl(f, c)));
        assertAbsent(phi.unify(b, B.newAppl(f, a)));
    }

    @Test(timeout = 10000) public void testUniversalDisequalityViaVarVarUnify() throws OccursException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();
        assertPresent(phi.disunify(Iterables2.singleton(a), b, B.newAppl(f, a)));
        assertPresent(phi.unify(b, c));
        assertAbsent(phi.unify(c, B.newAppl(f, x)));
    }

    @Test(timeout = 10000) public void testUniversalDisequalityShadowingVar() throws OccursException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();
        assertPresent(phi.disunify(Iterables2.singleton(a), b, B.newAppl(f, a)));
        assertPresent(phi.unify(a, x));
        assertAbsent(phi.unify(b, B.newAppl(f, y)));
    }

    @Test(timeout = 10000) public void testDirectlyUnifyRigidVar() throws OccursException, RigidException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();
        final Set.Immutable<ITermVar> rigidVars = Set.Immutable.of(a);
        assertPresent(phi.unify(a, b, rigidVars::contains));
    }

    @Test(expected = RigidException.class, timeout = 10000) public void testDirectlyUnifyRigidVars()
            throws OccursException, RigidException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();
        final Set.Immutable<ITermVar> rigidVars = Set.Immutable.of(a, b);
        phi.unify(a, b, rigidVars::contains);
    }

    @Test(expected = RigidException.class, timeout = 10000) public void testIndirectlyUnifyRigidVars()
            throws OccursException, RigidException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();
        final Set.Immutable<ITermVar> rigidVars = Set.Immutable.of(a, b);
        assertPresent(phi.unify(c, a, rigidVars::contains));
        phi.unify(c, b, rigidVars::contains);
    }

    @Test(expected = RigidException.class, timeout = 10000) public void testDisunifyRigidVars()
            throws OccursException, RigidException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();
        final Set.Immutable<ITermVar> rigidVars = Set.Immutable.of(a, b);
        phi.disunify(a, b, rigidVars::contains);
    }

    @Test(expected = RigidException.class, timeout = 10000) public void testDisunifyRigidVarTerm()
            throws OccursException, RigidException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();
        final Set.Immutable<ITermVar> rigidVars = Set.Immutable.of(a);
        phi.unify(a, x, rigidVars::contains);
    }

    @Test(timeout = 10000) public void testUnifyImpliedDiseqOfRigidVars() throws OccursException, RigidException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();
        final Set.Immutable<ITermVar> rigidVars = Set.Immutable.of(a, b);
        assertPresent(phi.unify(a, b)); // no rigid vars, 
        assertPresent(phi.unify(a, b, rigidVars::contains));
    }

    @Test(timeout = 10000) public void testDisunifyImpliedByDiseqOfRigidVars() throws OccursException, RigidException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();
        final Set.Immutable<ITermVar> rigidVars = Set.Immutable.of(a, b);
        assertPresent(phi.disunify(a, b)); // no rigid vars, 
        assertPresent(phi.disunify(a, b, rigidVars::contains));
    }

    @Test(timeout = 10000) public void testDisunifyOfRigidVarImpliedUniversalDiseq()
            throws OccursException, RigidException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();
        final Set.Immutable<ITermVar> rigidVars = Set.Immutable.of(b);
        assertPresent(phi.disunify(Set.Immutable.of(a), b, B.newList(a)));
        assertPresent(phi.disunify(b, B.newList(x), rigidVars::contains));
    }

    @Test(timeout = 10000) public void testDiseqVarSets1() throws OccursException, RigidException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();

        assertPresent(phi.disunify(Set.Immutable.of(a), b, B.newList(a)));
        assertEquals(CapsuleUtil.toSet(b), phi.varSet());
        assertEquals(CapsuleUtil.toSet(), phi.domainSet());
        assertEquals(CapsuleUtil.toSet(b), phi.rangeSet());
    }

    @Test(timeout = 10000) public void testDiseqVarSets2() throws OccursException, RigidException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();

        assertPresent(phi.disunify(Set.Immutable.of(), B.newTuple(a, b), B.newTuple(c, d)));
        assertEquals(CapsuleUtil.toSet(a, b, c, d), phi.varSet());
        assertEquals(CapsuleUtil.toSet(), phi.domainSet());
        assertEquals(CapsuleUtil.toSet(a, b, c, d), phi.rangeSet());

        assertPresent(phi.unify(a, B.newInt(1)));
        assertEquals(CapsuleUtil.toSet(a, b, c, d), phi.varSet());
        assertEquals(CapsuleUtil.toSet(a), phi.domainSet());
        assertEquals(CapsuleUtil.toSet(b, c, d), phi.rangeSet());

        assertPresent(phi.unify(c, B.newInt(2)));
        assertEquals(CapsuleUtil.toSet(a, c), phi.varSet());
        assertEquals(CapsuleUtil.toSet(a, c), phi.domainSet());
        assertEquals(CapsuleUtil.toSet(), phi.rangeSet());
    }

    @Test(timeout = 10000) public void testDiseqVarSets3() throws OccursException, RigidException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();

        assertPresent(phi.disunify(B.newTuple(a, b), B.newTuple(c, d)));
        assertEquals(CapsuleUtil.toSet(a, b, c, d), phi.varSet());
        assertEquals(CapsuleUtil.toSet(), phi.domainSet());
        assertEquals(CapsuleUtil.toSet(a, b, c, d), phi.rangeSet());

        assertPresent(phi.unify(a, c));
        assertEquals(CapsuleUtil.toSet(a, b, c, d), phi.varSet());
        assertTrue(phi.rangeSet().contains(b));
        assertTrue(phi.rangeSet().contains(d));

        assertPresent(phi.disunify(B.newTuple(b, u), B.newTuple(d, v))); // implied
        assertFalse(phi.rangeSet().contains(u));
        assertFalse(phi.rangeSet().contains(v));
    }

    @Test(timeout = 10000) public void testDiseqVarSets4() throws OccursException, RigidException {
        IUniDisunifier.Transient phi = PersistentUniDisunifier.Immutable.of().melt();

        assertPresent(phi.disunify(a, b));
        assertEquals(CapsuleUtil.toSet(a, b), phi.varSet());
        assertEquals(CapsuleUtil.toSet(), phi.domainSet());
        assertEquals(CapsuleUtil.toSet(a, b), phi.rangeSet());

        assertPresent(phi.unify(a, c));
        assertEquals(CapsuleUtil.toSet(a, b, c), phi.varSet());
        assertXor(phi.domainSet().contains(a) && phi.rangeSet().contains(c),
                phi.rangeSet().contains(a) && phi.domainSet().contains(c));
        assertContains(b, phi.rangeSet());

        phi.remove(c);
        assertEquals(CapsuleUtil.toSet(a, b), phi.varSet());
        assertEquals(CapsuleUtil.toSet(), phi.domainSet());
        assertEquals(CapsuleUtil.toSet(a, b), phi.rangeSet());
    }

}