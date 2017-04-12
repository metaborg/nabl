package org.metaborg.meta.nabl2.unification;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.terms.Terms;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

public final class VarTracker<D> implements Serializable {
    private static final long serialVersionUID = 42L;

    private final SetMultimap<ITermVar, D> activeVars;
    private final Set<ITermVar> frozenVars;
    private final IUnifier unifier;

    public VarTracker(IUnifier unifier) {
        this.activeVars = HashMultimap.create();
        this.frozenVars = Sets.newHashSet();
        this.unifier = unifier;
    }

    /**
     * Test if any unlocked variables in term are in the active set.
     */
    public boolean isActive(ITerm term, @SuppressWarnings("unchecked") D... excludedDeps) {
        Set<D> excludedDepList = Sets.newHashSet(excludedDeps);
        for(ITermVar var : Terms.unlockedVars(unifier.find(term))) {
            for(D dep : activeVars.get(var)) {
                if(!excludedDepList.contains(dep)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Add variables in term to active set.
     */
    public void addActive(ITerm term, D dep) {
        for(ITermVar var : Terms.unlockedVars(unifier.find(term))) {
            if(frozenVars.contains(var)) {
                throw new IllegalArgumentException("Re-activating frozen " + var);
            }
            activeVars.put(var, dep);
        }
    }

    /**
     * Remove variables in term from active set.
     */
    public void removeActive(ITerm term, D dep) {
        for(ITermVar var : Terms.unlockedVars(unifier.find(term))) {
            activeVars.remove(var, dep);
        }
    }

    public Set<ITermVar> getActiveVars() {
        return Collections.unmodifiableSet(activeVars.keySet());
    }

    public void updateActive(Set<ITermVar> substituted) {
        for(ITermVar var : substituted) {
            updateActive(var);
        }
    }

    private void updateActive(ITermVar var) {
        if(frozenVars.contains(var)) {
            throw new IllegalArgumentException("Updating frozen " + var);
        }
        if(!activeVars.containsKey(var)) {
            return;
        }
        final Set<D> n = activeVars.get(var);
        for(ITermVar newVar : Terms.unlockedVars(unifier.find(var))) {
            if(frozenVars.contains(newVar)) {
                throw new IllegalArgumentException("Re-activating frozen " + newVar + " on update of " + var);
            }
            activeVars.putAll(newVar, n);
        }
        activeVars.removeAll(var);
    }

    public void freeze(ITerm term) {
        for(ITermVar var : Terms.unlockedVars(unifier.find(term))) {
            if(activeVars.containsKey(var)) {
                throw new IllegalArgumentException("Freezing active " + var);
            }
            frozenVars.add(var);
        }
    }

}