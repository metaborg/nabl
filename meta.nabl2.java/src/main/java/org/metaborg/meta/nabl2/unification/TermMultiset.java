package org.metaborg.meta.nabl2.unification;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;

public class TermMultiset {

    private final IUnifier unifier;
    private final Multiset<ITerm> terms;
    private final Multimap<ITermVar, ITerm> varTerms;

    public TermMultiset(IUnifier unifier) {
        this.unifier = unifier;
        this.terms = HashMultiset.create();
        this.varTerms = HashMultimap.create();
    }

    public void add(ITerm term) {
        add(term, 1);
    }

    public void add(ITerm term, int n) {
        final ITerm rep = unifier.find(term);
        for(ITermVar var : rep.getVars().elementSet()) {
            varTerms.put(var, rep);
        }
        terms.add(rep, n);
    }

    public int remove(ITerm term) {
        return remove(term, 1);
    }

    public int remove(ITerm term, int n) {
        final ITerm rep = unifier.find(term);
        final int prev_n = terms.remove(rep, n);
        if(prev_n <= n) {
            varTerms.values().remove(rep);
        }
        return Math.min(prev_n, n);
    }

    public boolean contains(ITerm term) {
        return count(term) > 0;
    }

    public int count(ITerm term) {
        final ITerm rep = unifier.find(term);
        return terms.count(rep);
    }

    public int size() {
        return terms.size();
    }

    public Set<ITerm> elementSet() {
        return Collections.unmodifiableSet(terms.elementSet());
    }

    public Set<ITermVar> varSet() {
        return Collections.unmodifiableSet(varTerms.keySet());
    }

    public boolean update(Collection<ITermVar> vars) {
        final Set<ITerm> updatedTerms =
                vars.stream().flatMap(var -> varTerms.removeAll(var).stream()).collect(Collectors.toSet());
        for(ITerm term : updatedTerms) {
            final int n = terms.remove(term, terms.count(term));
            varTerms.values().remove(term);
            add(term, n);
        }
        return !updatedTerms.isEmpty();
    }

}