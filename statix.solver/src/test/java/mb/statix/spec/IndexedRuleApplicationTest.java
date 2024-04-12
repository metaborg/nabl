package mb.statix.spec;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermPattern.P;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.Arrays;
import java.util.Optional;

import org.junit.Test;
import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.collection.ImList;

import io.usethesource.capsule.SetMultimap;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.statix.constraints.CEqual;
import mb.statix.constraints.CExists;
import mb.statix.constraints.CFalse;
import mb.statix.constraints.CTrue;
import mb.statix.constraints.CUser;
import mb.statix.solver.Delay;

public class IndexedRuleApplicationTest {

    private static final ITermVar x = B.newVar("", "x");
    private static final ITermVar y = B.newVar("", "y");
    private static final ITermVar z = B.newVar("", "z");

    private static final ITerm foo = B.newString("foo");
    private static final ITerm bar = B.newString("bar");
    private static final ITerm baz = B.newString("baz");

    // @formatter:off
    final RuleSet ruleSet = RuleSet.of(Arrays.asList(
        Rule.of("p", RuleName.empty(), ImList.Immutable.of(P.newVar(x), P.newVar(x)), new CTrue()),
        Rule.of("p", RuleName.empty(), ImList.Immutable.of(P.newWld(), P.newWld()), new CFalse()),
        Rule.of("q", RuleName.empty(), ImList.Immutable.of(P.newInt(1)), new CFalse()),
        Rule.of("q", RuleName.empty(), ImList.Immutable.of(P.newWld()), new CTrue())
    ));
    final Spec spec = Spec.of(ruleSet, CapsuleUtil.immutableSet(), CapsuleUtil.immutableSet(), SetMultimap.Immutable.of());
    // @formatter:on

    @Test public void testTrue() throws Delay, InterruptedException {
        final Rule rule = Rule.of("", RuleName.empty(), ImList.Immutable.of(P.newVar(x)), new CTrue());
        IndexedRuleApplication ira = assertPresent(IndexedRuleApplication.of(spec, rule));
        assertEquals(assertPresent(ira.applyIndex(foo)), assertPresent(ira.applyIndex(bar)));
    }

    @Test public void testFalse() throws Delay, InterruptedException {
        final Rule rule = Rule.of("", RuleName.empty(), ImList.Immutable.of(P.newVar(x)), new CFalse());
        assertAbsent(IndexedRuleApplication.of(spec, rule));
    }

    @Test public void testIdentity() throws Delay, InterruptedException {
        final Rule rule = Rule.of("", RuleName.empty(), ImList.Immutable.of(P.newVar(x)), new CEqual(x, y));
        IndexedRuleApplication ira = assertPresent(IndexedRuleApplication.of(spec, rule));
        assertNotEquals(assertPresent(ira.applyIndex(foo)), assertPresent(ira.applyIndex(baz)));
    }

    @Test public void testPartialInPattern() throws Delay, InterruptedException {
        final Rule rule = Rule.of("", RuleName.empty(), ImList.Immutable.of(P.newTuple(P.newVar(x), P.newWld())), new CEqual(x, y));
        IndexedRuleApplication ira = assertPresent(IndexedRuleApplication.of(spec, rule));
        assertEquals(assertPresent(ira.applyIndex(foo, bar)), assertPresent(ira.applyIndex(foo, baz)));
        assertNotEquals(assertPresent(ira.applyIndex(bar, foo)), assertPresent(ira.applyIndex(baz, foo)));
    }

    @Test public void testPartialInBody() throws Delay, InterruptedException {
        final Rule rule =
                Rule.of("", RuleName.empty(), ImList.Immutable.of(P.newVar(x)), new CExists(Arrays.asList(z), new CEqual(x, B.newTuple(y, z))));
        IndexedRuleApplication ira = assertPresent(IndexedRuleApplication.of(spec, rule));
        assertEquals(assertPresent(ira.applyIndex(foo, bar)), assertPresent(ira.applyIndex(foo, baz)));
        assertNotEquals(assertPresent(ira.applyIndex(bar, foo)), assertPresent(ira.applyIndex(baz, foo)));
    }

    @Test public void testEquals() throws Delay, InterruptedException {
        final Rule rule = Rule.of("", RuleName.empty(), ImList.Immutable.of(P.newTuple(P.newVar(x), P.newVar(x))), new CTrue());
        IndexedRuleApplication ira = assertPresent(IndexedRuleApplication.of(spec, rule));
        assertAbsent(ira.applyIndex(bar, baz));
        assertEquals(assertPresent(ira.applyIndex(foo, foo)), assertPresent(ira.applyIndex(baz, baz)));
    }

    @Test public void testPred() throws Delay, InterruptedException {
        final Rule rule = Rule.of("", RuleName.empty(), ImList.Immutable.of(P.newVar(x)), new CUser("q", Arrays.asList(x)));
        IndexedRuleApplication ira = assertPresent(IndexedRuleApplication.of(spec, rule));
        assertAbsent(ira.applyIndex(B.newInt(1)));
        assertEquals(assertPresent(ira.applyIndex(B.newInt(0))), assertPresent(ira.applyIndex(B.newInt(2))));
    }

    @Test(expected = Delay.class) public void testPredWithFreeArgument() throws Delay, InterruptedException {
        final Rule rule = Rule.of("", RuleName.empty(), ImList.Immutable.of(P.newVar(x)), new CUser("p", Arrays.asList(x, y)));
        IndexedRuleApplication.of(spec, rule);
    }

    private <T> T assertPresent(Optional<T> opt) {
        if(!opt.isPresent()) {
            throw new AssertionError("Expected result, got empty.");
        }
        return opt.get();
    }

    private <T> void assertAbsent(Optional<T> opt) {
        if(opt.isPresent()) {
            throw new AssertionError("Expected empty, got result: " + opt.get());
        }
    }

}
