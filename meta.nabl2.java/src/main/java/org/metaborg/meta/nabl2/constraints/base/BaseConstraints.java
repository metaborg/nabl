package org.metaborg.meta.nabl2.constraints.base;

import org.metaborg.meta.nabl2.constraints.messages.MessageInfo;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.terms.generic.TB;
import org.metaborg.meta.nabl2.unification.IUnifier;

public final class BaseConstraints {

    private static final String C_FALSE = "CFalse";
    private static final String C_TRUE = "CTrue";

    public static IMatcher<IBaseConstraint> matcher() {
        return M.<IBaseConstraint>cases(
            // @formatter:off
            M.appl1(C_TRUE, MessageInfo.matcherOnlyOriginTerm(), (c, origin) -> {
                return ImmutableCTrue.of(origin);
            }),
            M.appl1(C_FALSE, MessageInfo.matcher(), (c, origin) -> {
                return ImmutableCFalse.of(origin);
            })
            // @formatter:on
        );
    }

    public static ITerm build(IBaseConstraint constraint) {
        return constraint.match(IBaseConstraint.Cases.<ITerm>of(
            // @formatter:off
            t -> TB.newAppl(C_TRUE, MessageInfo.buildOnlyOriginTerm(t.getMessageInfo())),
            f -> TB.newAppl(C_FALSE, MessageInfo.build(f.getMessageInfo()))
            // @formatter:on
        ));
    }

    public static IBaseConstraint find(IBaseConstraint constraint, IUnifier unifier) {
        return constraint.match(IBaseConstraint.Cases.<IBaseConstraint>of(
            // @formatter:off
            t -> ImmutableCTrue.of(t.getMessageInfo().apply(unifier::find)),
            f -> ImmutableCTrue.of(f.getMessageInfo().apply(unifier::find))
            // @formatter:on
        ));
    }

}