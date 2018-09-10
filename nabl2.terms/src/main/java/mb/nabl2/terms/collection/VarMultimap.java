package mb.nabl2.terms.collection;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.IUnifier;

public class VarMultimap<T> {

    private final Multimap<ITermVar, T> map;

    public VarMultimap() {
        this.map = HashMultimap.create();
    }

    public boolean put(ITermVar key, T value, IUnifier unifier) {
        final Set<ITermVar> reps = unifier.getVars(key);
        boolean change = false;
        for(ITermVar rep : reps) {
            change |= map.put(rep, value);
        }
        return change;
    }

    public boolean remove(ITermVar key, T value, IUnifier unifier) {
        final Set<ITermVar> reps = unifier.getVars(key);
        boolean change = false;
        for(ITermVar rep : reps) {
            change |= map.remove(rep, value);
        }
        return change;
    }

    public Collection<ITermVar> keySet() {
        return Collections.unmodifiableSet(map.keySet());
    }

    public boolean contains(ITermVar key, T value, IUnifier unifier) {
        final Set<ITermVar> reps = unifier.getVars(key);
        return reps.stream().anyMatch(rep -> map.containsEntry(rep, value));
    }

    public Set<T> get(ITermVar key, IUnifier unifier) {
        final Set<ITermVar> reps = unifier.getVars(key);
        return reps.stream().flatMap(rep -> map.get(rep).stream()).collect(Collectors.toSet());
    }

    public int size() {
        return map.size();
    }

    public boolean update(Collection<ITermVar> vars, IUnifier unifier) {
        boolean change = false;
        for(ITermVar var : vars) {
            change |= update(var, unifier);
        }
        return change;
    }

    public boolean update(final ITermVar var, IUnifier unifier) {
        final Collection<T> values = map.removeAll(var);
        final Set<ITermVar> reps = unifier.getVars(var);
        boolean change = false;
        for(ITermVar rep : reps) {
            change |= map.putAll(rep, values);
        }
        return change;
    }

}