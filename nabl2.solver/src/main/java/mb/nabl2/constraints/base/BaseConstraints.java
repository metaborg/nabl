package mb.nabl2.constraints.base;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.collection.ImList;
import org.metaborg.util.functions.Function1;

import io.usethesource.capsule.util.stream.CapsuleCollectors;
import mb.nabl2.constraints.Constraints;
import mb.nabl2.constraints.messages.MessageInfo;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.nabl2.terms.substitution.ISubstitution;

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
                return CTrue.of(origin);
            }),
            M.appl1(C_FALSE, MessageInfo.matcher(), (c, msginfo) -> {
                return CFalse.of(msginfo);
            }),
            M.appl2(C_CONJ, (t, u) -> Constraints.matcher().match(t, u), (t, u) -> Constraints.matcher().match(t, u),
                    (c, c1, c2) -> {
                        return CConj.of(c1, c2, MessageInfo.empty());
                    }),
            M.appl2(C_EXISTS, M.listElems(M.var()), (t, u) -> Constraints.matcher().match(t, u),
                    (c, vars, constraint) -> {
                        return CExists.of(CapsuleUtil.toSet(vars), constraint, MessageInfo.empty());
                    }),
            M.appl2(C_NEW, M.listElems(M.var()), MessageInfo.matcherOnlyOriginTerm(),
                    (c, vars, origin) -> {
                        return CNew.of(CapsuleUtil.toSet(vars), origin);
                    })
            // @formatter:on
        );
    }

    public static ITerm build(IBaseConstraint constraint) {
        return constraint.match(IBaseConstraint.Cases.<ITerm>of(
        // @formatter:off
            t -> B.newAppl(C_TRUE, MessageInfo.buildOnlyOriginTerm(t.getMessageInfo())),
            f -> B.newAppl(C_FALSE, MessageInfo.build(f.getMessageInfo())),
            c -> B.newAppl(C_CONJ, Constraints.build(c.getLeft()), Constraints.build(c.getRight())),
            e -> B.newAppl(C_EXISTS, B.newList(e.getEVars()), Constraints.build(e.getConstraint())),
            n -> B.newAppl(C_NEW, B.newList(n.getNVars()), MessageInfo.buildOnlyOriginTerm(n.getMessageInfo()))
            // @formatter:on
        ));
    }

    public static IBaseConstraint substitute(IBaseConstraint constraint, ISubstitution.Immutable subst) {
        // @formatter:off
        return constraint.match(IBaseConstraint.Cases.<IBaseConstraint>of(
            t -> CTrue.of(t.getMessageInfo().apply(subst::apply)),
            f -> CFalse.of(f.getMessageInfo().apply(subst::apply)),
            c -> {
                return CConj.of(
                        Constraints.substitute(c.getLeft(), subst),
                        Constraints.substitute(c.getRight(), subst),
                        c.getMessageInfo().apply(subst::apply));
            },
            e -> {
                final ISubstitution.Immutable restrictedSubst = subst.removeAll(e.getEVars());
                return CExists.of(e.getEVars(),
                        Constraints.substitute(e.getConstraint(), restrictedSubst),
                        e.getMessageInfo().apply(restrictedSubst::apply));
            },
            n -> {
                return CNew.of(n.getNVars().stream().map(subst::apply).collect(CapsuleCollectors.toSet()),
                        n.getMessageInfo().apply(subst::apply));
            }
        ));
        // @formatter:on
    }

    public static IBaseConstraint transform(IBaseConstraint constraint, Function1<ITerm, ITerm> map) {
        // @formatter:off
        return constraint.match(IBaseConstraint.Cases.<IBaseConstraint>of(
            t -> CTrue.of(t.getMessageInfo().apply(map::apply)),
            f -> CFalse.of(f.getMessageInfo().apply(map::apply)),
            c -> {
                return CConj.of(
                        Constraints.transform(c.getLeft(), map),
                        Constraints.transform(c.getRight(), map),
                        c.getMessageInfo().apply(map::apply));
            },
            e -> {
                return CExists.of(e.getEVars(),
                        Constraints.transform(e.getConstraint(), map),
                        e.getMessageInfo().apply(map::apply));
            },
            n -> {
                return CNew.of(n.getNVars().stream().map(map::apply).collect(CapsuleCollectors.toSet()),
                        n.getMessageInfo().apply(map::apply));
            }
        ));
        // @formatter:on
    }

}