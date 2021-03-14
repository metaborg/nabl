package mb.nabl2.terms.unification.ud;

import static mb.nabl2.terms.build.TermBuild.B;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITermVar;

public class DiseqTest {

    final ITermVar a = B.newVar("", "a");
    final ITermVar b = B.newVar("", "b");
    final ITermVar c = B.newVar("", "c");
    final ITermVar d = B.newVar("", "d");
    final ITermVar e = B.newVar("", "e");
    final ITermVar f = B.newVar("", "f");

    final ITermVar x = B.newVar("", "x");
    final ITermVar y = B.newVar("", "y");

    @Test public void test01() {
        final Diseq deq1 = Diseq.of(Set.Immutable.of(), a, b).get();
        final Diseq deq2 = Diseq.of(Set.Immutable.of(), a, b).get();
        assertTrue(deq1.implies(deq2));
        assertTrue(deq2.implies(deq1));
    }

    @Test public void test02() {
        final Diseq deq1 = Diseq.of(Set.Immutable.of(), b, a).get();
        final Diseq deq2 = Diseq.of(Set.Immutable.of(), a, b).get();
        assertTrue(deq1.implies(deq2));
        assertTrue(deq2.implies(deq1));
    }

    @Test public void test03() {
        final Diseq deq1 = Diseq.of(Set.Immutable.of(), B.newTuple(a, b), B.newTuple(c, d)).get();
        final Diseq deq2 = Diseq.of(Set.Immutable.of(), a, c).get();
        assertFalse(deq1.implies(deq2));
        assertTrue(deq2.implies(deq1));
    }

    @Test public void test04() {
        final Diseq deq1 = Diseq.of(Set.Immutable.of(x), a, B.newTuple(x, b)).get();
        final Diseq deq2 = Diseq.of(Set.Immutable.of(y), a, B.newTuple(y, b)).get();
        assertTrue(deq1.implies(deq2));
        assertTrue(deq2.implies(deq1));
    }

    @Test public void test05() {
        final Diseq deq1 = Diseq.of(Set.Immutable.of(x), a, B.newTuple(x, c)).get();
        final Diseq deq2 = Diseq.of(Set.Immutable.of(), a, B.newTuple(b, c)).get();
        assertTrue(deq1.implies(deq2));
        assertFalse(deq2.implies(deq1));
    }

    @Test public void test06() {
        final Diseq deq1 = Diseq.of(Set.Immutable.of(x, y), a, B.newTuple(x, y)).get();
        final Diseq deq2 = Diseq.of(Set.Immutable.of(x, y), a, B.newTuple(x, y)).get();
        assertTrue(deq1.implies(deq2));
        assertTrue(deq2.implies(deq1));
    }

    @Test public void test07() {
        final Diseq deq1 = Diseq.of(Set.Immutable.of(x), a, B.newTuple(x, b)).get();
        final Diseq deq2 = Diseq.of(Set.Immutable.of(x), a, B.newTuple(b, x)).get();
        assertFalse(deq1.implies(deq2));
        assertFalse(deq2.implies(deq1));
    }

    @Test public void test08() {
        final Diseq deq1 = Diseq.of(Set.Immutable.of(x), a, B.newTuple(x, x)).get();
        final Diseq deq2 = Diseq.of(Set.Immutable.of(x, y), a, B.newTuple(x, y)).get();
        assertFalse(deq1.implies(deq2));
        assertTrue(deq2.implies(deq1));
    }

    @Test public void test09() {
        final Diseq deq1 = Diseq.of(Set.Immutable.of(x), a, B.newTuple(x, x)).get();
        final Diseq deq2 = Diseq.of(Set.Immutable.of(), a, B.newTuple(b, c)).get();
        assertFalse(deq1.implies(deq2));
        assertFalse(deq2.implies(deq1));
    }

    @Test public void test10() {
        final Diseq deq1 = Diseq.of(Set.Immutable.of(x), a, B.newTuple(x, b)).get();
        final Diseq deq2 = Diseq.of(Set.Immutable.of(y), a, B.newTuple(B.newList(y), b)).get();
        assertTrue(deq1.implies(deq2));
        assertFalse(deq2.implies(deq1));
    }

    @Test public void test11() {
        final Diseq deq1 = Diseq.of(Set.Immutable.of(x), a, B.newList(x)).get();
        final Diseq deq2 = Diseq.of(Set.Immutable.of(), a, B.newList(b)).get();
        assertTrue(deq1.implies(deq2));
        assertFalse(deq2.implies(deq1));
    }

    @Test public void test12() {
        final Diseq deq1 = Diseq.of(Set.Immutable.of(x), a, B.newList(x)).get();
        final Diseq deq2 = Diseq.of(Set.Immutable.of(y), a, B.newList(y)).get();
        assertTrue(deq1.implies(deq2));
        assertTrue(deq2.implies(deq1));
    }

    @Test public void test13() {
        final Diseq deq1 = Diseq.of(Set.Immutable.of(x), a, B.newList(x)).get();
        final Diseq deq2 = Diseq.of(Set.Immutable.of(y), b, B.newList(y)).get();
        assertFalse(deq1.implies(deq2));
        assertFalse(deq2.implies(deq1));
    }

}