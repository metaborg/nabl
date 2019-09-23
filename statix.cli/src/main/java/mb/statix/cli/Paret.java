package mb.statix.cli;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.statix.random.strategy.SearchStrategies.*;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.ITerm;
import mb.statix.constraints.CResolveQuery;
import mb.statix.constraints.CUser;
import mb.statix.constraints.Constraints;
import mb.statix.random.FocusedSearchState;
import mb.statix.random.SearchState;
import mb.statix.random.SearchStrategy;
import mb.statix.random.predicate.Any;
import mb.statix.random.predicate.Match;
import mb.statix.random.predicate.Not;
import mb.statix.random.strategy.Either2;

public class Paret {

    public static SearchStrategy<SearchState, SearchState> allIn() {
        // @formatter:off
        return seq(generateExp(4),
               seq(generateLex(),
                   identity()));
        // @formatter:on
    }

    // generation of expressions

    private static SearchStrategy<SearchState, SearchState> inferAndDrop() {
        return seq(infer(), dropAst());
    }

    // generation of expressions

    private static SearchStrategy<SearchState, SearchState> generateExp(int size) {
        return seq(addExpSizeAndSorts(size), fix(enumerateComb(), inferAndDrop(), new Match("gen_.*")));
    }

    private static SearchStrategy<SearchState, SearchState> addExpSizeAndSorts(int size) {
        return addAuxPred("programOK", c -> {
            final ITerm e = c.args().get(0);
            final CUser Csize = new CUser("gen_sizeExpr", ImmutableList.of(B.newInt(size), e), c);
            final CUser Csort = new CUser("gen_isExpr", ImmutableList.of(e), c);
            return Constraints.conjoin(ImmutableList.of(Csize, Csort));
        });
    }

    // enumerate all possible combinations of solving constraints
    private static SearchStrategy<SearchState, SearchState> enumerateComb() {
        // @formatter:off
        return seq(selectConstraint(1), match(expand(), resolve()));
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
        return limit(1, fix(expandLex(), inferAndDrop(), new Not<>(new Match("gen_is.*"))));
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