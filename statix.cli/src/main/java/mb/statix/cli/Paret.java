package mb.statix.cli;

import static mb.statix.random.strategy.SearchStrategies.*;

import mb.statix.constraints.CResolveQuery;
import mb.statix.constraints.CUser;
import mb.statix.random.FocusedSearchState;
import mb.statix.random.SearchState;
import mb.statix.random.SearchStrategy;
import mb.statix.random.predicate.Any;
import mb.statix.random.predicate.Match;
import mb.statix.random.predicate.Not;
import mb.statix.random.strategy.Either2;

public class Paret {

    public static SearchStrategy<SearchState, SearchState> enumerate() {
        // @formatter:off
        return seq(enumerateExp(),
               seq(generateLex(),
                   identity()));
        // @formatter:on
    }

    public static SearchStrategy<SearchState, SearchState> search() {
        // @formatter:off
        return seq(searchExp(),
               seq(generateLex(),
                   identity()));
        // @formatter:on
    }

    // generation of expressions

    private static SearchStrategy<SearchState, SearchState> inferAndDrop() {
        return seq(infer(), dropAst());
    }

    // generation of expressions

    private static SearchStrategy<SearchState, SearchState> enumerateExp() {
        // @formatter:off
        return fix(
            seq(
                selectConstraint(1),
                match(
                    seq(expand(), infer()),
                    seq(resolve(), infer())
                )
            ),
            inferAndDrop(),
            new Match("gen_.*")
        );
        // @formatter:on
    }

    private static SearchStrategy<SearchState, SearchState> searchExp() {
        // @formatter:off
        return repeat(fix(
            seq(
                selectConstraint(1),
                match(
                    limit(3, seq(expand(), infer())),
                    limit(3, seq(resolve(), infer()))
                )
            ),
            inferAndDrop(),
            new Match("gen_.*")
        ));
        // @formatter:on
    }

    public static SearchStrategy<SearchState, Either2<FocusedSearchState<CUser>, FocusedSearchState<CResolveQuery>>>
            selectConstraint(int limit) {
        // @formatter:off
        return limit(limit, alt(
            select(CUser.class, new Not<>(new Match("gen_.*"))),
            seq(select(CResolveQuery.class, new Any<>()), canResolve())
        ));
        // @formatter:on
    }

    // generation of id's

    private static SearchStrategy<SearchState, SearchState> generateLex() {
        return limit(1, fix(expandLex(), infer(), new Not<>(new Match("gen_is.*"))));
    }

    private static SearchStrategy<SearchState, SearchState> expandLex() {
        // @formatter:off
        return seq(
            select(CUser.class, new Match("gen_is.*")),
            expand()
        );
        // @formatter:on
    }

}