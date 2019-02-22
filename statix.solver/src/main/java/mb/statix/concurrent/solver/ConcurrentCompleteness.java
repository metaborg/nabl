package mb.statix.concurrent.solver;

import java.util.Collection;
import java.util.Set;

import org.metaborg.util.functions.Predicate2;
import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.Sets;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.solver.Completeness;
import mb.statix.solver.IConstraint;
import mb.statix.solver.State;

/**
 * Concurrent, mutable version of {@link Completeness}. This version adheres to the original
 * signatures, but is mutable.
 */
public class ConcurrentCompleteness extends Completeness {
    private final Set<IConstraint> incomplete = Sets.newConcurrentHashSet();
    private volatile Completeness frozen;

    public ConcurrentCompleteness() {
        super();
    }

    public ConcurrentCompleteness(Collection<IConstraint> incomplete) {
        this.incomplete.addAll(incomplete);
    }

    @Override
    public boolean isComplete(ITerm scope, ITerm label, State state) {
        final IUnifier unifier = state.unifier();
        final Predicate2<ITerm, ITerm> equal = (t1, t2) -> {
            return t2.equals(label) && unifier.areEqual(t1, scope).orElse(false /* (1) */);
            /* (1) This assumes well-formed constraints and specifications,
             * which guarantee us that a non-ground scope variable is never
             * instantiated to an already known scope.
             */
        };
        return incomplete.stream().flatMap(c -> Iterables2.stream(c.criticalEdges(state.spec())))
                .noneMatch(sl -> equal.test(sl.scope(), sl.label()));
    }

    @Override
    public ConcurrentCompleteness add(IConstraint constraint) {
        incomplete.add(constraint);
        frozen = null;
        return this;
    }
    
    @Override
    public ConcurrentCompleteness addAll(Iterable<IConstraint> constraints) {
        for (IConstraint constraint : constraints) {
            incomplete.add(constraint);
            frozen = null;
        }
        return this;
    }
    
    public ConcurrentCompleteness addAll(Collection<IConstraint> constraints) {
        incomplete.addAll(constraints);
        frozen = null;
        return this;
    }

    @Override
    public ConcurrentCompleteness remove(IConstraint constraint) {
        incomplete.remove(constraint);
        frozen = null;
        return this;
    }

    @Override
    public ConcurrentCompleteness removeAll(Iterable<IConstraint> constraints) {
        for (IConstraint constraint : constraints) {
            incomplete.remove(constraint);
            frozen = null;
        }
        return this;
    }
    
    public ConcurrentCompleteness removeAll(Collection<IConstraint> constraints) {
        incomplete.removeAll(constraints);
        frozen = null;
        return this;
    }
    
    /**
     * @return
     *      a copy of this mutable completeness
     */
    public ConcurrentCompleteness copy() {
        return new ConcurrentCompleteness(incomplete);
    }
    
    /**
     * @return
     *      an immutable version of this mutable completeness
     */
    public Completeness freeze() {
        Completeness c = frozen;
        if (c != null) return c;
        
        synchronized (this) {
            c = frozen;
            if (c != null) return c;
            return frozen = new Completeness().addAll(incomplete);
        }
    }
    
    /**
     * @param completeness
     *      immutable completeness
     * @return
     *      a new concurrent, mutable completeness with the same state as the given completeness
     */
    public static ConcurrentCompleteness asConcurrent(Completeness completeness) {
        return new ConcurrentCompleteness(); //TODO IMPORTANT new ConcurrentCompleteness(completeness.incomplete);
    }
}