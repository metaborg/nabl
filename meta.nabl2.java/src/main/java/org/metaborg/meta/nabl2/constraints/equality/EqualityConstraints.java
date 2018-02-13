package org.metaborg.meta.nabl2.constraints.equality;

import org.metaborg.meta.nabl2.constraints.messages.MessageInfo;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.build.TB;
import org.metaborg.meta.nabl2.terms.matching.Match.IMatcher;
import org.metaborg.meta.nabl2.terms.matching.Match.M;
import org.metaborg.meta.nabl2.terms.unification.IUnifier;

public final class EqualityConstraints {

    private static final String C_EQUAL = "CEqual";
    private static final String C_INEQUAL = "CInequal";

    public static IMatcher<IEqualityConstraint> matcher() {
        return M.<IEqualityConstraint>cases(
            // @formatter:off
            M.appl3(C_EQUAL, M.term(), M.term(), MessageInfo.matcher(), (c, term1, term2, origin) -> {
                return ImmutableCEqual.of(term1, term2, origin);
            }),
            M.appl3(C_INEQUAL, M.term(), M.term(), MessageInfo.matcher(), (c, term1, term2, origin) -> {
                return ImmutableCInequal.of(term1, term2, origin);
            })
            // @formatter:on
        );
    }

    public static ITerm build(IEqualityConstraint constraint) {
        return constraint.match(IEqualityConstraint.Cases.<ITerm>of(
            // @formatter:off
            eq -> TB.newAppl(C_EQUAL, eq.getLeft(), eq.getRight(), MessageInfo.build(eq.getMessageInfo())),
            ineq -> TB.newAppl(C_INEQUAL, ineq.getLeft(), ineq.getRight(), MessageInfo.build(ineq.getMessageInfo()))
            // @formatter:on
        ));
    }

    public static IEqualityConstraint substitute(IEqualityConstraint constraint, IUnifier unifier) {
        return constraint.match(IEqualityConstraint.Cases.<IEqualityConstraint>of(
            // @formatter:off
            eq -> ImmutableCEqual.of(
                    unifier.findRecursive(eq.getLeft()),
                    unifier.findRecursive(eq.getRight()),
                    eq.getMessageInfo().apply(unifier::findRecursive)),
            ineq -> ImmutableCInequal.of(
                    unifier.findRecursive(ineq.getLeft()),
                    unifier.findRecursive(ineq.getRight()),
                    ineq.getMessageInfo().apply(unifier::findRecursive))
            // @formatter:on
        ));
    }

}