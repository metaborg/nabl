package mb.statix.modular.observers;

import java.util.Set;

import com.google.common.collect.SetMultimap;

import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.modular.util.TDebug;

/**
 * Interface which contains most of the logic for implementing an observer mechanism for variable
 * groundness.
 * <p>
 * Implementing classes only have to implement {@link #getTarget}, to redirect the request to the
 * correct target.
 */
public interface VarCompleteManager {
    /**
     * @return
     *      the (mutable) multimap with the observers
     */
    public SetMultimap<ITermVar, VarCompleteObserver> observers();
    
    /**
     * Registers the given observer to be called whenever the given edge is resolved.
     * The registration is automatically redirected to the correct {@link VarCompleteManager}.
     * <p>
     * If the given critical edge has already been resolved, the given observer is called
     * immediately.
     * 
     * @param var
     *      the variable
     * @param unifier
     *      the unifier
     * @param observer
     *      the observer to call whenever the variable becomes ground
     * 
     * @return
     *      true if registered, false if the variable is already ground and the observer was
     *      called directly
     */
    public default boolean registerObserver(ITermVar var, IUnifier unifier, VarCompleteObserver observer) {
        VarCompleteManager target = getTarget(var);
        return target == null || target._registerObserver(var, unifier, observer);
    }
    
    /**
     * NOTE: This method does <b>not</b> redirect to the correct {@link VarCompleteManager}, and
     * should not be called from any class other than the extending class.
     * <p>
     * Registers the given observer to be called whenever the given variable becomes ground. If the
     * given variable is already ground, the given observer is called immediately.
     * 
     * @param var
     *      the variable
     * @param unifier
     *      the unifier
     * @param observer
     *      the observer to call
     * 
     * @return
     *      false if the variable was already ground, true if the registration was made
     */
    public default boolean _registerObserver(ITermVar var, IUnifier unifier, VarCompleteObserver observer) {
        if (unifier.isGround(var)) {
            observer.accept(var);
            return false;
        }
        
        synchronized (observers()) {
            observers().put(var, observer);
        }
        return true;
    }
    
    /**
     * Activates all the observers for the given variable.
     * 
     * @param var
     *      the variable to activate
     */
    public default void activateObservers(ITermVar var) {
        if (TDebug.COMPLETENESS) System.out.println("Activating variable " + var);
        
        Set<VarCompleteObserver> observers;
        synchronized (observers()) {
            observers = observers().removeAll(var);
        }
        
        for (VarCompleteObserver observer : observers) {
            observer.accept(var);
        }
    }
    
    /**
     * Gets the {@link VarCompleteManager} where this request should be redirected.
     * 
     * @param var
     *      the variable
     * 
     * @return
     *      the target, or null if there is no target for this variable
     */
    public VarCompleteManager getTarget(ITermVar var);
}
