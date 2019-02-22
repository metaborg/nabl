package mb.statix.taico.solver;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.metaborg.util.functions.Predicate2;
import org.metaborg.util.iterators.Iterables2;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.solver.Completeness;
import mb.statix.solver.IConstraint;
import mb.statix.solver.State;

/**
 * Concurrent, mutable version of {@link Completeness}. This version adheres to the original
 * signatures, but is mutable.
 */
public class MCompleteness extends Completeness {
    private final Set<IConstraint> incomplete = new HashSet<>();
    private volatile Completeness frozen;

    public MCompleteness() {
        super();
    }

    public MCompleteness(Collection<IConstraint> incomplete) {
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
    
    public boolean isComplete(ITerm scope, ITerm label, MState state) {
        final IUnifier unifier = state.unifier();
        final Predicate2<ITerm, ITerm> equal = (t1, t2) -> {
            return t2.equals(label) && unifier.areEqual(t1, scope).orElse(false /* (1) */);
            /* (1) This assumes well-formed constraints and specifications,
             * which guarantee us that a non-ground scope variable is never
             * instantiated to an already known scope.
             */
        };
        //TODO TAICO SPEC is used for determining CRITICALEDGES
        return incomplete.stream().flatMap(c -> Iterables2.stream(c.criticalEdges(state.spec())))
                .noneMatch(sl -> equal.test(sl.scope(), sl.label()));
    }

    @Override
    public MCompleteness add(IConstraint constraint) {
        incomplete.add(constraint);
        frozen = null;
        return this;
    }
    
    @Override
    public MCompleteness addAll(Iterable<IConstraint> constraints) {
        for (IConstraint constraint : constraints) {
            incomplete.add(constraint);
            frozen = null;
        }
        return this;
    }
    
    public MCompleteness addAll(Collection<IConstraint> constraints) {
        incomplete.addAll(constraints);
        frozen = null;
        return this;
    }

    @Override
    public MCompleteness remove(IConstraint constraint) {
        incomplete.remove(constraint);
        frozen = null;
        return this;
    }

    @Override
    public MCompleteness removeAll(Iterable<IConstraint> constraints) {
        for (IConstraint constraint : constraints) {
            incomplete.remove(constraint);
            frozen = null;
        }
        return this;
    }
    
    public MCompleteness removeAll(Collection<IConstraint> constraints) {
        incomplete.removeAll(constraints);
        frozen = null;
        return this;
    }
    
    /**
     * @return
     *      a copy of this mutable completeness
     */
    public MCompleteness copy() {
        return new MCompleteness(incomplete);
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
}