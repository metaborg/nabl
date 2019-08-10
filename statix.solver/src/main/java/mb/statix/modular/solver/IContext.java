package mb.statix.modular.solver;

import java.io.Serializable;

import org.checkerframework.checker.nullness.qual.Nullable;

import mb.nabl2.terms.ITerm;
import mb.statix.modular.module.IModule;
import mb.statix.modular.scopegraph.IMInternalScopeGraph;
import mb.statix.modular.solver.state.IMState;
import mb.statix.modular.unifier.DistributedUnifier;
import mb.statix.scopegraph.terms.Scope;

public interface IContext extends Serializable {
    
    // --------------------------------------------------------------------------------------------
    // Context
    // --------------------------------------------------------------------------------------------
    
    /**
     * @return
     *      the old context
     */
    public @Nullable Context getOldContext();
    
    // --------------------------------------------------------------------------------------------
    // States
    // --------------------------------------------------------------------------------------------
    
    /**
     * @param module
     *      the id of the module
     * 
     * @return
     *      the state associated with the given module in this context, or null if there is none
     */
    public @Nullable IMState getState(String moduleId);
    
    /**
     * @param module
     *      the module
     * 
     * @return
     *      the state associated with the given module in this context, or null if there is none
     */
    public @Nullable default IMState getState(IModule module) {
        return getState(module.getId());
    }
    
    /**
     * @param moduleId
     *      the id of the module
     * 
     * @return
     *      the state associated with the given module in the old context, or null if there was
     *      none or if there is no old context
     */
    public default @Nullable IMState getOldState(String moduleId) {
        final Context oldContext = getOldContext();
        if (oldContext == null) return null;
        
        return oldContext.getState(moduleId);
    }
    
    /**
     * @param module
     *      the module
     * 
     * @return
     *      the state associated with the given module in the old context, or null if there was
     *      none or if there is no old context
     */
    public default @Nullable IMState getOldState(IModule module) {
        return getOldState(module.getId());
    }
    
    // --------------------------------------------------------------------------------------------
    // Scope Graphs
    // --------------------------------------------------------------------------------------------
    
    /**
     * @param moduleId
     *      the id of the module
     * 
     * @return
     *      the scope graph of the given module in this context, or null if none
     */
    public default @Nullable IMInternalScopeGraph<Scope, ITerm, ITerm> getScopeGraph(String moduleId) {
        IMState state = getState(moduleId);
        return state == null ? null : state.scopeGraph();
    }
    
    /**
     * @param id
     *      the module
     * 
     * @return
     *      the scope graph of the given module in this context, or null if none
     */
    public default @Nullable IMInternalScopeGraph<Scope, ITerm, ITerm> getScopeGraph(IModule module) {
        return getScopeGraph(module.getId());
    }
    
    /**
     * @param moduleId
     *      the id of the module
     * 
     * @return
     *      the scope graph of the given module in the old context, or null if none
     */
    public default @Nullable IMInternalScopeGraph<Scope, ITerm, ITerm> getOldScopeGraph(String moduleId) {
        final Context oldContext = getOldContext();
        if (oldContext == null) return null;
        
        return oldContext.getScopeGraph(moduleId);
    }
    
    /**
     * @param module
     *      the module
     * 
     * @return
     *      the scope graph of the given module in the old context, or null if none
     */
    public default @Nullable IMInternalScopeGraph<Scope, ITerm, ITerm> getOldScopeGraph(IModule module) {
        return getOldScopeGraph(module.getId());
    }
    
    // --------------------------------------------------------------------------------------------
    // Unifiers
    // --------------------------------------------------------------------------------------------
    
    /**
     * @param moduleId
     *      the id of the module
     * 
     * @return
     *      the unifier of the given module in the current context, or null if there is none
     */
    public default DistributedUnifier.@Nullable Immutable getUnifier(String moduleId) {
        IMState state = getState(moduleId);
        return state == null ? null : state.unifier();
    }
    
    /**
     * @param module
     *      the module
     * 
     * @return
     *      the unifier of the given module in the current context, or null if there is none
     */
    public default DistributedUnifier.@Nullable Immutable getUnifier(IModule module) {
        return getUnifier(module.getId());
    }
    
    /**
     * @param moduleId
     *      the id of the module
     * @param def
     *      the unifier to return if there is no state for the given module
     * 
     * @return
     *      the unifier of the given module in the current context, or def if there is none
     */
    public default DistributedUnifier.Immutable getUnifierOrDefault(String moduleId, DistributedUnifier.Immutable def) {
        IMState state = getState(moduleId);
        return state == null ? def : state.unifier();
    }
    
    /**
     * @param moduleId
     *      the id of the module
     * 
     * @return
     *      the unifier of the given module in the previous context, or null if there is none
     */
    public default DistributedUnifier.@Nullable Immutable getOldUnifier(String moduleId) {
        final Context oldContext = getOldContext();
        if (oldContext == null) return null;
        
        return oldContext.getUnifier(moduleId);
    }
    
    /**
     * @param module
     *      the module
     * 
     * @return
     *      the unifier of the given module in the previous context, or null if there is none
     */
    public default DistributedUnifier.@Nullable Immutable getOldUnifier(IModule module) {
        return getOldUnifier(module.getId());
    }
    
    /**
     * @param moduleId
     *      the id of the module
     * @param def
     *      the default unifier
     * 
     * @return
     *      the unifier of the given module in the previous context, or def if there is none
     */
    public default DistributedUnifier.Immutable getOldUnifierOrDefault(String moduleId, DistributedUnifier.Immutable def) {
        final Context oldContext = getOldContext();
        if (oldContext == null) return null;
        
        return oldContext.getUnifierOrDefault(moduleId, def);
    }
    
    // --------------------------------------------------------------------------------------------
    // Solvers
    // --------------------------------------------------------------------------------------------
    
    /**
     * @param moduleId
     *      the id of the module
     * 
     * @return
     *      the solver of the given module, or null if there is no state for the given module
     */
    public default @Nullable ModuleSolver getSolver(String moduleId) {
        IMState state = getState(moduleId);
        return state == null ? null : state.solver();
    }
    
    /**
     * @param module
     *      the module
     * 
     * @return
     *      the solver of the given module, or null if there is no state for the given module
     */
    public default @Nullable ModuleSolver getSolver(IModule module) {
        return getSolver(module.getId());
    }
}
