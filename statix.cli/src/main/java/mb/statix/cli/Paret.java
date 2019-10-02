package mb.statix.cli;

import static mb.statix.random.strategy.SearchStrategies.*;
import static mb.statix.random.util.StreamUtil.flatMap;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.metaborg.util.functions.Function1;

import com.google.common.collect.ImmutableMap;

import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.constraints.CResolveQuery;
import mb.statix.constraints.CUser;
import mb.statix.random.EitherSearchState;
import mb.statix.random.FocusedSearchState;
import mb.statix.random.SearchState;
import mb.statix.random.SearchStrategy;
import mb.statix.random.predicate.Any;
import mb.statix.random.predicate.Match;
import mb.statix.random.predicate.Not;
import mb.statix.random.util.StreamUtil;
import mb.statix.scopegraph.reference.CriticalEdge;
import mb.statix.solver.completeness.CompletenessUtil;

public class Paret {

    public static SearchStrategy<SearchState, SearchState> search() {
        // @formatter:off
        return seq(searchExp())
               .$(marker("generateLex"))
               .$(generateLex())
               .$(marker("done"))
               .$();
        // @formatter:on
    }

    // inference step

    private static SearchStrategy<SearchState, SearchState> inferDelayAndDrop() {
        return seq(infer()).$(delayStuckQueries()).$(dropAst()).$();
    }

    // generation of expressions

    private static SearchStrategy<SearchState, SearchState> searchExp() {
        // @formatter:off
        return repeat(limit(10, fix(
            seq(selectConstraint(1))
            .$(match(
               limit(3, seq(limit(5, resolve())).$(infer()).$()),
               limit(1, seq(limit(5, expand(defaultRuleWeight, ruleWeights))).$(infer()).$())))
            .$(),
            inferDelayAndDrop(),
            new Match("is_.*"), // everything except is_* constraints should be resolved
            50 // what is a good number here? size / 4?
        )));
        // @formatter:on
    }

    // @formatter:off
    private static int defaultRuleWeight = 1;
    private static Map<String, Double> ruleWeights = ImmutableMap.<String, Double>builder()
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

    public static
            SearchStrategy<SearchState, EitherSearchState<FocusedSearchState<CResolveQuery>, FocusedSearchState<CUser>>>
            selectConstraint(int limit) {
        // @formatter:off
        return limit(limit, concatAlt(
            // TWEAK Resolve queries first, to improve inference
            select(CResolveQuery.class, new Any<>()),
            select(CUser.class, /*Paret::predWeights*/ new Match("gen_.*"))
        ));
        // @formatter:on
    }

    private static Function1<CUser, Double> predWeights(SearchState state) {
        final IUnifier unifier = state.state().unifier();
        final Set<CriticalEdge> criticalEdges =
                flatMap(state.delays().values().stream(), d -> d.criticalEdges().stream()).collect(Collectors.toSet());
        // @formatter:off
        final Stream<CUser> criticalPreds = StreamUtil.filterInstances(CUser.class, state.constraints().stream())
            .filter(c -> CompletenessUtil.criticalEdges(c, state.state().spec(), unifier).stream().anyMatch(criticalEdges::contains));
        final Set<ITermVar> criticalVars = flatMap(criticalPreds, c -> flatMap(c.args().stream(), arg -> unifier.getVars(arg).stream()))
            .collect(Collectors.toSet());
        // @formatter:on
        return (c) -> {
            if(!c.name().matches("gen_.*")) {
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

    private static SearchStrategy<SearchState, SearchState> generateLex() {
        return require(limit(1, fix(expandLex(), infer(), new Not<>(new Match("is_.*")), -1)));
    }

    private static SearchStrategy<SearchState, SearchState> expandLex() {
        // @formatter:off
        return seq(select(CUser.class, new Match("is_.*")))
               .$(limit(1, seq(expand(1, idWeights)).$(infer()).$()))
               .$();
        // @formatter:on
    }

    // @formatter:off
    private static Map<String, Double> idWeights = ImmutableMap.<String, Double>builder()
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

}