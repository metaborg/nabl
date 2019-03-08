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

import mb.nabl2.scopegraph.terms.Scope;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.scopegraph.reference.CriticalEdge;
import mb.statix.solver.Completeness;
import mb.statix.solver.IConstraint;
import mb.statix.taico.module.IModule;
import mb.statix.taico.scopegraph.OwnableScope;
import mb.statix.taico.util.IOwnable;
import mb.statix.util.Capsules;

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
    public MCompleteness createChild(IModule owner) {
        MCompleteness child = new MCompleteness(this, owner);
        children.add(child);
        return child;
    }
    
    /**
     * @return
     *      a copy of this mutable completeness
     */
    public MCompleteness copy() {
        return new MCompleteness(parent, owner, incomplete);
    }
    
    @Override
    public IModule getOwner() {
        return owner;
    }
    
    public boolean isComplete(ITerm scope, ITerm label, MState state) {
        if (state.owner() != owner) {
            throw new IllegalArgumentException("isComplete request with state of " + state.owner() + " redirected to completeness of " + owner);
        }
        
        //TODO Do we need to use unifiers of other states as well?
        final IUnifier unifier = state.unifier();
        final Predicate2<ITerm, ITerm> equal = (t1, t2) -> {
            return t2.equals(label) && unifier.areEqual(t1, scope).orElse(false /* (1) */);
            /* (1) This assumes well-formed constraints and specifications,
             * which guarantee us that a non-ground scope variable is never
             * instantiated to an already known scope.
             */
        };
        
        IModule scopeOwner;
        OwnableScope oscope;
        if (scope instanceof OwnableScope) {
            System.err.println("Scope is ownableScope!");
            oscope = (OwnableScope) scope;
            scopeOwner = oscope.getOwner();
        } else if (scope instanceof IOwnable) {
            System.err.println("Scope is ownable: " + scope.getClass().getName());
            scopeOwner = ((IOwnable) scope).getOwner();
            oscope = OwnableScope.ownableMatcher(state.manager()::getModule).match(scope, unifier).orElse(null);
            if (oscope != null && oscope.getOwner() != scopeOwner) System.err.println("FATAL: Scope owner does not match iownable");
        } else {
            System.err.println("FATAL Scope is not ownable!");
            oscope = OwnableScope.ownableMatcher(state.manager()::getModule).match(scope, unifier).orElse(null);
            scopeOwner = oscope == null ? null : oscope.getOwner();
        }
        
        if (oscope == null) {
            System.err.println("Cannot turn scope " + scope + " into ownable scope via matcher!");
            return isCompleteFinal(equal);
        } else if (scopeOwner == state.owner()) {
            System.err.println("Completeness of " + owner + " got isComplete query from matching owner: " + scope);
            //Transitive
            return isCompleteFinal(equal);
        } else {
            System.err.println("Completeness of " + owner + " got isComplete query on scope owned by " + scopeOwner + ". Redirecting there");
            //TODO CONCURRENCY This is a concurrency problem, delegation to solvers
            //TODO Possible state leaking point
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
    public boolean isCompleteFinal(Predicate2<ITerm, ITerm> equal) {
        System.err.println("Completeness of " + owner + " got isCompleteFinal query");
        //Ask children
        for (MCompleteness child : children) {
            if (!child.isCompleteFinal(equal)) {
                System.err.println("Completeness of " + owner + " result: (child) false");
                return false;
            }
        }
        
        //TODO OPTIMIZATION point
        //Use passed spec instead?
        //TODO Possible state inconsistency point (uses current state of module)
        boolean tbr = incomplete.stream().flatMap(c -> Iterables2.stream(c.criticalEdges(owner.getCurrentState().spec())))
                .noneMatch(sl -> equal.test(sl.scope(), sl.label()));
        System.err.println("Completeness of " + owner + " result: " + tbr);
        return tbr;
    }

    public MCompleteness add(IConstraint constraint) {
        incomplete.add(constraint);
        return this;
    }
    
    public MCompleteness addAll(Iterable<IConstraint> constraints) {
        for (IConstraint constraint : constraints) {
            incomplete.add(constraint);
        }
        return this;
    }
    
    public MCompleteness addAll(Collection<IConstraint> constraints) {
        incomplete.addAll(constraints);
        return this;
    }

    public MCompleteness remove(IConstraint constraint) {
        incomplete.remove(constraint);
        return this;
    }

    public MCompleteness removeAll(Iterable<IConstraint> constraints) {
        for (IConstraint constraint : constraints) {
            incomplete.remove(constraint);
        }
        return this;
    }
    
    public MCompleteness removeAll(Collection<IConstraint> constraints) {
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
    
    public static List<CriticalEdge> criticalEdges(IConstraint constraint, MState state) {
        return constraint.criticalEdges(state.spec()).stream().flatMap(ce -> {
            final Optional<CriticalEdge> edge =
                    Scope.matcher().match(ce.scope(), state.unifier()).map(s -> CriticalEdge.of(s, ce.label()));
            return Optionals.stream(edge);
        }).collect(Collectors.toList());
    }
}