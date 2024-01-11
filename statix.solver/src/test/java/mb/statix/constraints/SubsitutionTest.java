package mb.statix.constraints;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermPattern.P;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.metaborg.util.collection.ImList;
import org.metaborg.util.collection.Sets;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.substitution.PersistentSubstitution;
import mb.statix.solver.IConstraint;
import mb.statix.spec.Rule;
import mb.statix.spec.RuleName;

public class SubsitutionTest {

    @Test public void testOneLevel0() {
        final ISubstitution.Immutable subst = PersistentSubstitution.Immutable.of(a, f());
        final IConstraint p = new CExists(list(b), new CEqual(b, a));
        final IConstraint q = p.apply(subst);
        assertInvariant(p, subst, q);
    }

    @Test public void testOneLevel1() {
        final ISubstitution.Immutable subst = PersistentSubstitution.Immutable.of(a, f(a));
        final IConstraint p = new CExists(list(b), new CEqual(b, a));
        final IConstraint q = p.apply(subst);
        assertInvariant(p, subst, q);
    }

    @Test public void testOneLevel2() {
        final ISubstitution.Immutable subst = PersistentSubstitution.Immutable.of(a, g(b));
        final IConstraint p = new CExists(list(b), new CEqual(b, a));
        final IConstraint q = p.apply(subst);
        assertInvariant(p, subst, q);
    }

    @Test public void testOneLevel3() {
        final ISubstitution.Immutable subst = PersistentSubstitution.Immutable.of(a, g(c));
        final IConstraint p = new CExists(list(b), new CEqual(b, a));
        final IConstraint q = p.apply(subst);
        assertInvariant(p, subst, q);
    }

    @Test public void testOneLevel4() {
        final ISubstitution.Immutable subst = PersistentSubstitution.Immutable.of(a, g(c)).put(b, f(a));
        final IConstraint p = new CExists(list(b), new CEqual(b, a));
        final IConstraint q = p.apply(subst);
        assertInvariant(p, subst, q);
    }


    @Test public void testTwoLevels0() {
        final ISubstitution.Immutable subst = PersistentSubstitution.Immutable.of(a, f());
        final IConstraint p = new CExists(list(b), new CExists(list(c), new CEqual(b, a)));
        final IConstraint q = p.apply(subst);
        assertInvariant(p, subst, q);
    }

    @Test public void testTwoLevels1() {
        final ISubstitution.Immutable subst = PersistentSubstitution.Immutable.of(a, f(a));
        final IConstraint p = new CExists(list(b), new CExists(list(c), new CEqual(b, a)));
        final IConstraint q = p.apply(subst);
        assertInvariant(p, subst, q);
    }

    @Test public void testTwoLevels2() {
        final ISubstitution.Immutable subst = PersistentSubstitution.Immutable.of(a, g(b));
        final IConstraint p = new CExists(list(b), new CExists(list(c), new CEqual(b, a)));
        final IConstraint q = p.apply(subst);
        assertInvariant(p, subst, q);
    }

    @Test public void testTwoLevels3() {
        final ISubstitution.Immutable subst = PersistentSubstitution.Immutable.of(a, g(c));
        final IConstraint p = new CExists(list(b), new CExists(list(c), new CEqual(b, a)));
        final IConstraint q = p.apply(subst);
        assertInvariant(p, subst, q);
    }


    @Test public void testTwoLevelsOneVar0() {
        final ISubstitution.Immutable subst = PersistentSubstitution.Immutable.of(a, f());
        final IConstraint p = new CExists(list(b), new CExists(list(c), new CEqual(b, a)));
        final IConstraint q = p.apply(subst);
        assertInvariant(p, subst, q);
    }

    @Test public void testTwoLevelsOneVar1() {
        final ISubstitution.Immutable subst = PersistentSubstitution.Immutable.of(a, f(a));
        final IConstraint p = new CExists(list(b), new CExists(list(c), new CEqual(b, a)));
        final IConstraint q = p.apply(subst);
        assertInvariant(p, subst, q);
    }

    @Test public void testTwoLevelsOneVar2() {
        final ISubstitution.Immutable subst = PersistentSubstitution.Immutable.of(a, g(b));
        final IConstraint p = new CExists(list(b), new CExists(list(c), new CEqual(b, a)));
        final IConstraint q = p.apply(subst);
        assertInvariant(p, subst, q);
    }

    @Test public void testTwoLevelsOneVar3() {
        final ISubstitution.Immutable subst = PersistentSubstitution.Immutable.of(a, g(c));
        final IConstraint p = new CExists(list(b), new CExists(list(c), new CEqual(b, a)));
        final IConstraint q = p.apply(subst);
        assertInvariant(p, subst, q);
    }


    @Test public void testRule0() {
        final ISubstitution.Immutable subst = PersistentSubstitution.Immutable.of(a, f());
        final Rule p = Rule.of("", RuleName.empty(), list(P.newVar(b)), new CExists(list(c), new CEqual(b, a)));
        final Rule q = p.apply(subst);
        assertInvariant(p, subst, q);
    }

    @Test public void testRule1() {
        final ISubstitution.Immutable subst = PersistentSubstitution.Immutable.of(a, f(a));
        final Rule p = Rule.of("", RuleName.empty(), list(P.newVar(b)), new CExists(list(c), new CEqual(b, a)));
        final Rule q = p.apply(subst);
        assertInvariant(p, subst, q);
    }

    @Test public void testRule2() {
        final ISubstitution.Immutable subst = PersistentSubstitution.Immutable.of(a, g(b));
        final Rule p = Rule.of("", RuleName.empty(), list(P.newVar(b)), new CExists(list(c), new CEqual(b, a)));
        final Rule q = p.apply(subst);
        assertInvariant(p, subst, q);
    }

    @Test public void testRule3() {
        final ISubstitution.Immutable subst = PersistentSubstitution.Immutable.of(a, g(c));
        final Rule p = Rule.of("", RuleName.empty(), list(P.newVar(b)), new CExists(list(c), new CEqual(b, a)));
        final Rule q = p.apply(subst);
        assertInvariant(p, subst, q);
    }


    ////////////////////////////////////////////////////////////////////////////

    private static final ITermVar a = B.newVar("", "a");
    private static final ITermVar b = B.newVar("", "b");
    private static final ITermVar c = B.newVar("", "c");

    private ITerm f(ITerm... args) {
        return B.newAppl("f", args);
    }

    private ITerm g(ITerm... args) {
        return B.newAppl("g", args);
    }

    private ITerm h(ITerm... args) {
        return B.newAppl("h", args);
    }

    private <X> ImList.Immutable<X> list(X... xs) {
        return ImList.Immutable.of(xs);
    }

    ////////////////////////////////////////////////////////////////////////////

    private void assertInvariant(final IConstraint p, final ISubstitution.Immutable subst, final IConstraint q) {
        final Set<ITermVar> expected =
                Sets.union(Sets.difference(p.freeVars(), subst.domainSet()), subst.retainAll(p.freeVars()).rangeSet());
        final Set<ITermVar> actual = q.freeVars();
        if(!expected.equals(actual)) {
            throw new AssertionError(
                    "Expected " + expected + ", got " + actual + ".\nSubstituted " + p + " " + subst + " to " + q);
        }
    }

    private void assertInvariant(final Rule p, final ISubstitution.Immutable subst, final Rule q) {
        final Set<ITermVar> expected =
                Sets.union(Sets.difference(p.freeVars(), subst.domainSet()), subst.retainAll(p.freeVars()).rangeSet());
        final Set<ITermVar> actual = q.freeVars();
        if(!expected.equals(actual)) {
            throw new AssertionError(
                    "Expected " + expected + ", got " + actual + ".\nSubstituted " + p + " " + subst + " to " + q);
        }
    }


}
