package mb.statix.cli;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.statix.random.strategy.SearchStrategies.*;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

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
import mb.statix.random.strategy.SearchStrategies;

public class STLC {

    private static final ILogger log = LoggerUtils.logger(STLC.class);

    public static SearchStrategy<SearchState, SearchState> allIn() {
        // @formatter:off
        return generateExp(30);
//        return seq(generateExp(30),
//               seq(generateIDs(),
//                   identity()));
        // @formatter:on
    }

    // generation of expressions

    public static SearchStrategy<SearchState, SearchState> inferAndDrop() {
        return seq(infer(), dropAst());
    }

    // generation of expressions

    public static SearchStrategy<SearchState, SearchState> generateExp(int size) {
        //        return seq(addExpSizeAndSorts(size), fix(enumerateComb(), inferAndDrop(), new Match("gen_.*")));
        return fix(enumerateComb(), inferAndDrop(), new Match("gen_.*"));
    }

    public static SearchStrategy<SearchState, SearchState> addExpSizeAndSorts(int size) {
        return addAuxPred("programOK", c -> {
            ITerm e = c.args().get(0);
            CUser Csize = new CUser("gen_sizeExp", ImmutableList.of(B.newInt(size), e), c);
            CUser Csort = new CUser("gen_isExp", ImmutableList.of(e), c);
            return Constraints.conjoin(ImmutableList.of(Csize, Csort));
        });
    }

    // enumerate all possible combinations of solving constraints
    public static SearchStrategy<SearchState, SearchState> enumerateComb() {
        // @formatter:off
        return seq(selectConstraint(1), match(
            expandExpComb(),
            resolve()
        ));
        // @formatter:on
    }

    public static SearchStrategy<SearchState, Either2<FocusedSearchState<CUser>, FocusedSearchState<CResolveQuery>>>
            selectConstraint(int limit) {
        // @formatter:off
        return limit(limit, alt(
            select(CUser.class, new Not<>(new Match("allFields|dst|subField|subFields|subType|unique|gen_.*"))),
            seq(select(CResolveQuery.class, new Any<>()), canResolve())
        ));
        // @formatter:on
    }

    public static SearchStrategy<FocusedSearchState<CUser>, SearchState> expandExpComb() {
        // @formatter:off
        return expand();
        // @formatter:on
    }

    public static SearchStrategy<SearchState, SearchState> search(int ruleLimit) {
        // @formatter:off
        return seq(selectConstraint(1), match(
            expandExpSearch(ruleLimit),
            resolve()
        ));
        // @formatter:on
    }

    public static SearchStrategy<FocusedSearchState<CUser>, SearchState> expandExpSearch(int ruleLimit) {
        // @formatter:off
        return limit(ruleLimit, expand(ImmutableMap.of(
        )));
        // @formatter:on
    }

    // generation of types

    public static SearchStrategy<SearchState, SearchState> generateTypes(int size) {
        return seq(addTypeSize(size), limit(1, fix(expandType(), inferAndDrop(), new Not<>(new Match("gen_isType")))));
    }

    public static SearchStrategy<SearchState, SearchState> addTypeSize(int size) {
        return addAuxPred("gen_isTypeExp", c -> {
            CUser Csize = new CUser("gen_sizeTypeExp", ImmutableList.of(B.newInt(size), c.args().get(0)), c);
            return Csize;
        });
    }

    public static SearchStrategy<SearchState, SearchState> expandType() {
        // @formatter:off
        return seq(
            select(CUser.class, new Match("gen_isTypeExp")),
            expand()
        );
        // @formatter:on
    }

    // generation of id's

    public static SearchStrategy<SearchState, SearchState> generateIDs() {
        return limit(1, fix(expandID(), inferAndDrop(), new Not<>(new Match("gen_isID"))));
    }

    public static SearchStrategy<SearchState, SearchState> expandID() {
        // @formatter:off
        return seq(
            select(CUser.class, new Match("gen_isID")),
            expand()
        );
        // @formatter:on
    }

    // util

    public static <I, O> SearchStrategy<I, O> debug(String name, SearchStrategy<I, O> s) {
        return SearchStrategies.debug(s, n -> {
            log.info("{}: {}", name, n);
        });
    }

}