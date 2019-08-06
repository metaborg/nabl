package mb.statix.taico.scopegraph.diff;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.util.collections.HashTrieRelation3;
import mb.nabl2.util.collections.IRelation3;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.taico.module.split.SplitModuleUtil;
import mb.statix.taico.name.Name;
import mb.statix.taico.name.Names;
import mb.statix.taico.scopegraph.IMInternalScopeGraph;
import mb.statix.taico.scopegraph.ModuleScopeGraph;
import mb.statix.taico.solver.Context;
import mb.statix.taico.solver.state.IMState;
import mb.statix.taico.unifier.DistributedUnifier;

public class Diff {
    /**
     * Computes the diff for the module with the given id between the given contexts.
     * 
     * @param id
     *      the id of the module
     * @param newContext
     *      the new context
     * @param oldContext
     *      the old context
     * @param external
     *      if true, the external scope graph is compared, otherwise, the internal scope graph is
     *      compared
     * 
     * @return
     *      a diffresult
     */
    public static DiffResult diff(String id, Context newContext, Context oldContext, boolean external) {
        DiffResult result = new DiffResult();
        diff(result, id, newContext, oldContext, external, false);
        return result;
    }
    
    /**
     * Computes the diff for the module with the given id between the given contexts.
     * 
     * @param id
     *      the id of the module
     * @param newContext
     *      the new context
     * @param oldContext
     *      the old context
     * @param external
     *      if true, the external scope graph is compared, otherwise, the internal scope graph is
     *      compared
     * 
     * @return
     *      a diffresult
     */
    public static DiffResult contextFreeDiff(String id, Context newContext, Context oldContext, boolean external) {
        DiffResult result = new DiffResult();
        diff(result, id, newContext, oldContext, external, true);
        return result;
    }
    
    public static void diff(
            DiffResult result,
            String id,
            Context cNew, Context cOld,
            boolean external,
            boolean onlyContextFree) {
        //Determine the graphs and their unifiers from the context
        IMInternalScopeGraph<Scope, ITerm, ITerm> sgNew = scopeGraph(cNew, cOld, id, external);
        IUnifier uNew = unifier(cNew, id);
        
        IMInternalScopeGraph<Scope, ITerm, ITerm> sgOld = scopeGraph(cOld, cNew, id, external);
        IUnifier uOld = unifier(cOld, id);
        
        //Scopes
        Set<Scope> newScopes     = getNew(sgOld.getScopes(), sgNew.getScopes());
        Set<Scope> removedScopes = getNew(sgNew.getScopes(), sgOld.getScopes());
        
        //Edges
        IRelation3.Transient<Scope, ITerm, Scope> newEdges =
                getNew(sgOld.getOwnEdges(), sgNew.getOwnEdges());
        IRelation3.Transient<Scope, ITerm, Scope> removedEdges =
                getNew(sgNew.getOwnEdges(), sgOld.getOwnEdges());
        
        //Data
        IRelation3.Transient<Scope, ITerm, ITerm> oldDataInst = Context.executeInContext(cOld,
                () -> instantiate(sgOld.getOwnData(), uOld));
        IRelation3.Transient<Scope, ITerm, ITerm> newDataInst = Context.executeInContext(cNew,
                () -> instantiate(sgNew.getOwnData(), uNew));
        
        IRelation3.Transient<Scope, ITerm, ITerm> newData     = getNew(oldDataInst, newDataInst);
        IRelation3.Transient<Scope, ITerm, ITerm> removedData = getNew(newDataInst, oldDataInst);
        
        //Convert data to names
        IRelation3.Transient<Scope, ITerm, Name> newDataNames = Context.executeInContext(cNew,
                () -> toNames(newData, d -> Names.getNameOrNull(d, uNew)));
        IRelation3.Transient<Scope, ITerm, Name> removedDataNames = Context.executeInContext(cOld,
                () -> toNames(removedData, d -> Names.getNameOrNull(d, uOld)));
        IRelation3.Transient<Scope, ITerm, Name> changedDataNames =
                nameOverlap(newDataNames, removedDataNames);
        
        ScopeGraphDiff diff =
                new ScopeGraphDiff(
                        newScopes, removedScopes,
                        newEdges, removedEdges,
                        newData, removedData,
                        newDataNames, removedDataNames, changedDataNames);
        result.addDiff(id, diff);
        
        //Child modules
        for (String childId : sgNew.getChildIds()) {
            if (!cNew.getModuleManager().hasModule(childId)) {
                System.err.println("Encountered child module " + childId + " of " + id + " which is a stale child (is a child in sgNew but is not in cNew!)");
                continue;
            }
            if (sgOld.getChildIds().contains(childId)) {
                //Ignore split modules
                if (onlyContextFree && SplitModuleUtil.isSplitModule(childId)) continue;
                
                //Child is contained in both, create a diff
                //Verify that the child is contained in both
                diff(result, childId, cNew, cOld, external, onlyContextFree);
            } else {
                //Child is in new but not in old -> added
                IMInternalScopeGraph<Scope, ITerm, ITerm> sg = cNew.getScopeGraph(childId);
                result.addAddedChild(childId, sg);
            }
        }
        
        for (String childId : sgOld.getChildIds()) {
            if (!cOld.getModuleManager().hasModule(childId)) {
                System.err.println("Encountered child module " + childId + " of " + id + " which is a stale child (is a child in sgOld but is not in cOld!)");
                continue;
            }
            //TODO Should split modules be included here?
            if (!sgNew.getChildIds().contains(childId)) {
                //Child is in old but not in new -> removed
                result.addRemovedChild(childId, cOld.getScopeGraph(childId));
            }
        }
    }
    
    /**
     * 
     * @param cTarget
     *      the context to get the scope graph from
     * @param cOther
     *      the context to retrieve an empty scope graph from 
     * @param id
     *      the id of the module
     * @param external
     *      if true, an external scope graph is returned, otherwise, an internal scope graph is
     *      returned
     * 
     * @return
     *      the scope graph of the module with the given id in the target context, or null if no
     *      module with the given id exists in either context
     */
    protected static IMInternalScopeGraph<Scope, ITerm, ITerm> scopeGraph(Context cTarget, Context cOther, String id, boolean external) {
        IMInternalScopeGraph<Scope, ITerm, ITerm> sgNew = cTarget.getScopeGraph(id);
        
        if (sgNew == null) {
            IMInternalScopeGraph<Scope, ITerm, ITerm> sgOld = cOther.getScopeGraph(id);
            if (sgOld == null) return null;
            return ModuleScopeGraph.empty(cOther.getScopeGraph(id));
        }
        
        return external
                ? (IMInternalScopeGraph<Scope, ITerm, ITerm>) sgNew.externalGraph()
                : sgNew;
    }
    
    /**
     * 
     * @param cTarget
     *      the context to get the unifier from
     * @param id
     *      the id of the module
     * 
     * @return
     *      
     */
    protected static IUnifier unifier(Context cTarget, String id) {
        IMState state = cTarget.getState(id);
        if (state == null) return DistributedUnifier.Immutable.of(id).unrestricted();
        
        return state.unifier().unrestricted();
    }
    
    /**
     * @param oldSet
     *      the old set
     * @param newSet
     *      the new set
     * 
     * @return
     *      everything in newSet that is not in oldSet
     */
    protected static <S> Set<S> getNew(Set<S> oldSet, Set<S> newSet) {
        Set<S> added = new HashSet<>();
        for (S s : newSet) {
            if (!oldSet.contains(s)) added.add(s);
        }
        return added;
    }
    
    /**
     * @param oldRel
     *      the old relation
     * @param newRel
     *      the new relation
     * 
     * @return
     *      everything in newRel that is not in oldRel
     */
    protected static <S, L, D> IRelation3.Transient<S, L, D> getNew(IRelation3<S, L, D> oldRel, IRelation3<S, L, D> newRel) {
        IRelation3.Transient<S, L, D> added = HashTrieRelation3.Transient.of();
        for (S s : oldRel.keySet()) {
            for (Entry<L, D> entry : oldRel.get(s)) {
                final L l = entry.getKey();
                final D d = entry.getValue();
                if (!newRel.contains(s, l, d)) added.put(s, l, d);
            }
        }
        return added;
    }
    
    /**
     * Fully instantiates the terms in the given relation by looking them up in the given unifier.
     * 
     * @param rel
     *      the relation
     * @param unifier
     *      the unifier
     * 
     * @return
     *      the instantiated terms
     */
    protected static <S, L> IRelation3.Transient<S, L, ITerm> instantiate(IRelation3<S, L, ITerm> rel, IUnifier unifier) {
        IRelation3.Transient<S, L, ITerm> tbr = HashTrieRelation3.Transient.of();
        for (S s : rel.keySet()) {
            for (Entry<L, ITerm> entry : rel.get(s)) {
                tbr.put(s, entry.getKey(), unifier.findRecursive(entry.getValue()));
            }
        }
        return tbr;
    }
    
    /**
     * Determines the overlap between the names in the given relations.
     * All overlapping names are removed from both.
     * 
     * @param newData
     *      all the new names
     * @param removedData
     *      all the removed names
     * 
     * @return
     *      the names that are in both
     */
    protected static <S, L, D> IRelation3.Transient<S, L, Name> nameOverlap(
            IRelation3.Transient<S, L, Name> newData, IRelation3.Transient<S, L, Name> removedData) {
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
    
    /**
     * @param data
     *      the data
     * @param dataToName
     *      the function to convert data to a name
     * 
     * @return
     *      the given relation, but with data converted to names
     */
    protected static <S, L, D> IRelation3.Transient<S, L, Name> toNames(IRelation3<S, L, D> data, Function<D, Name> dataToName) {
        HashTrieRelation3.Transient<S, L, Name> tbr = HashTrieRelation3.Transient.of();
        for (S s : data.keySet()) {
            for (Entry<L, D> entry : data.get(s)) {
                Name name = dataToName.apply(entry.getValue());
                if (name == null) {
                    System.err.println("DIFF: Skipping data " + entry.getValue() + " at scope " + s + " relation " + entry.getKey() + ": cannot convert to name");
                    continue;
                }
                tbr.put(s, entry.getKey(), dataToName.apply(entry.getValue()));
            }
        }
        return tbr;
    }
}
