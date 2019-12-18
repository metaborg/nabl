package mb.statix.cli;

import static mb.statix.constraints.Constraints.collectBase;
import static mb.statix.generator.util.StreamUtil.flatMap;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.metaborg.util.functions.Function1;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.SetMultimap;

import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.u.IUnifier;
import mb.statix.constraints.CResolveQuery;
import mb.statix.constraints.CUser;
import mb.statix.generator.EitherSearchState;
import mb.statix.generator.FocusedSearchState;
import mb.statix.generator.SearchState;
import mb.statix.generator.SearchStrategy;
import mb.statix.generator.SearchStrategy.Mode;
import mb.statix.generator.predicate.Any;
import mb.statix.generator.predicate.Match;
import mb.statix.generator.predicate.Not;
import mb.statix.generator.strategy.SearchStrategies;
import mb.statix.generator.util.StreamUtil;
import mb.statix.scopegraph.reference.CriticalEdge;
import mb.statix.solver.IConstraint;
import mb.statix.solver.completeness.CompletenessUtil;
import mb.statix.spec.Rule;
import mb.statix.spec.RuleUtil;
import mb.statix.spec.Spec;

public class Paret {

    private static final ILogger log = LoggerUtils.logger(Paret.class);

    private static final String GEN_RE = ".*!gen_.*";
    private static final String IS_RE = ".*!is_.*";

    private final Spec spec;
    private final SearchStrategies S;

    public Paret(Spec spec) {
        this.spec = spec;
        this.S = new SearchStrategies(spec);
    }

    public SearchStrategy<SearchState, SearchState> search() {
        // @formatter:off
        return S.seq(searchExp())
               .$(S.marker("generateLex"))
               .$(generateLex())
               .$(S.marker("done"))
               .$();
        // @formatter:on
    }

    // inference step

    private SearchStrategy<SearchState, SearchState> inferDelayAndDrop() {
        return S.seq(S.infer()).$(S.delayStuckQueries()).$(S.dropAst()).$();
    }

    // generation of expressions

    private SearchStrategy<SearchState, SearchState> searchExp() {
        // @formatter:off
//        final ListMultimap<String, Rule> fragments = makeFragments(spec);
//        fragments.forEach((n,r) -> {
//            log.info(" * {}", r);
//        });
        return S.repeat(S.limit(10, S.fix(
            S.seq(selectConstraint(1))
            .$(S.match(
               S.limit(3, S.seq(S.limit(5, S.resolve())).$(S.infer()).$()),
               S.limit(1, S.seq(
//                            S.concat(S.limit(5, S.expand(Mode.RND, defaultRuleWeight, ruleWeights)), S.expand(Mode.ENUM, fragments))
                            S.limit(5, S.expand(Mode.RND, defaultRuleWeight, ruleWeights))
                          ).$(S.infer()).$())))
            .$(),
            inferDelayAndDrop(),
            new Match(IS_RE), // everything except is_* constraints should be resolved
            50 // what is a good number here? size / 4?
        )));
        // @formatter:on
    }

    private Function1<IConstraint, List<IConstraint>> collectSubGoals = collectBase(
            c -> c instanceof CUser && ((CUser) c).name().matches(GEN_RE) ? Optional.of(c) : Optional.empty(), false);

    private double hasNoSubGoals(Rule rule, long count) {
        return collectSubGoals.apply(rule.body()).isEmpty() ? 1d : 0d;
    }

    // @formatter:off
    private int defaultRuleWeight = 1;
    private Map<String, Double> ruleWeights = ImmutableMap.<String, Double>builder()
        // TWEAK Disable operations until better inference in the solver
        .put("G-UnOp", 1.0)
        .put("G-BinOp", 1.0)
        // TWEAK Prefer rules that force types
        .put("G-Num", 1.0)
        .put("G-True", 1.0)
        .put("G-False", 1.0)
        .put("G-Nil", 1.0)
        .put("G-List", 1.0)
        .put("G-Fun", 1.0)
        // TWEAK Discourage rules that are 'free'
        .put("G-If", 1.0)
        .put("G-App", 1.0)
        .put("G-Let", 1.0)
        .build();
    // @formatter:on

    public SearchStrategy<SearchState, EitherSearchState<FocusedSearchState<CResolveQuery>, FocusedSearchState<CUser>>>
            selectConstraint(int limit) {
        // @formatter:off
        return S.limit(limit, S.concatAlt(
            // TWEAK Resolve queries first, to improve inference
            S.select(CResolveQuery.class, new Any<>()),
            S.select(CUser.class, /*Paret::predWeights*/ new Match(GEN_RE))
        ));
        // @formatter:on
    }

    private Function1<CUser, Double> predWeights(SearchState state) {
        final IUnifier unifier = state.state().unifier();
        final Set<CriticalEdge> criticalEdges =
                flatMap(state.delays().values().stream(), d -> d.criticalEdges().stream()).collect(Collectors.toSet());
        // @formatter:off
        final Stream<CUser> criticalPreds = StreamUtil.filterInstances(CUser.class, state.constraints().stream())
            .filter(c -> CompletenessUtil.criticalEdges(c, spec, unifier).stream().anyMatch(criticalEdges::contains));
        final Set<ITermVar> criticalVars = flatMap(criticalPreds, c -> flatMap(c.args().stream(), arg -> unifier.getVars(arg).stream()))
            .collect(Collectors.toSet());
        // @formatter:on
        return (c) -> {
            if(!c.name().matches(GEN_RE)) {
                return 0.0;
            }
            if(flatMap(c.args().stream(), arg -> unifier.getVars(arg).stream()).anyMatch(criticalVars::contains)) {
                return 2.0;
            } else {
                return 1.0;
            }
        };
    }

    // generation of id's

    private SearchStrategy<SearchState, SearchState> generateLex() {
        return S.require(S.limit(1, S.fix(expandLex(), S.infer(), new Not<>(new Match(IS_RE)), -1)));
    }

    private SearchStrategy<SearchState, SearchState> expandLex() {
        // @formatter:off
        return S.seq(S.select(CUser.class, new Match(IS_RE)))
               .$(S.limit(1, S.seq(S.expand(Mode.RND, 1, idWeights)).$(S.infer()).$()))
               .$();
        // @formatter:on
    }

    // @formatter:off
    private Map<String, Double> idWeights = ImmutableMap.<String, Double>builder()
        // TWEAK Increase likelihood of duplicate choices, while still providing many identifiers
        .put("[ID-A]", 16.0)
        .put("[ID-B]", 8.0)
        .put("[ID-C]", 8.0)
        .put("[ID-D]", 4.0)
        .put("[ID-E]", 4.0)
        .put("[ID-F]", 4.0)
        .put("[ID-G]", 4.0)
        .put("[ID-H]", 2.0)
        .put("[ID-I]", 2.0)
        .put("[ID-J]", 2.0)
        .put("[ID-K]", 2.0)
        .put("[ID-L]", 2.0)
        .put("[ID-M]", 2.0)
        .put("[ID-N]", 2.0)
        .put("[ID-O]", 2.0)
        .put("[ID-P]", 1.0)
        .put("[ID-Q]", 1.0)
        .put("[ID-R]", 1.0)
        .put("[ID-S]", 1.0)
        .put("[ID-T]", 1.0)
        .put("[ID-U]", 1.0)
        .put("[ID-V]", 1.0)
        .put("[ID-W]", 1.0)
        .put("[ID-X]", 1.0)
        .put("[ID-Y]", 1.0)
        .put("[ID-Z]", 1.0)
        .build();
    // @formatter:on

    public SetMultimap<String, Rule> makeFragments(Spec spec) {
        log.info("Building fragments.");
        final SetMultimap<String, Rule> fragments =
                RuleUtil.makeFragments(spec.rules(), n -> n.matches(GEN_RE), (n, l) -> true, 2);
        log.info("Built fragments.");
        return fragments;
    }

}