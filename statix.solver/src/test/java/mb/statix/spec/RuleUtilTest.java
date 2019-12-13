package mb.statix.spec;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermPattern.P;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.matching.Pattern;
import mb.statix.constraints.CConj;
import mb.statix.constraints.CEqual;
import mb.statix.constraints.CExists;
import mb.statix.constraints.CTrue;
import mb.statix.constraints.CUser;
import mb.statix.constraints.Constraints;
import mb.statix.solver.IConstraint;

public class RuleUtilTest {

    private final static ILogger logger = LoggerUtils.logger(RuleUtilTest.class);

    public static void main(String[] args) {
        testUnorderedRules1();
        testUnorderedRules2();
        testInlineRules1();
        testInlineRules2();
        testInlineRules3();
    }

    private static void testUnorderedRules1() {
        final ITermVar v1 = B.newVar("", "p-1");
        final ITermVar v2 = B.newVar("", "p-2");
        final Pattern p1 = P.newVar(v1);
        final IConstraint body = Constraints.exists(Arrays.asList(v1), new CEqual(v1, v2));
        // @formatter:off
        final List<Rule> rules = Arrays.asList(
          Rule.of("c", Arrays.asList(P.newInt(1), P.newWld()), body)
        , Rule.of("c", Arrays.asList(p1, P.newAs(v2, P.newListTail(Arrays.asList(P.newWld()), P.newWld()))), body)
        , Rule.of("c", Arrays.asList(p1, P.newAs(v2, P.newInt(1))), body)
        , Rule.of("c", Arrays.asList(p1, P.newAs(v2, P.newWld())), body)
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
          Rule.of("c", Arrays.asList(p1, P.newAs(v1, P.newInt(1))), body)
        , Rule.of("c", Arrays.asList(p1, p2), body)
        );
        testUnorderedRules(rules);
    }

    private static void testUnorderedRules(List<Rule> rules) {
        logger.info("Ordered rules:");
        rules.forEach(r -> logger.info(" * {}", r));

        // @formatter:on
        final Set<Rule> newRules = RuleUtil.makeUnordered(rules);
        logger.info("Unordered rules:");
        newRules.forEach(r -> logger.info(" * {}", r));
    }

    private static void testInlineRules1() {
        final Pattern p1 = P.newVar("p1");
        final Pattern p2 = P.newVar("p2");
        final ITermVar v1 = B.newVar("", "p1");
        final ITermVar v2 = B.newVar("", "p2");
        final Rule into = Rule.of("c", Arrays.asList(p1, P.newWld()),
                new CConj(new CTrue(), new CExists(Arrays.asList(v2), new CUser("c", Arrays.asList(v1, v2)))));
        final Rule rule = Rule.of("c", Arrays.asList(p1, p2), new CEqual(v1, v2));
        testInlineRules(rule, 0, into);
    }

    private static void testInlineRules2() {
        final Pattern p1 = P.newVar("p1");
        final Pattern p2 = P.newVar("p2");
        final ITermVar v1 = B.newVar("", "p1");
        final ITermVar v2 = B.newVar("", "p2");
        final Rule into = Rule.of("c", Arrays.asList(p1, P.newWld()),
                new CConj(new CTrue(), new CExists(Arrays.asList(v2), new CUser("c", Arrays.asList(B.newList(), v2)))));
        final Rule rule = Rule.of("c", Arrays.asList(P.newInt(42), p2), new CEqual(v1, v2));
        testInlineRules(rule, 0, into);
    }

    private static void testInlineRules3() {
        final Pattern p1 = P.newVar("p1");
        final Pattern p2 = P.newVar("p2");
        final ITermVar v1 = B.newVar("", "p1");
        final ITermVar v2 = B.newVar("", "p2");
        final Rule into = Rule.of("c", Arrays.asList(p1, P.newWld()),
                new CConj(new CTrue(), new CExists(Arrays.asList(v2), new CUser("c", Arrays.asList(v1, B.newList())))));
        final Rule rule = Rule.of("c", Arrays.asList(P.newInt(42), p2), new CEqual(v1, v2));
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
        } else {
            logger.info("failed");
        }
    }

}