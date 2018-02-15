package org.metaborg.meta.nabl2.constraints.base;

import java.util.stream.Collectors;

import org.metaborg.meta.nabl2.constraints.Constraints;
import org.metaborg.meta.nabl2.constraints.messages.MessageInfo;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.build.TB;
import org.metaborg.meta.nabl2.terms.matching.Match.IMatcher;
import org.metaborg.meta.nabl2.terms.matching.Match.M;
import org.metaborg.meta.nabl2.terms.unification.IUnifier;

public final class BaseConstraints {

    private static final String C_FALSE = "CFalse";
    private static final String C_TRUE = "CTrue";
    private static final String C_CONJ = "CConj";
    private static final String C_EXISTS = "CExists";
    private static final String C_NEW = "CNew";

    public static IMatcher<IBaseConstraint> matcher() {
        return M.<IBaseConstraint>cases(
        // @formatter:off
            M.appl1(C_TRUE, MessageInfo.matcherOnlyOriginTerm(), (c, origin) -> {
                return ImmutableCTrue.of(origin);
            }),
            M.appl1(C_FALSE, MessageInfo.matcher(), (c, msginfo) -> {
                return ImmutableCFalse.of(msginfo);
            }),
            M.appl2(C_CONJ, (t, u) -> Constraints.matcher().match(t, u), (t, u) -> Constraints.matcher().match(t, u),
                    (c, c1, c2) -> {
                        return ImmutableCConj.of(c1, c2, MessageInfo.empty());
                    }),
            M.appl2(C_EXISTS, M.listElems(M.var()), (t, u) -> Constraints.matcher().match(t, u),
                    (c, vars, constraint) -> {
                        return ImmutableCExists.of(vars, constraint, MessageInfo.empty());
                    }),
            M.appl2(C_NEW, M.listElems(M.var()), MessageInfo.matcherOnlyOriginTerm(),
                    (c, vars, origin) -> {
                        return ImmutableCNew.of(vars, origin);
                    })
            // @formatter:on
        );
    }

    public static ITerm build(IBaseConstraint constraint) {
        return constraint.match(IBaseConstraint.Cases.<ITerm>of(
        // @formatter:off
            t -> TB.newAppl(C_TRUE, MessageInfo.buildOnlyOriginTerm(t.getMessageInfo())),
            f -> TB.newAppl(C_FALSE, MessageInfo.build(f.getMessageInfo())),
            c -> TB.newAppl(C_CONJ, Constraints.build(c.getLeft()), Constraints.build(c.getRight())),
            e -> TB.newAppl(C_EXISTS, TB.newList(e.getEVars()), Constraints.build(e.getConstraint())),
            n -> TB.newAppl(C_NEW, TB.newList(n.getNVars()), MessageInfo.buildOnlyOriginTerm(n.getMessageInfo()))
            // @formatter:on
        ));
    }

    public static IBaseConstraint substitute(IBaseConstraint constraint, IUnifier.Immutable subst) {
        return constraint.match(IBaseConstraint.Cases.<IBaseConstraint>of(
        // @formatter:off
            t -> ImmutableCTrue.of(t.getMessageInfo().apply(subst::findRecursive)),
            f -> ImmutableCFalse.of(f.getMessageInfo().apply(subst::findRecursive)),
            c -> {
                return ImmutableCConj.of(
                        Constraints.substitute(c.getLeft(), subst),
                        Constraints.substitute(c.getRight(), subst),
                        c.getMessageInfo().apply(subst::findRecursive));
            },
            e -> {
                final IUnifier.Immutable restrictedSubst = subst.removeAll(e.getEVars()).unifier();
                return ImmutableCExists.of(e.getEVars(),
                        Constraints.substitute(e.getConstraint(), restrictedSubst),
                        e.getMessageInfo().apply(restrictedSubst::findRecursive));
            },
            n -> {
                return ImmutableCNew.of(n.getNVars().stream().map(subst::findRecursive).collect(Collectors.toSet()),
                        n.getMessageInfo().apply(subst::findRecursive));
            }
            // @formatter:on
        ));
    }

}