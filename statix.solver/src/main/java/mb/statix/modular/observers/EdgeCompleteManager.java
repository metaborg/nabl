package mb.statix.modular.observers;

import java.util.Set;

import com.google.common.collect.SetMultimap;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.modular.util.TDebug;
import mb.statix.scopegraph.reference.CriticalEdge;
import mb.statix.scopegraph.terms.Scope;

/**
 * Interface which contains most of the logic for implementing an observer mechanism for edge
 * completeness.
 * <p>
 * Implementing classes only have to implement {@link #getTarget}, to redirect the request to the
 * correct target, and {@link #alreadyResolved}, which should check if the edge has already been
 * resolved.
 */
public interface EdgeCompleteManager {
    /**
     * @return
     *      the (mutable) multimap with the observers
     */
    public SetMultimap<CriticalEdge, EdgeCompleteObserver> observers();
    
    /**
     * Registers the given observer to be called whenever the given edge is resolved.
     * The registration is automatically redirected to the correct {@link EdgeCompleteManager}.
     * <p>
     * If the given critical edge has already been resolved, the given observer is called
     * immediately.
     * 
     * @param edge
     *      the edge to register for
     * @param unifier
     *      the unifier
     * @param observer
     *      the observer to call whenever the edge is resolved
     * 
     * @return
     *      true if registered, false if the critical edge is already resolved and the observer was
     *      called directly
     */
    public default boolean registerObserver(ITerm scope, ITerm label, IUnifier unifier, EdgeCompleteObserver observer) {
        Scope scopeTerm;
        if (scope instanceof Scope) {
            scopeTerm = (Scope) scope;
        } else {
            scopeTerm = Scope.matcher().match(scope, unifier)
                    .orElseThrow(() -> new UnsupportedOperationException("Cannot observe a critical edge without an actual scope, for " + scope + "-" + label));
        }
        
        EdgeCompleteManager target = getTarget(scopeTerm);
        return target._registerObserver(scopeTerm, label, observer);
    }
    
    /**
     * NOTE: This method does <b>not</b> redirect to the correct {@link EdgeCompleteManager}, and
     * should not be called from any class other than the extending class.
     * <p>
     * Registers the given observer to be called whenever the given edge is resolved. If the given
     * critical edge has already been resolved, the given observer is called immediately.
     * 
     * @param scope
     *      the scope of the edge
     * @param label
     *      the label of the edge
     * @param observer
     *      the observer to call
     * 
     * @return
     *      false if the edge was already resolved, true if the registration was made
     */
    public default boolean _registerObserver(Scope scope, ITerm label, EdgeCompleteObserver observer) {
        final CriticalEdge edge = CriticalEdge.of(scope, label);
        if (alreadyResolved(scope, label)) {
            observer.accept(edge);
            return false;
        }
        
        sync: synchronized (observers()) {
            //We need to check again once we have the lock to prevent us from missing the event
            if (alreadyResolved(scope, label)) break sync;
            
            observers().put(edge, observer);
            return true;
        }
        
        observer.accept(edge);
        return false;
    }
    
    /**
     * Activates all the observers for the given critical edge.
     * 
     * @param edge
     *      the edge to activate
     */
    public default void activateObservers(CriticalEdge edge) {
        if (TDebug.COMPLETENESS) TDebug.DEV_OUT.info("Activating edge " + edge);
        
        Set<EdgeCompleteObserver> observers;
        synchronized (observers()) {
            observers = observers().removeAll(edge);
        }
        
        for (EdgeCompleteObserver observer : observers) {
            observer.accept(edge);
        }
    }
    
    /**
     * Gets the {@link EdgeCompleteManager} where this request should be redirected.
     * 
     * @param scope
     *      the scope of the edge
     * 
     * @return
     *      the target
     */
    public EdgeCompleteManager getTarget(ITerm scope);
    
    /**
     * Called to check if the edge has already been resolved.
     * 
     * @param scope
     *      the scope of the edge
     * @param term
     *      the label of the edge
     * 
     * @return
     *      true if the edge is resolved, false otherwise
     */
    public boolean alreadyResolved(Scope scope, ITerm label);
}
