package org.metaborg.meta.nabl2.unification;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.metaborg.meta.nabl2.terms.ITermVar;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

public class VarMultiset {

    private final IUnifier unifier;
    private final Multiset<ITermVar> vars;

    public VarMultiset(IUnifier unifier) {
        this.unifier = unifier;
        this.vars = HashMultiset.create();
    }

    public boolean add(ITermVar var) {
        return add(var, 1);
    }

    public boolean add(ITermVar var, int n) {
        final Multiset<ITermVar> reps = unifier.find(var).getVars();
        boolean change = false;
        for(ITermVar rep : reps) {
            vars.add(rep, n);
            change |= n > 0;
        }
        return change;
    }

    public boolean addAll(Collection<ITermVar> vars) {
        boolean change = false;
        for(ITermVar var : vars) {
            change |= add(var);
        }
        return change;
    }

    public boolean remove(ITermVar var) {
        return remove(var, 1);
    }

    public boolean remove(ITermVar var, int n) {
        final Multiset<ITermVar> reps = unifier.find(var).getVars();
        boolean change = false;
        for(ITermVar rep : reps) {
            int prev_n = vars.remove(rep, n);
            change |= prev_n > 0 && n > 0;
        }
        return change;
    }

    public boolean removeAll(Collection<ITermVar> vars) {
        boolean change = false;
        for(ITermVar var : vars) {
            change |= remove(var);
        }
        return change;
    }

    public boolean contains(ITermVar var) {
        return count(var) > 0;
    }

    public int count(ITermVar var) {
        final Multiset<ITermVar> reps = unifier.find(var).getVars();
        int n = 0;
        for(ITermVar rep : reps.elementSet()) {
            n += vars.count(rep);
        }
        return n;
    }

    public int size() {
        return vars.size();
    }

    public Set<ITermVar> varSet() {
        return Collections.unmodifiableSet(vars.elementSet());
    }

    public boolean update(Collection<ITermVar> vars) {
        boolean change = false;
        for(ITermVar var : vars) {
            change |= update(var);
        }
        return change;
    }

    public boolean update(final ITermVar var) {
        final int n = vars.remove(var, vars.count(var));
        if(n > 0) {
            final Multiset<ITermVar> reps = unifier.find(var).getVars();
            for(ITermVar rep : reps) {
                vars.add(rep, n);
            }
            return true;
        }
        return false;
    }

}