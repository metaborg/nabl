package mb.statix.spec;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermPattern.P;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.collection.ImList;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import mb.nabl2.terms.IStringTerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.matching.Pattern;
import mb.nabl2.terms.unification.u.IUnifier;
import mb.nabl2.terms.unification.u.PersistentUnifier;
import mb.scopegraph.oopsla20.reference.EdgeOrData;
import mb.statix.constraints.CConj;
import mb.statix.constraints.CEqual;
import mb.statix.constraints.CExists;
import mb.statix.constraints.CTrue;
import mb.statix.constraints.CUser;
import mb.statix.constraints.Constraints;
import mb.statix.solver.IConstraint;
import mb.statix.solver.completeness.Completeness;
import mb.statix.solver.completeness.ICompleteness;
import mb.statix.spec.ApplyMode.Safety;

public class RuleUtilTest {

    private final static ILogger logger = LoggerUtils.logger(RuleUtilTest.class);

    public static void main(String[] args) {
        testUnorderedRules0();
        testUnorderedRules1();
        testUnorderedRules2();
        testUnorderedRules3();
        testInlineRules1();
        testInlineRules2();
        testInlineRules3();
        testInlineRules4();
        testTransforms();
        testClose1();
        testClose2();
    }

    private static void testUnorderedRules0() {
        final IConstraint body = new CTrue();
        // @formatter:off
        final List<Rule> rules = Arrays.asList(
          Rule.of("c", RuleName.empty(), ImList.Immutable.of(P.newAppl("f", P.newInt(1))), body)
        , Rule.of("c", RuleName.empty(), ImList.Immutable.of(P.newAppl("f", P.newWld())), body)
        , Rule.of("c", RuleName.empty(), ImList.Immutable.of(P.newWld()), body)
        );
        testUnorderedRules(rules);
    }

    private static void testUnorderedRules1() {
        final ITermVar v1 = B.newVar("", "x");
        final ITermVar v2 = B.newVar("", "x");
        final Pattern p1 = P.newVar(v1);
        final IConstraint body = Constraints.exists(Arrays.asList(v1), new CEqual(v1, v2));
        // @formatter:off
        final List<Rule> rules = Arrays.asList(
          Rule.of("c", RuleName.empty(), ImList.Immutable.of(P.newInt(1), P.newWld()), body)
        , Rule.of("c", RuleName.empty(), ImList.Immutable.of(p1, P.newAs(v2, P.newListTail(Arrays.asList(P.newWld()), P.newWld()))), body)
        , Rule.of("c", RuleName.empty(), ImList.Immutable.of(p1, P.newAs(v2, P.newInt(1))), body)
        , Rule.of("c", RuleName.empty(), ImList.Immutable.of(p1, P.newAs(v2, P.newWld())), body)
        );
        testUnorderedRules(rules);
    }

    private static void testUnorderedRules2() {
        final ITermVar v1 = B.newVar("", "p-1");
        final ITermVar v2 = B.newVar("", "p-2");
        final Pattern p1 = P.newVar(v1);
        final Pattern p2 = P.newVar(v2);
        final IConstraint body = new CTrue();
        // @formatter:off
        final List<Rule> rules = Arrays.asList(
          Rule.of("c", RuleName.empty(), ImList.Immutable.of(p1, P.newAs(v1, P.newInt(1))), body)
        , Rule.of("c", RuleName.empty(), ImList.Immutable.of(p1, p2), body)
        );
        // @formatter:on
        testUnorderedRules(rules);
    }

    private static void testUnorderedRules3() {
        final ITermVar s = B.newVar("", "s");
        final ITermVar e1 = B.newVar("", "e1");
        final ITermVar e2 = B.newVar("", "e2");
        final Pattern ps = P.newVar(s);
        final Pattern p1 = P.newVar(e1);
        final Pattern p2 = P.newVar(e2);
        final IConstraint body = new CTrue();

        // criticalEdges
        final ICompleteness.Transient _completeness = Completeness.Transient.of();
        _completeness.add(s, EdgeOrData.edge(B.newString("lbl")), PersistentUnifier.Immutable.of());
        final ICompleteness.Immutable completeness = _completeness.freeze();

        // @formatter:off
        final List<Rule> rules = Arrays.asList(
          Rule.of("c", RuleName.empty(), ImList.Immutable.of(ps, P.newAppl("Add", p1, p2)), body).withBodyCriticalEdges(completeness)
        , Rule.of("c", RuleName.empty(), ImList.Immutable.of(ps, P.newAppl("Var", p1)), body).withBodyCriticalEdges(completeness)
        , Rule.of("c", RuleName.empty(), ImList.Immutable.of(ps, P.newWld()), body).withBodyCriticalEdges(completeness)
        );
        // @formatter:on
        testUnorderedRules(rules);
    }

    private static void testUnorderedRules(List<Rule> rules) {
        logger.info("Ordered rules:");
        rules.forEach(r -> logger.info(" * {}", r));

        Collection<java.util.Set<Rule>> newRules = RuleSet.of(rules).getAllOrderIndependentRules().values();
        logger.info("Unordered rules:");
        newRules.forEach(r -> logger.info(" * {}", r));
    }


    private static void testInlineRules1() {
        final Pattern p1 = P.newVar("p1");
        final Pattern p2 = P.newVar("p2");
        final ITermVar v1 = B.newVar("", "p1");
        final ITermVar v2 = B.newVar("", "p2");
        final Rule into = Rule.of("c", RuleName.empty(), ImList.Immutable.of(p1, P.newWld()),
                new CConj(new CTrue(), new CExists(Arrays.asList(v2), new CUser("c", Arrays.asList(v1, v2)))));
        final Rule rule = Rule.of("c", RuleName.empty(), ImList.Immutable.of(p1, p2), new CEqual(v1, v2));
        testInlineRules(rule, 0, into);
    }

    private static void testInlineRules2() {
        final Pattern p1 = P.newVar("p1");
        final Pattern p2 = P.newVar("p2");
        final ITermVar v1 = B.newVar("", "p1");
        final ITermVar v2 = B.newVar("", "p2");
        final Rule into = Rule.of("c", RuleName.empty(), ImList.Immutable.of(p1, P.newWld()),
                new CConj(new CTrue(), new CExists(Arrays.asList(v2), new CUser("c", Arrays.asList(B.newList(), v2)))));
        final Rule rule = Rule.of("c", RuleName.empty(), ImList.Immutable.of(P.newInt(42), p2), new CEqual(v1, v2));
        testInlineRules(rule, 0, into);
    }

    private static void testInlineRules3() {
        final Pattern p1 = P.newVar("p1");
        final Pattern p2 = P.newVar("p2");
        final ITermVar v1 = B.newVar("", "p1");
        final ITermVar v2 = B.newVar("", "p2");
        final Rule into = Rule.of("c", RuleName.empty(), ImList.Immutable.of(p1, P.newWld()),
                new CConj(new CTrue(), new CExists(Arrays.asList(v2), new CUser("c", Arrays.asList(v1, B.newList())))));
        final Rule rule = Rule.of("c", RuleName.empty(), ImList.Immutable.of(P.newInt(42), p2), new CEqual(v1, v2));
        testInlineRules(rule, 0, into);
    }

    private static void testInlineRules4() {
        final Pattern p1 = P.newVar("p1");
        final Pattern p2 = P.newVar("p2");
        final ITermVar v1 = B.newVar("", "p1");
        final ITermVar v2 = B.newVar("", "p2");
        final Rule into = Rule.of("c", RuleName.empty(), ImList.Immutable.of(p1, P.newWld()),
                new CConj(new CTrue(), new CExists(Arrays.asList(v2), new CUser("c", Arrays.asList(v1, B.newList())))));
        final Rule rule = Rule.of("c", RuleName.empty(), ImList.Immutable.of(P.newInt(42), p2), new CTrue());
        testInlineRules(rule, 0, into);
    }

    private static void testInlineRules(Rule rule, int i, Rule into) {
        logger.info("Inline");
        logger.info("* {}", rule);
        logger.info("into premise {} of", i);
        logger.info("* {}", into);
        final Optional<Rule> r = RuleUtil.inline(rule, i, into);
        if(r.isPresent()) {
            logger.info("gives");
            logger.info("* {}", r.get());
            final Rule rs = RuleUtil.hoist(r.get());
            logger.info("which simplifies to");
            logger.info("* {}", rs);
        } else {
            logger.info("failed");
        }
    }


    private static void testTransforms() {
        final ITermVar x = B.newVar("", "x");
        final ITermVar y = B.newVar("", "y");
        final ITermVar z = B.newVar("", "z");
        final ITermVar Ts = B.newVar("", "Ts");
        final ITermVar Us = B.newVar("", "Us");
        final IStringTerm A = B.newString("A");
        final ITermVar wld = B.newVar("", "_1");

        // @formatter:off
        final List<Rule> rules = Arrays.asList(
          Rule.of("", RuleName.empty(), ImList.Immutable.of(P.newVar(x)), new CEqual(x, A))
        , Rule.of("", RuleName.empty(), ImList.Immutable.of(P.newVar(x)), new CEqual(x, y))

        , Rule.of("", RuleName.empty(), ImList.Immutable.of(P.newVar(x)), new CExists(Arrays.asList(wld), new CEqual(x, B.newTuple(A, wld))))
        , Rule.of("", RuleName.empty(), ImList.Immutable.of(P.newVar(x)), new CExists(Arrays.asList(wld), new CEqual(x, B.newTuple(y, wld))))

        , Rule.of("", RuleName.empty(), ImList.Immutable.of(P.newAppl("Id", P.newVar(x))), new CEqual(x, A))
        , Rule.of("", RuleName.empty(), ImList.Immutable.of(P.newAppl("Id", P.newVar(x))), new CEqual(x, y))
        , Rule.of("", RuleName.empty(), ImList.Immutable.of(P.newAs(z, P.newAppl("Id", P.newVar(x)))), new CEqual(z, y))

        , Rule.of("", RuleName.empty(), ImList.Immutable.of(P.newVar(x)), new CEqual(x, B.newAppl("Id", A)))
        , Rule.of("", RuleName.empty(), ImList.Immutable.of(P.newVar(x)), new CEqual(x, B.newAppl("Id", y)))

        , Rule.of("", RuleName.empty(), ImList.Immutable.of(P.newVar(x)), new CEqual(y, B.newAppl("Id", A)))
        , Rule.of("", RuleName.empty(), ImList.Immutable.of(P.newVar(x)), new CEqual(y, B.newAppl("Id", x)))
        , Rule.of("", RuleName.empty(), ImList.Immutable.of(P.newVar(x)), new CExists(Arrays.asList(z), new CEqual(y, B.newAppl("Id", z))))

        , Rule.of("", RuleName.empty(), ImList.Immutable.of(P.newTuple(P.newVar(x), P.newVar(Ts))), new CConj(new CEqual(x, A), new CUser("p", Arrays.asList(Us, Ts))))
        , Rule.of("", RuleName.empty(), ImList.Immutable.of(P.newTuple(P.newVar(x), P.newVar(Ts))), new CConj(new CEqual(x, y), new CUser("p", Arrays.asList(Us, Ts))))

        , Rule.of("", RuleName.empty(), ImList.Immutable.of(P.newAs(z, P.newAppl("Id", P.newVar(x)))), new CEqual(z, B.newAppl("ID", y)))
        , Rule.of("", RuleName.empty(), ImList.Immutable.of(P.newVar(x), P.newVar(x)), new CTrue())

        , Rule.of("", RuleName.empty(), ImList.Immutable.of(P.newAs(z, P.newAppl("Id", P.newVar(x)))), new CExists(Arrays.asList(y),
                new CConj(new CUser("p", Arrays.asList(z, y)), new CExists(Arrays.asList(z),
                        new CEqual(y, B.newAppl("f", x, z))))))
        );
        // @formatter:on

        testTransformRules(rules);
    }

    private static void testTransformRules(List<Rule> rules) {
        for(Rule r : rules) {
            logger.info("Transform {}", r);
            //  testTransformRuleResult("hoist", r, RuleUtil.hoist(r));
            testTransformRuleResult("inlineHead", r, RuleUtil.instantiateHeadPatterns(r));
        }
    }

    private static void testTransformRuleResult(String name, Rule r, Rule s) {
        logger.info("* {} to {}", name, s);
        if(!Set.Immutable.subtract(s.freeVars(), r.freeVars()).isEmpty()) {
            logger.error("  !! {} introduced new free variables", name);
        }
    }


    private static void testClose1() {
        ITermVar x = B.newVar("", "x");
        ITermVar y = B.newVar("", "y");
        IUnifier.Immutable u = PersistentUnifier.Immutable.of(true, CapsuleUtil.immutableMap(), CapsuleUtil.immutableMap(),
                Map.Immutable.of(y, B.newAppl("Id", x)));
        Rule r = Rule.of("", RuleName.empty(), ImList.Immutable.of(), new CExists(Arrays.asList(x), new CEqual(y, B.newAppl("Id", x))));
        Rule s = RuleUtil.closeInUnifier(r, u, Safety.SAFE);
        logger.info("Close {} in {}", r, u);
        logger.info("  to {}", s);
    }

    private static void testClose2() {
        ITermVar x = B.newVar("", "x");
        ITermVar y = B.newVar("", "y");
        IUnifier.Immutable u = PersistentUnifier.Immutable.of(true, CapsuleUtil.immutableMap(), CapsuleUtil.immutableMap(),
                Map.Immutable.of(y, B.newAppl("Id", x)));
        Rule r = Rule.of("", RuleName.empty(), ImList.Immutable.of(P.newVar(x)), new CEqual(y, B.newAppl("Id", x)));
        Rule s = RuleUtil.closeInUnifier(r, u, Safety.SAFE);
        logger.info("Close {} in {}", r, u);
        logger.info("  to {}", s);
    }

}
