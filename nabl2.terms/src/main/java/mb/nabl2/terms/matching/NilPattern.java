package mb.nabl2.terms.matching;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Optional;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.functions.Action2;
import org.metaborg.util.functions.Function0;
import org.metaborg.util.functions.Function1;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.IAttachments;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.ListTerms;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.u.IUnifier;

public final class NilPattern extends Pattern {
    private static final long serialVersionUID = 1L;

    NilPattern(IAttachments attachments) {
        super(attachments);
    }

    @Override public Set<ITermVar> getVars() {
        return CapsuleUtil.immutableSet();
    }

    @Override public boolean isConstructed() {
        return true;
    }

    @Override protected boolean matchTerm(ITerm term, ISubstitution.Transient subst, IUnifier.Immutable unifier,
            Eqs eqs) {
        // @formatter:off
        return M.list(listTerm -> {
            return listTerm.match(ListTerms.<Boolean>cases()
                .nil(nilTerm -> {
                    return true;
                }).var(v -> {
                    eqs.add(v, this);
                    return true;
                }).otherwise(t -> {
                    return false;
                })
            );
        }).match(unifier.findTerm(term)).orElse(false);
        // @formatter:on
    }

    @Override public NilPattern apply(IRenaming subst) {
        return this;
    }

    @Override public Pattern eliminateWld(Function0<ITermVar> fresh) {
        return this;
    }

    @Override protected ITerm asTerm(Action2<ITermVar, ITerm> equalities,
            Function1<Optional<ITermVar>, ITermVar> fresh) {
        return B.newNil(getAttachments());
    }

    @Override public String toString() {
        return "[]";
    }

    @Override public boolean equals(Object o) {
        if(this == o)
            return true;
        return o != null && getClass() == o.getClass();
    }

    @Override public int hashCode() {
        return NilPattern.class.hashCode();
    }
}
