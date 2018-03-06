package mb.nabl2.constraints.nameresolution;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import mb.nabl2.constraints.Constraints;
import mb.nabl2.constraints.messages.MessageInfo;
import mb.nabl2.scopegraph.terms.Label;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.nabl2.terms.unification.IUnifier;

public final class NameResolutionConstraints {

    private static final String C_RESOLVE = "CResolve";
    private static final String C_ASSOC = "CAssoc";
    private static final String C_DECL_PROPERTY = "CDeclProperty";

    public static IMatcher<INameResolutionConstraint> matcher() {
        return M.<INameResolutionConstraint>cases(
            // @formatter:off
            M.appl3(C_RESOLVE, M.term(), M.term(), MessageInfo.matcher(),
                    (c, ref, decl, origin) -> {
                        return ImmutableCResolve.of(ref, decl, origin);
                    }),
            M.appl4(C_ASSOC, M.term(), Label.matcher(), M.term(), MessageInfo.matcher(),
                    (c, decl, label, scope, origin) -> {
                        return ImmutableCAssoc.of(decl, label, scope, origin);
                    }),
            M.appl5(C_DECL_PROPERTY, M.term(), M.term(), M.term(), Constraints.priorityMatcher(), MessageInfo.matcher(),
                    (c, decl, key, value, prio, origin) -> {
                        return ImmutableCDeclProperty.of(decl, key, value, prio, origin);
                    })
            // @formatter:on
        );
    }

    public static ITerm build(INameResolutionConstraint constraint) {
        return constraint.match(INameResolutionConstraint.Cases.<ITerm>of(
            // @formatter:off
            res -> B.newAppl(C_RESOLVE, res.getReference(), res.getDeclaration(),
                              MessageInfo.build(res.getMessageInfo())),
            assoc -> B.newAppl(C_ASSOC, assoc.getDeclaration(), assoc.getLabel(), assoc.getScope(),
                                MessageInfo.build(assoc.getMessageInfo())),
            prop ->B.newAppl(C_DECL_PROPERTY, prop.getDeclaration(), prop.getKey(), prop.getValue(),
                              Constraints.buildPriority(prop.getPriority()), MessageInfo.build(prop.getMessageInfo()))
            // @formatter:on
        ));
    }

    public static INameResolutionConstraint substitute(INameResolutionConstraint constraint, IUnifier unifier) {
        return constraint.match(INameResolutionConstraint.Cases.<INameResolutionConstraint>of(
            // @formatter:off
            res -> ImmutableCResolve.of(
                        unifier.findRecursive(res.getReference()),
                        unifier.findRecursive(res.getDeclaration()),
                        res.getMessageInfo().apply(unifier::findRecursive)),
            assoc -> ImmutableCAssoc.of(
                        unifier.findRecursive(assoc.getDeclaration()),
                        assoc.getLabel(),
                        unifier.findRecursive(assoc.getScope()),
                        assoc.getMessageInfo().apply(unifier::findRecursive)),
            prop -> ImmutableCDeclProperty.of(
                        unifier.findRecursive(prop.getDeclaration()),
                        prop.getKey(),
                        unifier.findRecursive(prop.getValue()),
                        prop.getPriority(),
                        prop.getMessageInfo().apply(unifier::findRecursive))
            // @formatter:on
        ));
    }

}