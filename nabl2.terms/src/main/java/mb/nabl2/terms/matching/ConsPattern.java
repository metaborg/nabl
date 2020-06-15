package mb.nabl2.terms.matching;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.metaborg.util.functions.Action2;
import org.metaborg.util.functions.Function0;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.ListTerms;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution.Transient;
import mb.nabl2.terms.unification.u.IUnifier;

class ConsPattern extends Pattern {
    private static final long serialVersionUID = 1L;

    private final Pattern head;
    private final Pattern tail;

    public ConsPattern(Pattern head, Pattern tail, ImmutableClassToInstanceMap<Object> attachments) {
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
        ImmutableSet.Builder<ITermVar> vars = ImmutableSet.builder();
        vars.addAll(head.getVars());
        vars.addAll(tail.getVars());
        return vars.build();
    }

    @Override protected boolean matchTerm(ITerm term, Transient subst, IUnifier.Immutable unifier, Eqs eqs) {
        // @formatter:off
        return M.list(listTerm -> {
            return listTerm.match(ListTerms.<Boolean>cases()
                .cons(consTerm -> {
                    return matchTerms(Iterables2.from(head, tail),
                            Iterables2.from(consTerm.getHead(), consTerm.getTail()), subst, unifier, eqs);
                }).var(v -> {
                    eqs.add(v, this);
                    return true;
                }).otherwise(t -> {
                    return false;
                })
            );
        }).match(unifier.findTerm(term)).orElse(false);
    }

    @Override public ConsPattern apply(IRenaming subst) {
        return new ConsPattern(head.apply(subst), tail.apply(subst), getAttachments());
    }
    
    @Override public ConsPattern eliminateWld(Function0<ITermVar> fresh) {
        return new ConsPattern(head.eliminateWld(fresh), tail.eliminateWld(fresh), getAttachments());
    }

    @Override
    protected ITerm asTerm(Action2<ITermVar, ITerm> equalities, Function1<Optional<ITermVar>, ITermVar> fresh) {
        return B.newCons(head.asTerm(equalities, fresh), (IListTerm)tail.asTerm(equalities, fresh), getAttachments());
    }

    @Override public String toString() {
        return "[" + head.toString() + "|" + tail.toString() + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConsPattern that = (ConsPattern) o;
        return Objects.equals(head, that.head) &&
                Objects.equals(tail, that.tail);
    }

    @Override
    public int hashCode() {
        return Objects.hash(head, tail);
    }
}