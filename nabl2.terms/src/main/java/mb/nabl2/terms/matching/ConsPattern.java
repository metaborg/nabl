package mb.nabl2.terms.matching;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Set;

import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.ListTerms;
import mb.nabl2.terms.substitution.ISubstitution.Transient;
import mb.nabl2.terms.unification.IUnifier;

class ConsPattern extends Pattern {
    private static final long serialVersionUID = 1L;

    private final Pattern head;
    private final Pattern tail;

    public ConsPattern(Pattern head, Pattern tail) {
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

    @Override protected MaybeNotInstantiatedBool matchTerm(ITerm term, Transient subst, IUnifier unifier) {
        // @formatter:off
        return M.list(listTerm -> {
            return listTerm.match(ListTerms.<MaybeNotInstantiatedBool>cases()
                .cons(consTerm -> {
                    return matchTerms(Iterables2.from(head, tail),
                            Iterables2.from(consTerm.getHead(), consTerm.getTail()), subst, unifier);
                }).var(v -> {
                    return MaybeNotInstantiatedBool.ofNotInstantiated(v);
                }).otherwise(t -> {
                    return MaybeNotInstantiatedBool.ofResult(false);
                })
            );
        }).match(unifier.findTerm(term)).orElse(MaybeNotInstantiatedBool.ofResult(false));
    }

    @Override public ITerm asTerm(ImmutableMultimap.Builder<ITermVar, ITerm> equalities) {
        return B.newCons(head.asTerm(equalities), (IListTerm)tail.asTerm(equalities));
    }

    @Override public String toString() {
        return "[" + head.toString() + "|" + tail.toString() + "]";
    }

}