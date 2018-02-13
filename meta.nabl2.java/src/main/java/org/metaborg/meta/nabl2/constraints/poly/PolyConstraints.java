package org.metaborg.meta.nabl2.constraints.poly;

import org.metaborg.meta.nabl2.constraints.messages.MessageInfo;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.build.TB;
import org.metaborg.meta.nabl2.terms.matching.Match.IMatcher;
import org.metaborg.meta.nabl2.terms.matching.Match.M;
import org.metaborg.meta.nabl2.terms.unification.IUnifier;

public final class PolyConstraints {

    private static final String C_GEN = "CGen";
    private static final String C_INST = "CInst";

    public static IMatcher<IPolyConstraint> matcher() {
        return M.<IPolyConstraint>cases(
            // @formatter:off
            M.appl4(C_GEN, M.term(), M.var(), M.term(), MessageInfo.matcher(), (c, scheme, genVars, type, origin) -> {
                return ImmutableCGeneralize.of(scheme, genVars, type, origin);
            }),
            M.appl4(C_INST, M.term(), M.var(), M.term(), MessageInfo.matcher(), (c, type, instVars, scheme, origin) -> {
                return ImmutableCInstantiate.of(type, instVars, scheme, origin);
            })
            // @formatter:on
        );
    }

    public static ITerm build(IPolyConstraint constraint) {
        return constraint.match(IPolyConstraint.Cases.<ITerm>of(
            // @formatter:off
            gen -> TB.newAppl(C_GEN, gen.getDeclaration(), gen.getType(), MessageInfo.build(gen.getMessageInfo())),
            inst -> TB.newAppl(C_INST, inst.getType(), inst.getDeclaration(), MessageInfo.build(inst.getMessageInfo()))
            // @formatter:on
        ));
    }

    public static IPolyConstraint substitute(IPolyConstraint constraint, IUnifier unifier) {
        return constraint.match(IPolyConstraint.Cases.of(
            // @formatter:off
            gen -> ImmutableCGeneralize.of(
                        unifier.findRecursive(gen.getDeclaration()),
                        gen.getGenVars(),
                        unifier.findRecursive(gen.getType()),
                        gen.getMessageInfo().apply(unifier::findRecursive)),
            inst -> ImmutableCInstantiate.of(
                        unifier.findRecursive(inst.getType()),
                        inst.getInstVars(),
                        unifier.findRecursive(inst.getDeclaration()),
                        inst.getMessageInfo().apply(unifier::findRecursive))
            // @formatter:on
        ));
    }

}