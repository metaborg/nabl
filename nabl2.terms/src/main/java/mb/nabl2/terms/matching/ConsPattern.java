package mb.nabl2.terms.matching;

import static mb.nabl2.terms.matching.CheckedTermMatch.CM;

import java.util.Optional;
import java.util.Set;

import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.IConsTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.ListTerms;
import mb.nabl2.terms.matching.CheckedTermMatch.ICheckedMatcher;
import mb.nabl2.terms.substitution.ISubstitution.Transient;
import mb.nabl2.terms.unification.IUnifier;

class ConsPattern extends Pattern {

    private final Pattern head;
    private final Pattern tail;
    private final ICheckedMatcher<IConsTerm, InsufficientInstantiationException> matcher;

    public ConsPattern(Pattern head, Pattern tail) {
        this.head = head;
        this.tail = tail;
        // @formatter:off
        this.matcher = CM.list(ListTerms.<Optional<IConsTerm>, InsufficientInstantiationException>checkedCases()
                .cons(consTerm -> {
                    return Optional.of(consTerm);
                }).var(v -> {
                    throw new InsufficientInstantiationException(v);
                }).otherwise(t -> {
                    return Optional.empty();
                }));
        // @formatter:on
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
        final Optional<IConsTerm> consTerm = matcher.matchOrThrow(term, unifier);
        if(!consTerm.isPresent()) {
            return false;
        }
        return matchTerms(Iterables2.from(head, tail),
                Iterables2.from(consTerm.get().getHead(), consTerm.get().getTail()), subst, unifier);
    }

    @Override public String toString() {
        return "[" + head.toString() + "|" + tail.toString() + "]";
    }

}