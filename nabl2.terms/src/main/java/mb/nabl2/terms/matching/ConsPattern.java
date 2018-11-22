package mb.nabl2.terms.matching;

import static mb.nabl2.terms.matching.CheckedTermMatch.CM;

import java.util.Set;

import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.ISubstitution.Transient;
import mb.nabl2.terms.unification.IUnifier;

class ConsPattern extends Pattern {

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

    @Override protected boolean matchTerm(ITerm term, Transient subst, IUnifier unifier)
            throws InsufficientInstantiationException {
        // @formatter:off
        return CM.<Boolean, InsufficientInstantiationException>cases(
            CM.cons(consTerm -> {
                if(matchTerms(Iterables2.from(head, tail), Iterables2.from(consTerm.getHead(), consTerm.getTail()), subst,
                        unifier)) {
                    return true;
                } else {
                    return false;
                }
            }),
            CM.var(v -> {
                throw new InsufficientInstantiationException(v);
            })
        ).matchOrThrow(term, unifier).orElse(false);
        // @formatter:on
    }

    @Override public String toString() {
        return "[" + head.toString() + "|" + tail.toString() + "]";
    }

}