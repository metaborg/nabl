package mb.nabl2.constraints.nameresolution;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import org.metaborg.util.functions.Function1;

import mb.nabl2.constraints.Constraints;
import mb.nabl2.constraints.messages.MessageInfo;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.scopegraph.pepm16.terms.Label;

public final class NameResolutionConstraints {

    private static final String C_RESOLVE = "CResolve";
    private static final String C_ASSOC = "CAssoc";
    private static final String C_DECL_PROPERTY = "CDeclProperty";

    public static IMatcher<INameResolutionConstraint> matcher() {
        return M.<INameResolutionConstraint>cases(
        // @formatter:off
            M.appl3(C_RESOLVE, M.term(), M.term(), MessageInfo.matcher(),
                    (c, ref, decl, origin) -> {
                        return CResolve.of(ref, decl, origin);
                    }),
            M.appl4(C_ASSOC, M.term(), Label.matcher(), M.term(), MessageInfo.matcher(),
                    (c, decl, label, scope, origin) -> {
                        return CAssoc.of(decl, label, scope, origin);
                    }),
            M.appl5(C_DECL_PROPERTY, M.term(), M.term(), M.term(), Constraints.priorityMatcher(), MessageInfo.matcher(),
                    (c, decl, key, value, prio, origin) -> {
                        return CDeclProperty.of(decl, key, value, prio, origin);
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

    public static INameResolutionConstraint substitute(INameResolutionConstraint constraint,
            ISubstitution.Immutable subst) {
        // @formatter:off
        return constraint.match(INameResolutionConstraint.Cases.<INameResolutionConstraint>of(
            res -> CResolve.of(
                        subst.apply(res.getReference()),
                        subst.apply(res.getDeclaration()),
                        res.getMessageInfo().apply(subst::apply)),
            assoc -> CAssoc.of(
                        subst.apply(assoc.getDeclaration()),
                        assoc.getLabel(),
                        subst.apply(assoc.getScope()),
                        assoc.getMessageInfo().apply(subst::apply)),
            prop -> CDeclProperty.of(
                        subst.apply(prop.getDeclaration()),
                        prop.getKey(),
                        subst.apply(prop.getValue()),
                        prop.getPriority(),
                        prop.getMessageInfo().apply(subst::apply))
        ));
        // @formatter:on
    }

    public static INameResolutionConstraint transform(INameResolutionConstraint constraint,
            Function1<ITerm, ITerm> map) {
        // @formatter:off
        return constraint.match(INameResolutionConstraint.Cases.<INameResolutionConstraint>of(
            res -> CResolve.of(
                        map.apply(res.getReference()),
                        map.apply(res.getDeclaration()),
                        res.getMessageInfo().apply(map::apply)),
            assoc -> CAssoc.of(
                        map.apply(assoc.getDeclaration()),
                        assoc.getLabel(),
                        map.apply(assoc.getScope()),
                        assoc.getMessageInfo().apply(map::apply)),
            prop -> CDeclProperty.of(
                        map.apply(prop.getDeclaration()),
                        prop.getKey(),
                        map.apply(prop.getValue()),
                        prop.getPriority(),
                        prop.getMessageInfo().apply(map::apply))
        ));
        // @formatter:on
    }

}