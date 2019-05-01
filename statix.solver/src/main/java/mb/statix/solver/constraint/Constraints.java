package mb.statix.solver.constraint;

import org.metaborg.util.functions.CheckedFunction1;
import org.metaborg.util.functions.Function1;

import mb.statix.solver.IConstraint;

public final class Constraints {

    private Constraints() {
    }

    // @formatter:off
    public static <R> IConstraint.Cases<R> cases(
                Function1<CEqual,R> onEqual,
                Function1<CFalse,R> onFalse,
                Function1<CInequal,R> onInequal,
                Function1<CNew,R> onNew,
                Function1<CPathDst,R> onPathDst,
                Function1<CPathLabels,R> onPathLabels,
                Function1<CPathLt,R> onPathLt,
                Function1<CPathMatch,R> onPathMatch,
                Function1<CPathScopes,R> onPathScopes,
                Function1<CPathSrc,R> onPathSrc,
                Function1<CResolveQuery,R> onResolveQuery,
                Function1<CTellEdge,R> onTellEdge,
                Function1<CTellRel,R> onTellRel,
                Function1<CTermId,R> onTermId,
                Function1<CTrue,R> onTrue,
                Function1<CUser,R> onUser
            ) {
        return new IConstraint.Cases<R>() {

            @Override public R caseEqual(CEqual c) {
                return onEqual.apply(c);
            }

            @Override public R caseFalse(CFalse c) {
                return onFalse.apply(c);
            }

            @Override public R caseInequal(CInequal c) {
                return onInequal.apply(c);
            }

            @Override public R caseNew(CNew c) {
                return onNew.apply(c);
            }

            @Override public R casePathDst(CPathDst c) {
                return onPathDst.apply(c);
            }

            @Override public R casePathLabels(CPathLabels c) {
                return onPathLabels.apply(c);
            }

            @Override public R casePathLt(CPathLt c) {
                return onPathLt.apply(c);
            }

            @Override public R casePathMatch(CPathMatch c) {
                return onPathMatch.apply(c);
            }

            @Override public R casePathScopes(CPathScopes c) {
                return onPathScopes.apply(c);
            }

            @Override public R casePathSrc(CPathSrc c) {
                return onPathSrc.apply(c);
            }

            @Override public R caseResolveQuery(CResolveQuery c) {
                return onResolveQuery.apply(c);
            }

            @Override public R caseTellEdge(CTellEdge c) {
                return onTellEdge.apply(c);
            }

            @Override public R caseTellRel(CTellRel c) {
                return onTellRel.apply(c);
            }

            @Override public R caseTermId(CTermId c) {
                return onTermId.apply(c);
            }

            @Override public R caseTrue(CTrue c) {
                return onTrue.apply(c);
            }

            @Override public R caseUser(CUser c) {
                return onUser.apply(c);
            }

        };
    }
    // @formatter:on

    // @formatter:off
    public static <R, E extends Throwable> IConstraint.CheckedCases<R, E> checkedCases(
                CheckedFunction1<CEqual, R, E> onEqual,
                CheckedFunction1<CFalse, R, E> onFalse,
                CheckedFunction1<CInequal, R, E> onInequal,
                CheckedFunction1<CNew, R, E> onNew,
                CheckedFunction1<CPathDst, R, E> onPathDst,
                CheckedFunction1<CPathLabels, R, E> onPathLabels,
                CheckedFunction1<CPathLt, R, E> onPathLt,
                CheckedFunction1<CPathMatch, R, E> onPathMatch,
                CheckedFunction1<CPathScopes, R, E> onPathScopes,
                CheckedFunction1<CPathSrc, R, E> onPathSrc,
                CheckedFunction1<CResolveQuery, R, E> onResolveQuery,
                CheckedFunction1<CTellEdge, R, E> onTellEdge,
                CheckedFunction1<CTellRel, R, E> onTellRel,
                CheckedFunction1<CTermId, R, E> onTermId,
                CheckedFunction1<CTrue, R, E> onTrue,
                CheckedFunction1<CUser, R, E> onUser
            ) {
        return new IConstraint.CheckedCases<R, E>() {

            @Override public R caseEqual(CEqual c) throws E {
                return onEqual.apply(c);
            }

            @Override public R caseFalse(CFalse c) throws E {
                return onFalse.apply(c);
            }

            @Override public R caseInequal(CInequal c) throws E {
                return onInequal.apply(c);
            }

            @Override public R caseNew(CNew c) throws E {
                return onNew.apply(c);
            }

            @Override public R casePathDst(CPathDst c) throws E {
                return onPathDst.apply(c);
            }

            @Override public R casePathLabels(CPathLabels c) throws E {
                return onPathLabels.apply(c);
            }

            @Override public R casePathLt(CPathLt c) throws E {
                return onPathLt.apply(c);
            }

            @Override public R casePathMatch(CPathMatch c) throws E {
                return onPathMatch.apply(c);
            }

            @Override public R casePathScopes(CPathScopes c) throws E {
                return onPathScopes.apply(c);
            }

            @Override public R casePathSrc(CPathSrc c) throws E {
                return onPathSrc.apply(c);
            }

            @Override public R caseResolveQuery(CResolveQuery c) throws E {
                return onResolveQuery.apply(c);
            }

            @Override public R caseTellEdge(CTellEdge c) throws E {
                return onTellEdge.apply(c);
            }

            @Override public R caseTellRel(CTellRel c) throws E {
                return onTellRel.apply(c);
            }

            @Override public R caseTermId(CTermId c) throws E {
                return onTermId.apply(c);
            }

            @Override public R caseTrue(CTrue c) throws E {
                return onTrue.apply(c);
            }

            @Override public R caseUser(CUser c) throws E {
                return onUser.apply(c);
            }

        };
    }
    // @formatter:on

}