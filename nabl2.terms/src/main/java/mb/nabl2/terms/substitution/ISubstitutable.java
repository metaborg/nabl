package mb.nabl2.terms.substitution;

import java.util.Map.Entry;
import java.util.Set;

import mb.nabl2.terms.ITermVar;

public interface ISubstitutable<T> {

    Set<ITermVar> boundVars();

    Set<ITermVar> freeVars();

    /**
     * Capture-avoiding substitution.
     */
    default T substitute(ISubstitution.Immutable subst) {
        final ISubstitution.Transient totalSubst = subst.melt();
        for(ITermVar var : freeVars()) {
            if(!totalSubst.contains(var)) {
                totalSubst.put(var, var);
            }
        }
        return recSubstitute(totalSubst.freeze());
    }

    /**
     * Implements the actual substitution. The substitution argument is a substitution that is safe to apply under the
     * binder. The implementation should call {@link #recSubstitute} to recurse on sub-terms that implement
     * {@link ISubstitutable}.
     * 
     * This method should not be called directly.
     */
    T doSubstitute(IRenaming.Immutable localRenaming, ISubstitution.Immutable totalSubst);

    /**
     * Recursion point for use in {@link #doSubstitute} implementations.
     * 
     * This method not be called directly, except in {@link #doSubstitute}.
     */
    default T recSubstitute(ISubstitution.Immutable totalSubst) {
        final Set<ITermVar> boundVars = boundVars();
        if(boundVars.isEmpty()) {
            return doSubstitute(PersistentRenaming.Immutable.of(), totalSubst);
        }
        final ISubstitution.Transient newTotalSubst = totalSubst.melt();
        newTotalSubst.removeAll(boundVars);
        final FreshVars fresh = new FreshVars(newTotalSubst.freeVarSet());
        final IRenaming.Immutable localRenaming = fresh.fresh(boundVars);
        for(Entry<ITermVar, ITermVar> entry : localRenaming.entrySet()) {
            newTotalSubst.put(entry.getKey(), entry.getValue());
        }
        return doSubstitute(localRenaming, newTotalSubst.freeze());

    }

}