package org.metaborg.meta.nabl2.stratego;

import java.util.List;
import java.util.Optional;

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
                    .appl1("CFalse", (msg) -> ImmutableFalse.of().setOriginatingTerm(originatingTerm(msg)))
                    .appl3("CEqual", (t1, t2, msg) -> {
                        ITerm term1 = strategoCommon.fromStratego(t1);
                        ITerm term2 = strategoCommon.fromStratego(t2);
                        return ImmutableEqual.of(term1, term2).setOriginatingTerm(originatingTerm(msg));
                    }).appl3("CInequal", (t1, t2, msg) -> {
                        ITerm term1 = strategoCommon.fromStratego(t1);
                        ITerm term2 = strategoCommon.fromStratego(t2);
                        return ImmutableInequal.of(term1, term2).setOriginatingTerm(originatingTerm(msg));
                    }).otherwise(() -> ImmutableTrue.of()).match(constraint)
            // @formatter:on
            );
        }
        return constraints;
    }

    private Optional<IStrategoTerm> originatingTerm(IStrategoTerm messageTerm) {
        return StrategoMatchers.<Optional<IStrategoTerm>> patterns()
                // @formatter:off
                .appl3("Message", (kind, message, origin) -> Optional.of(origin))
                .otherwise(() -> Optional.empty())
                .match(messageTerm);
                // @formatter:on
    }

}