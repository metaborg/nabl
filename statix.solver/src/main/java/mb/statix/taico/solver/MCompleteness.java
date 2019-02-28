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
        final IUnifier unifier = state.unifier();
        final Predicate2<ITerm, ITerm> equal = (t1, t2) -> {
            return t2.equals(label) && unifier.areEqual(t1, scope).orElse(false /* (1) */);
            /* (1) This assumes well-formed constraints and specifications,
             * which guarantee us that a non-ground scope variable is never
             * instantiated to an already known scope.
             */
        };
        
        if (scope instanceof IOwnable) {
            System.err.println("Scope is ownable!");
        }
        
        OwnableScope oscope = OwnableScope.ownableMatcher(state.manager()::getModule).match(scope, unifier).orElse(null);
        if (oscope == null) {
            System.err.println("Cannot turn scope " + scope + " into ownable scope via matcher!");
        } else if (oscope.getOwner() == state.owner()) {
            //Transitive
            oscope.getOwner();
        } else {
            //TODO CONCURRENCY This is a concurrency problem, delegation to solvers
            oscope.getOwner().getCurrentState().solver().getCompleteness().isComplete(oscope, label, state);
        }
        
        //TODO TAICO SPEC is used for determining CRITICALEDGES
        return incomplete.stream().flatMap(c -> Iterables2.stream(c.criticalEdges(state.spec())))
                .noneMatch(sl -> equal.test(sl.scope(), sl.label()));
    }
    
    public boolean isCompleteFinal(ITerm scope, ITerm label, MState state, IUnifier unifier) {
        
    }

    public MCompleteness add(IConstraint constraint) {
        incomplete.add(constraint);
        frozen = null;
        return this;
    }
    
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

    public MCompleteness remove(IConstraint constraint) {
        incomplete.remove(constraint);
        frozen = null;
        return this;
    }

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
}