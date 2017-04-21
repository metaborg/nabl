package org.metaborg.meta.nabl2.constraints.ast;

import org.metaborg.meta.nabl2.constraints.messages.MessageInfo;
import org.metaborg.meta.nabl2.stratego.TermIndex;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.terms.generic.TB;
import org.metaborg.meta.nabl2.unification.IUnifier;

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
            prop -> TB.newAppl(C_AST_PROPERTY, prop.getIndex(), prop.getKey(), prop.getValue())
            // @formatter:on
        ));

    }

    public static IAstConstraint find(IAstConstraint constraint, IUnifier unifier) {
        return constraint.match(IAstConstraint.Cases.<IAstConstraint>of(
            // @formatter:off
            prop -> ImmutableCAstProperty.of(
                        prop.getIndex(),
                        prop.getKey(),
                        unifier.find(prop.getValue()),
                        prop.getMessageInfo().apply(unifier::find))
            // @formatter:on
        ));
    }

}