package mb.nabl2.terms.matching;

import java.util.Objects;
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
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.Unifiers;
import mb.nabl2.terms.unification.u.IUnifier;
import mb.nabl2.terms.unification.ud.IUniDisunifier;

import static mb.nabl2.terms.build.TermBuild.B;

class ConsPattern extends Pattern {
    private static final long serialVersionUID = 1L;

    private final Pattern head;
    private final Pattern tail;

    public ConsPattern(Pattern head, Pattern tail, IAttachments attachments) {
        super(attachments);
        this.head = head;
        this.tail = tail;
    }

    public Pattern getHead() {
        return head;
    }

    public Pattern getTail() {
        return tail;
    }

    @Override public Set<ITermVar> getVars() {
        Set.Transient<ITermVar> vars = CapsuleUtil.transientSet();
        vars.__insertAll(head.getVars());
        vars.__insertAll(tail.getVars());
        return vars.freeze();
    }

    @Override public boolean isConstructed() {
        return true;
    }

    @Override protected boolean matchTerm(ITerm term,
            ISubstitution.Transient subst, IUnifier.Immutable unifier, Eqs eqs) {
        ITerm subj = Unifiers.Immutable.of().findTerm(unifier.findTerm(term));
        if(subj instanceof IListTerm) {
            final IListTerm list = (IListTerm) subj;
            switch(list.listTermTag()) {
                case IConsTerm: {
                    IConsTerm consTerm = (IConsTerm) list;
                    return matchTerms(Iterables2.from(head, tail),
                        Iterables2.from(consTerm.getHead(), consTerm.getTail()), subst, unifier,
                        eqs);
                }

                case INilTerm: {
                    return false;
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

    @Override public ConsPattern apply(IRenaming subst) {
        return new ConsPattern(head.apply(subst), tail.apply(subst), getAttachments());
    }

    @Override public ConsPattern eliminateWld(Function0<ITermVar> fresh) {
        return new ConsPattern(head.eliminateWld(fresh), tail.eliminateWld(fresh), getAttachments());
    }

    @Override protected ITerm asTerm(Action2<ITermVar, ITerm> equalities,
            Function1<Optional<ITermVar>, ITermVar> fresh) {
        return B.newCons(head.asTerm(equalities, fresh), (IListTerm) tail.asTerm(equalities, fresh), getAttachments());
    }

    @Override public String toString() {
        return "[" + head.toString() + "|" + tail.toString() + "]";
    }

    @Override public boolean equals(Object o) {
        if(this == o)
            return true;
        if(o == null || getClass() != o.getClass())
            return false;
        ConsPattern that = (ConsPattern) o;
        return Objects.equals(head, that.head) && Objects.equals(tail, that.tail);
    }

    @Override public int hashCode() {
        return Objects.hash(head, tail);
    }
}
