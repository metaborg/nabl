package org.metaborg.meta.nabl2.constraints.poly;

import org.metaborg.meta.nabl2.constraints.messages.MessageInfo;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.terms.generic.TB;
import org.metaborg.meta.nabl2.unification.IUnifier;

public final class PolyConstraints {

    private static final String C_GEN = "CGen";
    private static final String C_INST = "CInst";

    public static IMatcher<IPolyConstraint> matcher() {
        return M.<IPolyConstraint>cases(
            // @formatter:off
            M.appl3(C_GEN, M.term(), M.term(), MessageInfo.matcher(), (c, scheme, type, origin) -> {
                return ImmutableCGeneralize.of(scheme, type, origin);
            }),
            M.appl3(C_INST, M.term(), M.term(), MessageInfo.matcher(), (c, type, scheme, origin) -> {
                return ImmutableCInstantiate.of(type, scheme, origin);
            })
            // @formatter:on
        );
    }

    public static ITerm build(IPolyConstraint constraint) {
        return constraint.match(IPolyConstraint.Cases.<ITerm>of(
            // @formatter:off
            gen -> TB.newAppl(C_GEN, gen.getScheme(), gen.getType(), MessageInfo.build(gen.getMessageInfo())),
            inst -> TB.newAppl(C_INST, inst.getType(), inst.getScheme(), MessageInfo.build(inst.getMessageInfo()))
            // @formatter:on
        ));
    }

    public static IPolyConstraint find(IPolyConstraint constraint, IUnifier unifier) {
        return constraint.match(IPolyConstraint.Cases.of(
            // @formatter:off
            gen -> ImmutableCGeneralize.of(
                        unifier.find(gen.getScheme()),
                        unifier.find(gen.getType()),
                        gen.getMessageInfo().apply(unifier::find)),
            inst -> ImmutableCInstantiate.of(
                        unifier.find(inst.getType()),
                        unifier.find(inst.getScheme()),
                        inst.getMessageInfo().apply(unifier::find))
            // @formatter:on
        ));
    }

}