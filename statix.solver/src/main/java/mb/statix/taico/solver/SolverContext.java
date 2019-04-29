package mb.statix.taico.solver;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.spec.Spec;
import mb.statix.taico.incremental.IChangeSet;
import mb.statix.taico.incremental.strategy.IncrementalStrategy;
import mb.statix.taico.module.IModule;
import mb.statix.taico.module.ModuleCleanliness;
import mb.statix.taico.module.ModuleManager;
import mb.statix.taico.module.ModulePaths;
import mb.statix.taico.scopegraph.IOwnableScope;
import mb.statix.taico.solver.context.IContextAware;

public class SolverContext implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final transient IncrementalStrategy strategy;
    private final transient Spec spec;
    private final ModuleManager manager = new ModuleManager();
    private transient ASolverCoordinator coordinator;
    private transient SolverContext oldContext;
    private transient IChangeSet changeSet;
    
    private Set<IContextAware> contextObservers = ConcurrentHashMap.newKeySet();
    private Map<String, MSolverResult> solverResults = new ConcurrentHashMap<>();
    private Map<IModule, MState> states = new ConcurrentHashMap<>();
    private volatile int phase = -1;
    
    private SolverContext(IncrementalStrategy strategy, Spec spec) {
        this.strategy = strategy;
        this.spec = spec;
    }
    
    /**
     * NOTE: This method should only be used by the strategy.
     * 
     * @return
     *      the old context
     */
    public SolverContext getOldContext() {
        return oldContext;
    }
    
    // --------------------------------------------------------------------------------------------
    // Spec
    // --------------------------------------------------------------------------------------------
    public Spec getSpec() {
        return spec;
    }
    
    // --------------------------------------------------------------------------------------------
    // Modules
    // --------------------------------------------------------------------------------------------
    
    @Deprecated
    public ModuleManager getModuleManager() {
        return manager;
    }
    
    /**
     * 
     * @param module
     * 
     * @see ModuleManager#addModule(IModule)
     */
    public void addModule(IModule module) {
        manager.addModule(module);
    }
    
    public IModule getChildModuleByName(IModule requester, String name) throws Delay {
        String id = ModulePaths.build(requester.getId(), name);
        
        if (phase == -1) return _getModule(id);
        
        return strategy.getChildModule(this, oldContext, requester, id);
    }
    
    /**
     * TODO: Unchecked access.
     * @param name
     *      the name of the module
     * @param level
     *      the level on which to find the module
     * 
     * @return
     *      the given module, or null if no module with the given name exists
     * 
     * @throws IllegalStateException
     *      If the given name is not unique on its level
     */
    public IModule getModuleByName(String name, int level) {
        return manager.getModuleByName(name, level);
    }
    
    public IModule getModule(IModule requester, String id) throws Delay {
        if (phase == -1) return _getModule(id);
        
        //TODO Also do the first part based on the strategy, to allow the strategy to delay.
        return strategy.getModule(this, oldContext, requester, id);
    }
    
    /**
     * Gets the module with the given id, without checking with the strategy. If the given module
     * exists or existed in the previous context, it is returned.
     * 
     * @param id
     *      the id of the module
     * 
     * @return
     *      the module with the given id, or null if no such module exists nor existed
     * 
     * @deprecated
     *      It should never be necessary to use this method.
     */
    @Deprecated
    public IModule getModuleUnchecked(String id) {
        return _getModule(id);
    }
    
    /**
     * Gets the module with the given id, without checking with the strategy. If the given module
     * exists or existed in the previous context, it is returned.
     * 
     * @param id
     *      the id of the module
     * 
     * @return
     *      the module with the given id, or null if no such module exists nor existed
     */
    private IModule _getModule(String id) {
        IModule module = manager.getModule(id);
        if (module != null) return module;
        
        if (oldContext == null) return null;
        return oldContext.manager.getModule(id);
    }
    
    /**
     * If the module with the given name already existed as a child of this module, that module is
     * returned. Otherwise, this method returns a new child module of this module.
     * 
     * @param name
     *      the name of the module to create or get
     * @param canExtend
     *      the list of scopes from this module and parents that the child can extend, in the order
     *      they are encountered in the rule
     * @param moduleBoundary
     *      the name of the module boundary which caused this modules creation
     * @param args
     *      the arguments with which the module boundary was called (TODO IMPORTANT substitute scopes)
     *      (TODO does this preserve declarations (references) correctly?)
     * 
     * @return
     *      the new/old child module
     */
    public IModule createChild(IModule parent, String name, List<IOwnableScope> canExtend, IConstraint constraint) {
        //TODO Incrementality breaks if parent or child names are changed
        String id = ModulePaths.build(parent.getId(), name);
        
        IModule oldModule = getChildModuleByName(parent, name);
        if (oldModule == null) {
            System.err.println("[" + parent.getId() + "] Creating new child " + name);
            return createChild(name, canExtend, constraint);
        }
        //If the module is new, we throw an exception
        //Invariant: we can only create each module once
        if (manager.hasModule(oldModule.getId()))
        
        if (oldModule.getFlag() == ModuleCleanliness.CLEAN) {
            //Update the edges to the new scopes and add it as a child of the current scope graph.
            oldModule.getScopeGraph().substitute(canExtend);
            oldModule.setParent(this);
            //TODO We potentially need to replace some of the old arguments with new ones in the old module results?
            oldModule.setInitialization(constraint);
            //Set the coordinator to our coordinator
            oldModule.getCurrentState().setCoordinator(getCurrentState().coordinator());
            getScopeGraph().addChild(oldModule);
            return oldModule;
        } else {
            return createChild(name, canExtend, constraint);
        }
    }
    
    /**
     * @return
     *      a set with all modules
     * 
     * @see ModuleManager#getModules()
     */
    public Set<IModule> getModules() {
        return manager.getModules();
    }
    
    /**
     * @return
     *      a map from module ID to module
     * 
     * @see ModuleManager#getModulesAndIds()
     */
    public Map<String, IModule> getModulesAndIds() {
        return manager.getModulesAndIds();
    }
    
    /**
     * @return
     *      a map from module NAME to module
     * 
     * @see ModuleManager#getModulesOnLevel(int)
     */
    public Map<String, IModule> getModulesOnLevel(int level) {
        return manager.getModulesOnLevel(level);
    }
    
    // --------------------------------------------------------------------------------------------
    // States
    // --------------------------------------------------------------------------------------------
    
    /**
     * @param module
     *      the module
     * 
     * @return
     *      the state associated with the given module in the current context
     */
    public MState getState(IModule module) {
        return states.get(module);
    }
    
    /**
     * Sets the state for the given module.
     * 
     * @param module
     *      the module
     * @param state
     *      the state
     */
    public void setState(IModule module, MState state) {
        MState old = states.put(module, state);
        if (old != null) System.err.println("Overridden state of " + module + " in context " + hashCode());
    }
    
    // --------------------------------------------------------------------------------------------
    // Phase
    // --------------------------------------------------------------------------------------------
    
    public int getPhase() {
        return phase;
    }
    
    public void setPhase(int phase) {
        this.phase = phase;
    }
    
    // --------------------------------------------------------------------------------------------
    // Solver coordinator
    // --------------------------------------------------------------------------------------------
    
    public ASolverCoordinator getCoordinator() {
        return coordinator;
    }
    
    public void setCoordinator(ASolverCoordinator coordinator) {
        this.coordinator = coordinator;
    }
    
    // --------------------------------------------------------------------------------------------
    // Solver results
    // --------------------------------------------------------------------------------------------
    
    public void addResult(String moduleId, MSolverResult result) {
        solverResults.put(moduleId, result);
    }
    
    public MSolverResult getResult(String moduleId) {
        return solverResults.get(moduleId);
    }
    
    public Map<String, MSolverResult> getResults() {
        return new HashMap<>(solverResults);
    }

    // --------------------------------------------------------------------------------------------
    // Context observers
    // --------------------------------------------------------------------------------------------
    
    /**
     * Registers the given {@link IContextAware} as an observer of the context.
     * 
     * @param contextAware
     *      the observer to register
     */
    public void register(IContextAware contextAware) {
        contextObservers.add(contextAware);
    }
    
    /**
     * Transfers all context observers to the given target context.
     * This updates the context of all the observers to the target context.
     * 
     * @param target
     *      the target context
     */
    protected void transferContextObservers(SolverContext target) {
        //TODO Parallel + clear
        Iterator<IContextAware> it = contextObservers.iterator();
        while (it.hasNext()) {
            IContextAware observer = it.next();
            observer.setContext(target);
            it.remove();
        }
    }
    
    /**
     * Commits the changes with regards to the previous solver context.
     * This call copies over any information from the old context that is relevant and then removes
     * the links to the old context and the change set, finalizing this context.
     */
    public void commitChanges() {
        for (IModule module : oldContext.manager.getModules()) {
            if (changeSet.removed().contains(module)) continue;
            
            //If we have a new version for the module already, skip it
            if (manager.hasModule(module.getId())) continue;
            
            assert module.getFlag() == ModuleCleanliness.CLEAN : "module flag should be clean if it is not in the new context";
            module.flag(ModuleCleanliness.CLEAN);
            //TODO The module should get the new context and stuff.
            manager.addModule(module);
        }
        
        oldContext = null;
        changeSet = null;
    }
    
    // --------------------------------------------------------------------------------------------
    // Object methods
    // --------------------------------------------------------------------------------------------
    
    @Override
    public String toString() {
        return "SolverContext(" + hashCode() + ") [strategy=" + strategy
                + ", manager=" + manager
                + ", oldContext=" + oldContext
                + ", changeSet=" + changeSet
                + ", contextObservers=" + contextObservers
                + ", phase=" + phase
                + ", solverResults=" + solverResults + "]";
    }
    
    // --------------------------------------------------------------------------------------------
    // Creation
    // --------------------------------------------------------------------------------------------
    
    /**
     * Creates a solver context for when a clean build is requested or no previous results are
     * available.
     * 
     * @param strategy
     *      the incremental strategy to use
     * @param spec
     *      the spec
     * 
     * @return
     *      the new solver context
     */
    public static SolverContext initialContext(IncrementalStrategy strategy, Spec spec) {
        SolverContext newContext = new SolverContext(strategy, spec);
        return newContext;
    }

    /**
     * Creates a solver context for when a previous context is available as well as the changeset
     * with regards to that previous context.
     * 
     * @param strategy
     *      the incremental strategy to employ
     * @param previousContext
     *      the previous context
     * @param changeSet
     *      the changeset
     * 
     * @return
     *      the new solver context
     */
    public static SolverContext incrementalContext(IncrementalStrategy strategy, SolverContext previousContext, IChangeSet changeSet, Spec spec) {
        SolverContext newContext = new SolverContext(strategy, spec);
        newContext.oldContext = previousContext;
        newContext.changeSet = changeSet;
        previousContext.transferContextObservers(newContext);
        
        return newContext;
    }
}
