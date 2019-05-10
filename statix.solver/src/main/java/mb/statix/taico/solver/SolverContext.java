package mb.statix.taico.solver;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.log.NullDebugContext;
import mb.statix.spec.Spec;
import mb.statix.taico.incremental.IChangeSet;
import mb.statix.taico.incremental.strategy.IncrementalStrategy;
import mb.statix.taico.module.IModule;
import mb.statix.taico.module.ModuleCleanliness;
import mb.statix.taico.module.ModuleManager;
import mb.statix.taico.module.ModulePaths;
import mb.statix.taico.solver.context.IContextAware;

public class SolverContext implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final transient IncrementalStrategy strategy;
    private final transient Spec spec;
    private final ModuleManager manager = new ModuleManager();
    private transient ASolverCoordinator coordinator;
    private transient SolverContext oldContext;
    private transient IChangeSet changeSet;
    private transient Map<String, Set<IConstraint>> initConstraints;
    
    //TODO Weak keys
//    private transient Set<WeakReference<IContextAware>> contextObservers = ConcurrentHashMap.newKeySet(); //Will be written, custom write method
    private transient Set<IContextAware> contextObservers = ConcurrentHashMap.newKeySet();
    private Map<String, MSolverResult> solverResults = new ConcurrentHashMap<>();
    private Map<IModule, IMState> states = new ConcurrentHashMap<>();
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
    public Optional<SolverContext> getOldContext() {
        return Optional.ofNullable(oldContext);
    }
    
    /**
     * @return
     *      the constraints
     */
    public Map<String, Set<IConstraint>> getInitialConstraints() {
        return initConstraints;
    }
    
    /**
     * Retrieves the initialization constraints for the modules for which they were not provided.
     * 
     * @param context
     *      the context
     * @param moduleConstraints
     *      a map from module NAMES to constraints
     * 
     * @return
     *      the given map
     * 
     * @throws IllegalArgumentException
     *      If an entry has more than one constraint in the set.
     * @throws IllegalStateException
     *      If the module represented by an entry was not present in the previous analysis but has
     *      no initialization constraints.
     */
    protected Map<String, Set<IConstraint>> fixInitConstraints(Map<String, Set<IConstraint>> moduleConstraints) {
        for (Entry<String, Set<IConstraint>> entry : moduleConstraints.entrySet()) {
            String childName = entry.getKey();
            if (entry.getValue().size() > 1) {
                throw new IllegalArgumentException("Module " + childName + " has more than one initialization constraint: " + entry.getValue());
            }

            if (!entry.getValue().isEmpty()) continue;

            //Scope substitution does not have to occur here, since the global scope remains constant.
            //If there is no constraint available, use the initialization constraint for the child
            IModule child = oldContext == null ? null : oldContext.manager.getModuleByName(childName, 1);
            if (child == null) {
                throw new IllegalStateException("Encountered a module without initialization that was not present in the previous context: " + childName);
            }

            entry.setValue(Collections.singleton(child.getInitialization()));
        }
        return moduleConstraints;
    }
    
    public Spec getSpec() {
        return spec;
    }
    
    // --------------------------------------------------------------------------------------------
    // Modules
    // --------------------------------------------------------------------------------------------
    
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
        
        if (phase == -1) return getModuleUnchecked(id);
        
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
        IModule module = manager.getModuleByName(name, level);
        if (module != null) return module;
        
        return oldContext == null ? null : oldContext.manager.getModuleByName(name, level);
    }
    
    public IModule getModule(IModule requester, String id) throws Delay {
        if (phase == -1) return getModuleUnchecked(id);
        
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
     */
    public IModule getModuleUnchecked(String id) {
        IModule module = manager.getModule(id);
        if (module != null) return module;
        
        if (oldContext == null) return null;
        return oldContext.manager.getModule(id);
    }
    
    /**
     * @return
     *      the root module
     */
    public IModule getRootModule() {
        return coordinator.getRootModule();
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
        //TODO IMPORTANT does not include old modules
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
    public IMState getState(IModule module) {
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
    public void setState(IModule module, IMState state) {
        IMState old = states.put(module, state);
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
//        contextObservers.add(new WeakReference<>(contextAware));
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
//        Iterator<WeakReference<IContextAware>> it = contextObservers.iterator();
//        while (it.hasNext()) {
//            WeakReference<IContextAware> old = it.next();
//            IContextAware observer = old.get();
//            if (observer != null) observer.setContext(target);
//            it.remove();
//        }
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
        //TODO probably need more here
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
    
//    private void writeObject(ObjectOutputStream out) throws IOException {
//        Set<IContextAware> contextObservers = new HashSet<>();
//        for (WeakReference<IContextAware> wr : this.contextObservers) {
//            IContextAware observer = wr.get();
//            if (observer != null) contextObservers.add(observer);
//        }
//        out.defaultWriteObject();
//        out.writeObject(contextObservers);
//    }
//    
//    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
//        //TODO IMPORTANT CRITICAL Unable to read like this, the weak references might be cleared directly.
//        in.defaultReadObject();
//        Set<IContextAware> contextObservers = (Set<IContextAware>) in.readObject();
//        this.contextObservers = ConcurrentHashMap.newKeySet();
//        for (IContextAware observer : contextObservers) {
//            this.contextObservers.add(new WeakReference<>(observer));
//        }
//    }
    
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
    public static SolverContext incrementalContext(
            IncrementalStrategy strategy, SolverContext previousContext, IMState previousRootState,
            IChangeSet changeSet, Map<String, Set<IConstraint>> initConstraints, Spec spec) {
        SolverContext newContext = new SolverContext(strategy, spec);
        newContext.oldContext = previousContext; //TODO Ensure that changes are committed
        newContext.changeSet = changeSet;
        newContext.initConstraints = newContext.fixInitConstraints(initConstraints);
        previousContext.transferContextObservers(newContext);
        newContext.setState(previousRootState.getOwner(), previousRootState);
        ModuleSolver.topLevelSolver(previousRootState, Collections.emptyList(), new NullDebugContext());
        return newContext;
    }
}
