package mb.statix.spec;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermPattern.P;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.matching.Pattern;
import mb.statix.constraints.CEqual;
import mb.statix.constraints.CTrue;
import mb.statix.constraints.Constraints;
import mb.statix.solver.IConstraint;

public class RuleUtilTest {

    private final static ILogger logger = LoggerUtils.logger(RuleUtilTest.class);

    public static void main(String[] args) {
        testRules1();
        testRules2();
    }

    private static void testRules1() {
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
        testRules(rules);
    }

    private static void testRules2() {
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
        testRules(rules);
    }

    private static void testRules(List<Rule> rules) {
        logger.info("Ordered rules:");
        rules.forEach(r -> logger.info(" * {}", r));

        // @formatter:on
        final Set<Rule> newRules = RuleUtil.makeUnordered(rules);
        logger.info("Unordered rules:");
        newRules.forEach(r -> logger.info(" * {}", r));
    }

}