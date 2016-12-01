package org.metaborg.meta.nabl2.stratego;

import java.util.List;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.base.ImmutableFalse;
import org.metaborg.meta.nabl2.constraints.base.ImmutableTrue;
import org.metaborg.meta.nabl2.constraints.equality.ImmutableEqual;
import org.metaborg.meta.nabl2.constraints.equality.ImmutableInequal;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.spoofax.interpreter.terms.IStrategoTerm;

import com.google.common.collect.Lists;

public class StrategoConstraints {

    private final StrategoCommon strategoCommon;

    public StrategoConstraints(StrategoCommon strategoCommon) {
        this.strategoCommon = strategoCommon;
    }

    public Iterable<IConstraint> fromStratego(IStrategoTerm constraintTerm) {
        if (!constraintTerm.isList()) {
            throw new IllegalArgumentException("Constraints must be a list, got: " + constraintTerm);
        }
        List<IConstraint> constraints = Lists.newArrayList();
        for (IStrategoTerm constraint : constraintTerm) {
            constraints.add(StrategoMatchers.<IConstraint> patterns()
                // @formatter:off
                .appl0("CTrue", () -> ImmutableTrue.of())
                .appl1("CFalse", (x) -> ImmutableFalse.of())
                .appl3("CEqual", (t1,t2,x) -> {
                    ITerm term1 = strategoCommon.fromStratego(t1);
                    ITerm term2 = strategoCommon.fromStratego(t2);
                    return ImmutableEqual.of(term1, term2);
                })
                .appl3("CInequal", (t1,t2,x) -> {
                    ITerm term1 = strategoCommon.fromStratego(t1);
                    ITerm term2 = strategoCommon.fromStratego(t2);
                    return ImmutableInequal.of(term1, term2);
                })
                .otherwise(() -> ImmutableTrue.of())
                .match(constraint)
                // @formatter:on
            );
        }
        return constraints;
    }

}