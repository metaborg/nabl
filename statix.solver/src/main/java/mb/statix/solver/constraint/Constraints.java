package mb.statix.solver.constraint;

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

            public R caseEqual(CEqual c) {
                return onEqual.apply(c);
            }

            public R caseFalse(CFalse c) {
                return onFalse.apply(c);
            }

            public R caseInequal(CInequal c) {
                return onInequal.apply(c);
            }

            public R caseNew(CNew c) {
                return onNew.apply(c);
            }

            public R casePathDst(CPathDst c) {
                return onPathDst.apply(c);
            }

            public R casePathLabels(CPathLabels c) {
                return onPathLabels.apply(c);
            }

            public R casePathLt(CPathLt c) {
                return onPathLt.apply(c);
            }

            public R casePathMatch(CPathMatch c) {
                return onPathMatch.apply(c);
            }

            public R casePathScopes(CPathScopes c) {
                return onPathScopes.apply(c);
            }

            public R casePathSrc(CPathSrc c) {
                return onPathSrc.apply(c);
            }

            public R caseResolveQuery(CResolveQuery c) {
                return onResolveQuery.apply(c);
            }

            public R caseTellEdge(CTellEdge c) {
                return onTellEdge.apply(c);
            }

            public R caseTellRel(CTellRel c) {
                return onTellRel.apply(c);
            }

            public R caseTermId(CTermId c) {
                return onTermId.apply(c);
            }

            public R caseTrue(CTrue c) {
                return onTrue.apply(c);
            }

            public R caseUser(CUser c) {
                return onUser.apply(c);
            }

        };
    }
    // @formatter:on

}