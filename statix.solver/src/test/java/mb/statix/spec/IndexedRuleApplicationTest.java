package mb.statix.spec;

//import static mb.nabl2.terms.build.TermBuild.B;
//import static mb.nabl2.terms.matching.TermPattern.P;
//import static org.junit.Assert.assertFalse;
//import static org.junit.Assert.assertTrue;
//
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.Optional;
//
//import org.junit.Test;
//
//import com.google.common.collect.HashMultimap;
//
//import mb.statix.constraints.CEqual;
//import mb.statix.constraints.CExists;
//import mb.statix.constraints.CFalse;
//import mb.statix.constraints.CTrue;
//import mb.statix.constraints.CUser;
//
//public class IndexedRuleApplicationTest {
//
//    // @formatter:off
//    final RuleSet ruleSet = RuleSet.of(Arrays.asList(
//        Rule.of("p", Arrays.asList(P.newVar("x"), P.newVar("x")), new CTrue()),
//        Rule.of("p", Arrays.asList(P.newWld(), P.newWld()), new CFalse())
//    ));
//    final Spec spec = Spec.of(ruleSet, Collections.emptySet(), Collections.emptySet(), HashMultimap.create());
//    // @formatter:on
//
//    @Test public void testTrue() throws InterruptedException {
//        final Rule rule = Rule.of("", Arrays.asList(P.newVar("x")), new CTrue());
//        Optional<IndexedRuleApplication> ira = IndexedRuleApplication.of(spec, rule);
//        assertTrue(ira.isPresent());
//    }
//
//    @Test public void testFalse() throws InterruptedException {
//        final Rule rule = Rule.of("", Arrays.asList(P.newVar("x")), new CFalse());
//        Optional<IndexedRuleApplication> ira = IndexedRuleApplication.of(spec, rule);
//        assertFalse(ira.isPresent());
//    }
//
//    @Test public void testIdentity() throws InterruptedException {
//        final Rule rule = Rule.of("", Arrays.asList(P.newVar("x")), new CEqual(B.newVar("", "x"), B.newVar("", "y")));
//        Optional<IndexedRuleApplication> ira = IndexedRuleApplication.of(spec, rule);
//        assertTrue(ira.isPresent());
//    }
//
//    @Test public void testPartialInPattern() throws InterruptedException {
//        final Rule rule = Rule.of("", Arrays.asList(P.newTuple(P.newVar("x"), P.newWld())),
//                new CEqual(B.newVar("", "x"), B.newVar("", "y")));
//        Optional<IndexedRuleApplication> ira = IndexedRuleApplication.of(spec, rule);
//        assertTrue(ira.isPresent());
//    }
//
//    @Test public void testPartialInBody() throws InterruptedException {
//        final Rule rule = Rule.of("", Arrays.asList(P.newVar("x")), new CExists(Arrays.asList(B.newVar("", "_")),
//                new CEqual(B.newVar("", "x"), B.newTuple(B.newVar("", "y"), B.newVar("", "_")))));
//        Optional<IndexedRuleApplication> ira = IndexedRuleApplication.of(spec, rule);
//        assertTrue(ira.isPresent());
//    }
//
//    @Test public void testEquals() throws InterruptedException {
//        final Rule rule = Rule.of("", Arrays.asList(P.newTuple(P.newVar("x"), P.newVar("x"))), new CTrue());
//        Optional<IndexedRuleApplication> ira = IndexedRuleApplication.of(spec, rule);
//        assertTrue(ira.isPresent());
//    }
//
//    @Test public void testPred() throws InterruptedException {
//        final Rule rule = Rule.of("", Arrays.asList(P.newVar("x")),
//                new CUser("p", Arrays.asList(B.newVar("", "x"), B.newVar("", "y"))));
//        Optional<IndexedRuleApplication> ira = IndexedRuleApplication.of(spec, rule);
//        assertTrue(ira.isPresent());
//    }
//
//}