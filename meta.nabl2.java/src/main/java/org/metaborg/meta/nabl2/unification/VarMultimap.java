package org.metaborg.meta.nabl2.unification;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.metaborg.meta.nabl2.terms.ITermVar;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;

public class VarMultimap<T> {

    private final IUnifier unifier;
    private final Multimap<ITermVar, T> map;

    public VarMultimap(IUnifier unifier) {
        this.unifier = unifier;
        this.map = HashMultimap.create();
    }

    public boolean put(ITermVar key, T value) {
        final Multiset<ITermVar> reps = unifier.find(key).getVars();
        boolean change = false;
        for(ITermVar rep : reps) {
            change |= map.put(rep, value);
        }
        return change;
    }

    public boolean remove(ITermVar key, T value) {
        final Multiset<ITermVar> reps = unifier.find(key).getVars();
        boolean change = false;
        for(ITermVar rep : reps) {
            change |= map.remove(rep, value);
        }
        return change;
    }

    public Collection<ITermVar> keySet() {
        return Collections.unmodifiableSet(map.keySet());
    }

    public boolean contains(ITermVar key, T value) {
        final Multiset<ITermVar> reps = unifier.find(key).getVars();
        return reps.stream().anyMatch(rep -> map.containsEntry(rep, value));
    }

    public Set<T> get(ITermVar key) {
        final Multiset<ITermVar> reps = unifier.find(key).getVars();
        return reps.stream().flatMap(rep -> map.get(rep).stream()).collect(Collectors.toSet());
    }

    public int size() {
        return map.size();
    }

    public boolean update(Collection<ITermVar> vars) {
        boolean change = false;
        for(ITermVar var : vars) {
            change |= update(var);
        }
        return change;
    }

    public boolean update(final ITermVar var) {
        final Collection<T> values = map.removeAll(var);
        final Multiset<ITermVar> reps = unifier.find(var).getVars();
        boolean change = false;
        for(ITermVar rep : reps) {
            change |= map.putAll(rep, values);
        }
        return change;
    }

}