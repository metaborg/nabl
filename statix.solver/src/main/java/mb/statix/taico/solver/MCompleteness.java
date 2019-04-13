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

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.scopegraph.reference.CriticalEdge;
import mb.statix.solver.IConstraint;
import mb.statix.taico.module.IModule;
import mb.statix.taico.scopegraph.OwnableScope;
import mb.statix.taico.util.IOwnable;

/**
 * Concurrent, mutable version of {@link Completeness}. This version adheres to the original
 * signatures, but is mutable.
 */
public class MCompleteness implements IOwnable {
    private final IModule owner;
    private final MCompleteness parent;
    private final Set<MCompleteness> children = new HashSet<>();
    private final Set<IConstraint> incomplete = new HashSet<>();

    private MCompleteness(MCompleteness parent, IModule owner) {
        this.parent = parent;
        this.owner = owner;
    }
    
    private MCompleteness(MCompleteness parent, IModule owner, Collection<IConstraint> incomplete) {
        this.parent = parent;
        this.owner = owner;
        this.incomplete.addAll(incomplete);
    }
    
    public static MCompleteness topLevelCompleteness(IModule owner) {
        return new MCompleteness(null, owner);
    }
    
    /**
     * Creates a child completeness.
     * 
     * @param owner
     *      the owner of the new completeness
     * 
     * @return
     *      the newly created completeness
     */
    public synchronized MCompleteness createChild(IModule owner) {
        MCompleteness child = new MCompleteness(this, owner);
        children.add(child);
        return child;
    }
    
    /**
     * @return
     *      a copy of this mutable completeness
     */
    public synchronized MCompleteness copy() {
        return new MCompleteness(parent, owner, incomplete);
    }
    
    @Override
    public IModule getOwner() {
        return owner;
    }
    
    public CompletenessResult isComplete(ITerm scope, ITerm label, MState state) {
        if (state.owner() != owner) {
            throw new IllegalArgumentException("isComplete request with state of " + state.owner() + " redirected to completeness of " + owner);
        }
        
        final IUnifier unifier = state.unifier();
        final Predicate2<ITerm, ITerm> equal = (t1, t2) -> {
            return t2.equals(label) && unifier.areEqual(t1, scope).orElse(false /* (1) */);
            /* (1) This assumes well-formed constraints and specifications,
             * which guarantee us that a non-ground scope variable is never
             * instantiated to an already known scope.
             */
        };
        
        IModule scopeOwner;
        if (scope instanceof IOwnable) {
            scopeOwner = ((IOwnable) scope).getOwner();
        } else {
            OwnableScope oscope = OwnableScope.ownableMatcher(state.manager()::getModule).match(scope, unifier)
                    .orElseThrow(() -> new IllegalArgumentException("Scope " + scope + " is not an ownable scope!"));
            scopeOwner = oscope.getOwner();
        }
        
        if (scopeOwner == state.owner()) {
            System.err.println("Completeness of " + owner + " got isComplete query from matching owner: " + scope);
            return isCompleteFinal(equal);
        } else {
            System.err.println("Completeness of " + owner + " got isComplete query on scope owned by " + scopeOwner + ". Redirecting there");
            //TODO Static state access
            MCompleteness target = scopeOwner.getCurrentState().solver().getCompleteness();
            return target.isCompleteFinal(equal);
        }
    }
    
    /**
     * Determines if the terms matching the given predicate are complete according to the view of
     * this completeness.
     * 
     * @param equal
     *      the predicate to determine which terms we should match 
     * @return
     *      true if these terms are complete, false otherwise 
     */
    public CompletenessResult isCompleteFinal(Predicate2<ITerm, ITerm> equal) {
        //Ask ourselves
        //TODO OPTIMIZATION point: Use passed spec instead?
        //TODO Static state access
        boolean complete;
        synchronized (this) {
            complete = incomplete.stream().flatMap(c -> Iterables2.stream(c.criticalEdges(owner.getCurrentState().spec())))
                    .noneMatch(sl -> equal.test(sl.scope(), sl.label()));
        }
        System.err.println("Completeness of " + owner + " result: " + complete);
        if (!complete) return CompletenessResult.of(false, owner);

        //Ask children
        for (MCompleteness child : children) {
            CompletenessResult childResult = child.isCompleteFinal(equal);
            if (!childResult.isComplete()) {
                System.err.println("Completeness of " + owner + " result: (child) false");
                return childResult;
            }
        }

        return CompletenessResult.of(true, owner);
    }

    public synchronized MCompleteness add(IConstraint constraint) {
        incomplete.add(constraint);
        return this;
    }
    
    public synchronized MCompleteness addAll(Iterable<IConstraint> constraints) {
        for (IConstraint constraint : constraints) {
            incomplete.add(constraint);
        }
        return this;
    }
    
    public synchronized MCompleteness addAll(Collection<IConstraint> constraints) {
        incomplete.addAll(constraints);
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
    
    public static List<CriticalEdge> criticalEdges(IConstraint constraint, MState state) {
        return constraint.criticalEdges(state.spec()).stream().flatMap(ce -> {
            final Optional<CriticalEdge> edge =
                    OwnableScope.ownableMatcher(state.manager()::getModule)
                        .match(ce.scope(), state.unifier())
                        .map(s -> CriticalEdge.of(s, ce.label(), state.owner()));
            return Optionals.stream(edge);
        }).collect(Collectors.toList());
    }
}