package mb.nabl2.terms.collection;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.u.IUnifier;

public class TermMultiset {

    private final Multiset<ITerm> terms;
    private final Multimap<ITermVar, ITerm> varTerms;

    public TermMultiset() {
        this.terms = HashMultiset.create();
        this.varTerms = HashMultimap.create();
    }

    public void add(ITerm term, IUnifier unifier) {
        add(term, 1, unifier);
    }

    public void add(ITerm term, int n, IUnifier unifier) {
        final ITerm rep = unifier.findRecursive(term);
        for(ITermVar var : rep.getVars().elementSet()) {
            varTerms.put(var, rep);
        }
        terms.add(rep, n);
    }

    public int remove(ITerm term, IUnifier unifier) {
        return remove(term, 1, unifier);
    }

    public int remove(ITerm term, int n, IUnifier unifier) {
        final ITerm rep = unifier.findRecursive(term);
        final int prev_n = terms.remove(rep, n);
        if(prev_n <= n) {
            varTerms.values().remove(rep);
        }
        return Math.min(prev_n, n);
    }

    public boolean contains(ITerm term, IUnifier unifier) {
        return count(term, unifier) > 0;
    }

    public int count(ITerm term, IUnifier unifier) {
        final ITerm rep = unifier.findRecursive(term);
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

    public boolean update(Collection<ITermVar> vars, IUnifier unifier) {
        final Set<ITerm> updatedTerms =
                vars.stream().flatMap(var -> varTerms.removeAll(var).stream()).collect(Collectors.toSet());
        for(ITerm term : updatedTerms) {
            final int n = terms.remove(term, terms.count(term));
            varTerms.values().remove(term);
            add(term, n, unifier);
        }
        return !updatedTerms.isEmpty();
    }

}