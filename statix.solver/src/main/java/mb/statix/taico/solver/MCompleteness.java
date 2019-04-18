package mb.statix.taico.solver;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.metaborg.util.functions.Predicate2;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.optionals.Optionals;

import com.google.common.collect.Iterables;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.scopegraph.reference.CriticalEdge;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.Completeness;
import mb.statix.solver.IConstraint;
import mb.statix.taico.module.IModule;
import mb.statix.taico.util.IOwnable;
import mb.statix.util.Capsules;

/**
 * Concurrent, mutable version of {@link Completeness}. This version adheres to the original
 * signatures, but is mutable.
 */
public class MCompleteness implements IOwnable {
    private final IModule owner;
    private final Set<IConstraint> incomplete = new HashSet<>();

    public MCompleteness(IModule owner) {
        this.owner = owner;
    }
    
    public MCompleteness(IModule owner, Iterable<IConstraint> incomplete) {
        this.owner = owner;
        Iterables.addAll(this.incomplete, incomplete);
    }
    
    public static MCompleteness topLevelCompleteness(IModule owner) {
        return new MCompleteness(owner);
    }
    
    /**
     * @return
     *      a copy of this mutable completeness
     */
    @Deprecated
    public synchronized MCompleteness copy() {
        return new MCompleteness(owner, incomplete);
    }
    
    @Override
    public IModule getOwner() {
        return owner;
    }
    
    public CompletenessResult isComplete(ITerm scope, ITerm label, IMState state) {
        final IUnifier unifier = state.unifier();
        final Predicate2<ITerm, ITerm> equal = (t1, t2) -> {
            return t2.equals(label) && unifier.areEqual(t1, scope).orElse(false /* (1) */);
            /* (1) This assumes well-formed constraints and specifications,
             * which guarantee us that a non-ground scope variable is never
             * instantiated to an already known scope.
             */
        };
        
        boolean complete;
        synchronized (this) {
            complete = incomplete.stream()
                    .flatMap(c -> Iterables2.stream(Completeness.criticalEdges(c, owner.getCurrentState().spec())))
                    .noneMatch(sl -> equal.test(sl.scope(), sl.label()));
        }
        return CompletenessResult.of(complete, owner);
    }

    public synchronized MCompleteness add(IConstraint constraint) {
        incomplete.add(constraint);
        return this;
    }
    
    public synchronized MCompleteness addAll(Iterable<IConstraint> constraints) {
        Iterables.addAll(incomplete, constraints);
        return this;
    }

    public synchronized MCompleteness remove(IConstraint constraint) {
        incomplete.remove(constraint);
        return this;
    }

    public synchronized MCompleteness removeAll(Iterable<IConstraint> constraints) {
        for (IConstraint constraint : constraints) {
            incomplete.remove(constraint);
        }
        return this;
    }
    
    public synchronized MCompleteness removeAll(Collection<IConstraint> constraints) {
        incomplete.removeAll(constraints);
        return this;
    }
    
    /**
     * @deprecated This function does not keep the behavior of the MCompleteness.
     * 
     * Converts this module completeness to a normal completeness.
     * 
     * @return
     *      a new completeness 
     */
    @Deprecated
    public Completeness toCompleteness() {
        return new Completeness(Capsules.newSet(incomplete));
    }
    
    public static List<CriticalEdge> criticalEdges(IConstraint constraint, IMState state) {
        return Completeness.criticalEdges(constraint, state.spec()).stream().flatMap(ce -> {
            final Optional<CriticalEdge> edge =
                    Scope.matcher()
                        .match(ce.scope(), state.unifier())
                        .map(s -> CriticalEdge.of(s, ce.label(), state.owner()));
            return Optionals.stream(edge);
        }).collect(Collectors.toList());
    }
}