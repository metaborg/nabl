package mb.statix.taico.scopegraph.diff;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.util.collections.HashTrieRelation3;
import mb.nabl2.util.collections.IRelation3;
import mb.statix.taico.name.Name;
import mb.statix.taico.scopegraph.IMInternalScopeGraph;

public class Diff {
    /**
     * Performs a diff between the given scope graphs.
     * 
     * @param sga
     *      the previous version
     * @param ua
     *      the unifier of the previous version
     * @param sgb
     *      the new version
     * @param ub
     *      the unifier of the new version
     * @param external
     *      if the external scope graphs should be compared instead of the actual graphs
     * @param dataToName
     *      a function to convert data to a name
     * 
     * @return
     *      the diff
     */
    public static <S extends D, L, D> IScopeGraphDiff<S, L, D> diff(
            IMInternalScopeGraph<S, L, D> sga, IUnifier ua,
            IMInternalScopeGraph<S, L, D> sgb, IUnifier ub,
            boolean external, Function<D, Name> dataToName) {
        IMInternalScopeGraph<S, L, D> a = external ? (IMInternalScopeGraph<S, L, D>) sga.externalGraph() : sga;
        IMInternalScopeGraph<S, L, D> b = external ? (IMInternalScopeGraph<S, L, D>) sgb.externalGraph() : sgb;
        
        Set<S> newScopes = getNew(a.getScopes(), b.getScopes());   //new in b
        Set<S> removedScopes = getNew(b.getScopes(), a.getScopes()); //new in a = removed in b
        
        IRelation3<S, L, S> newEdges = getNew(a.getOwnEdges(), b.getOwnEdges());
        IRelation3<S, L, S> removedEdges = getNew(b.getOwnEdges(), a.getOwnEdges());
        
        //Changed = same name, but different data
        IRelation3<S, L, D> newData = getNew(a.getOwnData(), b.getOwnData());
        IRelation3<S, L, D> removedData = getNew(b.getOwnData(), a.getOwnData());
        
        HashTrieRelation3.Transient<S, L, Name> newDataNames = toNames(newData, dataToName);
        HashTrieRelation3.Transient<S, L, Name> removedDataNames = toNames(removedData, dataToName);
        HashTrieRelation3.Transient<S, L, Name> changedDataNames = nameOverlap(newDataNames, removedDataNames);
        
        return new ScopeGraphDiff<>(newScopes, removedScopes, newEdges, removedEdges, newDataNames, removedDataNames, changedDataNames);
        
        //TODO IMPORTANT Diff children!
//        if (external) {
//            
//        }
    }
    
    public static Name itermToName(ITerm term) {
        System.out.println(term);
        return new Name("", Collections.emptyList());
        
//        M.appl3(StatixTerms.OCCURRENCE_OP, M.string(), m2, m3)
//        //(Occurrence, value)
//        //(String, String, value)
//        M.appl(Terms.TUPLE_OP, a -> a
////        {
////            int arity = a.getArity();
////            
////        }
//        ).match(term);
    }
    
    private static <S> Set<S> getNew(Set<S> a, Set<S> b) {
        Set<S> added = new HashSet<>();
        for (S s : b) {
            if (!a.contains(s)) added.add(s);
        }
        return added;
    }
    
    private static <S extends D, L, D> IRelation3.Transient<S, L, D> getNew(IRelation3<S, L, D> a, IRelation3<S, L, D> b) {
        IRelation3.Transient<S, L, D> added = HashTrieRelation3.Transient.of();
        for (S s : a.keySet()) {
            for (Entry<L, D> entry : a.get(s)) {
                final L l = entry.getKey();
                final D d = entry.getValue();
                if (!b.contains(s, l, d)) added.put(s, l, d);
            }
        }
        return added;
    }
    
    private static <S extends D, L, D> HashTrieRelation3.Transient<S, L, Name> nameOverlap(
            HashTrieRelation3.Transient<S, L, Name> newData, HashTrieRelation3.Transient<S, L, Name> removedData) {
        HashTrieRelation3.Transient<S, L, Name> tbr = HashTrieRelation3.Transient.of();
        for (S s : newData.keySet()) {
            for (Entry<L, Name> entry : newData.get(s)) {
                final L l = entry.getKey();
                final Name name = entry.getValue();
                if (removedData.contains(s, l, name)) {
                    tbr.put(s, l, name);
                }
            }
        }
        
        for (S s : tbr.keySet()) {
            for (Entry<L, Name> entry : tbr.get(s)) {
                newData.remove(s, entry.getKey(), entry.getValue());
                removedData.remove(s, entry.getKey(), entry.getValue());
            }
        }
        return tbr;
    }
    
    private static <S extends D, L, D> HashTrieRelation3.Transient<S, L, Name> toNames(IRelation3<S, L, D> old, Function<D, Name> dataToName) {
        HashTrieRelation3.Transient<S, L, Name> tbr = HashTrieRelation3.Transient.of();
        for (S s : old.keySet()) {
            for (Entry<L, D> entry : old.get(s)) {
                tbr.put(s, entry.getKey(), dataToName.apply(entry.getValue()));
            }
        }
        return tbr;
    }
}
