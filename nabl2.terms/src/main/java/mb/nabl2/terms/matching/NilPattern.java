package mb.nabl2.terms.matching;

import java.util.Optional;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.functions.Action2;
import org.metaborg.util.functions.Function0;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.iterators.Iterables2;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.IAttachments;
import mb.nabl2.terms.IConsTerm;
import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.INilTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.ListTerms;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.Unifiers;
import mb.nabl2.terms.unification.u.IUnifier;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

class NilPattern extends Pattern {
    private static final long serialVersionUID = 1L;

    public NilPattern(IAttachments attachments) {
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
        ITerm subj = Unifiers.Immutable.of().findTerm(unifier.findTerm(term));
        if(subj instanceof IListTerm) {
            final IListTerm list = (IListTerm) subj;
            switch(list.listTermTag()) {
                case IConsTerm: {
                    return false;
                }

                case INilTerm: {
                    return true;
                }

                case ITermVar: {
                    eqs.add((ITermVar) list, this);
                    return true;
                }
            }
            // N.B. don't use this in default case branch, instead use IDE to catch non-exhaustive switch statements
            throw new RuntimeException("Missing case for IListTerm subclass/tag");
        } else {
            return false;
        }
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
