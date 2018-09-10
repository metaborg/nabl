package mb.nabl2.constraints.ast;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import mb.nabl2.constraints.messages.MessageInfo;
import mb.nabl2.stratego.TermIndex;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.nabl2.terms.substitution.ISubstitution;

public final class AstConstraints {

    private static final String C_AST_PROPERTY = "CAstProperty";

    public static IMatcher<IAstConstraint> matcher() {
        return M.<IAstConstraint>cases(
            // @formatter:off
            M.appl3(C_AST_PROPERTY, TermIndex.matcher(), M.term(), M.term(), (c, index, key, value) -> {
                return ImmutableCAstProperty.of(index, key, value, MessageInfo.of(index));
            })
            // @formatter:on
        );
    }

    public static ITerm build(IAstConstraint constraint) {
        return constraint.match(IAstConstraint.Cases.<ITerm>of(
            // @formatter:off
            prop -> B.newAppl(C_AST_PROPERTY, prop.getIndex(), prop.getKey(), prop.getValue())
            // @formatter:on
        ));

    }

    public static IAstConstraint substitute(IAstConstraint constraint, ISubstitution.Immutable subst) {
        return constraint.match(IAstConstraint.Cases.<IAstConstraint>of(
            // @formatter:off
            prop -> ImmutableCAstProperty.of(
                        prop.getIndex(),
                        prop.getKey(),
                        subst.apply(prop.getValue()),
                        prop.getMessageInfo().apply(subst::apply))
            // @formatter:on
        ));
    }

}