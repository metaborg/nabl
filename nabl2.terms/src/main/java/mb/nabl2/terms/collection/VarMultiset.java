package mb.nabl2.terms.collection;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.u.IUnifier;

public class VarMultiset {

    private final Multiset<ITermVar> vars;

    public VarMultiset() {
        this.vars = HashMultiset.create();
    }

    public boolean add(ITermVar var, IUnifier unifier) {
        return add(var, 1, unifier);
    }

    public boolean add(ITermVar var, int n, IUnifier unifier) {
        final Multiset<ITermVar> reps = unifier.findRecursive(var).getVars();
        boolean change = false;
        for(ITermVar rep : reps) {
            vars.add(rep, n);
            change |= n > 0;
        }
        return change;
    }

    public boolean addAll(Collection<ITermVar> vars, IUnifier unifier) {
        boolean change = false;
        for(ITermVar var : vars) {
            change |= add(var, unifier);
        }
        return change;
    }

    public boolean remove(ITermVar var, IUnifier unifier) {
        return remove(var, 1, unifier);
    }

    public boolean remove(ITermVar var, int n, IUnifier unifier) {
        final Multiset<ITermVar> reps = unifier.findRecursive(var).getVars();
        boolean change = false;
        for(ITermVar rep : reps) {
            int prev_n = vars.remove(rep, n);
            change |= prev_n > 0 && n > 0;
        }
        return change;
    }

    public boolean removeAll(Collection<ITermVar> vars, IUnifier unifier) {
        boolean change = false;
        for(ITermVar var : vars) {
            change |= remove(var, unifier);
        }
        return change;
    }

    public boolean contains(ITermVar var, IUnifier unifier) {
        return count(var, unifier) > 0;
    }

    public int count(ITermVar var, IUnifier unifier) {
        final Multiset<ITermVar> reps = unifier.findRecursive(var).getVars();
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

    public boolean update(Collection<ITermVar> vars, IUnifier unifier) {
        boolean change = false;
        for(ITermVar var : vars) {
            change |= update(var, unifier);
        }
        return change;
    }

    public boolean update(final ITermVar var, IUnifier unifier) {
        final int n = vars.remove(var, vars.count(var));
        if(n > 0) {
            final Multiset<ITermVar> reps = unifier.findRecursive(var).getVars();
            for(ITermVar rep : reps) {
                vars.add(rep, n);
            }
            return true;
        }
        return false;
    }

}