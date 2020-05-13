package mb.nabl2.constraints.equality;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import org.metaborg.util.functions.Function1;

import mb.nabl2.constraints.messages.MessageInfo;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.nabl2.terms.substitution.ISubstitution;

public final class EqualityConstraints {

    private static final String C_EQUAL = "CEqual";
    private static final String C_INEQUAL = "CInequal";

    public static IMatcher<IEqualityConstraint> matcher() {
        return M.<IEqualityConstraint>cases(
        // @formatter:off
            M.appl3(C_EQUAL, M.term(), M.term(), MessageInfo.matcher(), (c, term1, term2, origin) -> {
                return CEqual.of(term1, term2, origin);
            }),
            M.appl3(C_INEQUAL, M.term(), M.term(), MessageInfo.matcher(), (c, term1, term2, origin) -> {
                return CInequal.of(term1, term2, origin);
            })
            // @formatter:on
        );
    }

    public static ITerm build(IEqualityConstraint constraint) {
        return constraint.match(IEqualityConstraint.Cases.<ITerm>of(
        // @formatter:off
            eq -> B.newAppl(C_EQUAL, eq.getLeft(), eq.getRight(), MessageInfo.build(eq.getMessageInfo())),
            ineq -> B.newAppl(C_INEQUAL, ineq.getLeft(), ineq.getRight(), MessageInfo.build(ineq.getMessageInfo()))
            // @formatter:on
        ));
    }

    public static IEqualityConstraint substitute(IEqualityConstraint constraint, ISubstitution.Immutable subst) {
        // @formatter:off
        return constraint.match(IEqualityConstraint.Cases.<IEqualityConstraint>of(
            eq -> CEqual.of(
                    subst.apply(eq.getLeft()),
                    subst.apply(eq.getRight()),
                    eq.getMessageInfo().apply(subst::apply)),
            ineq -> CInequal.of(
                    subst.apply(ineq.getLeft()),
                    subst.apply(ineq.getRight()),
                    ineq.getMessageInfo().apply(subst::apply))
        ));
        // @formatter:on
    }

    public static IEqualityConstraint transform(IEqualityConstraint constraint, Function1<ITerm, ITerm> map) {
        // @formatter:off
        return constraint.match(IEqualityConstraint.Cases.<IEqualityConstraint>of(
            eq -> CEqual.of(
                    map.apply(eq.getLeft()),
                    map.apply(eq.getRight()),
                    eq.getMessageInfo().apply(map::apply)),
            ineq -> CInequal.of(
                    map.apply(ineq.getLeft()),
                    map.apply(ineq.getRight()),
                    ineq.getMessageInfo().apply(map::apply))
        ));
        // @formatter:on
    }

}