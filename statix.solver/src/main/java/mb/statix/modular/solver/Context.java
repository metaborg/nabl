package mb.statix.modular.solver;

import static mb.statix.modular.util.TOverrides.hashMap;

import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.google.common.collect.Sets;

import mb.statix.constraints.CTrue;
import mb.statix.modular.dependencies.Dependencies;
import mb.statix.modular.dependencies.DependencyManager;
import mb.statix.modular.incremental.Flag;
import mb.statix.modular.incremental.changeset.IChangeSet;
import mb.statix.modular.incremental.manager.IncrementalManager;
import mb.statix.modular.incremental.strategy.IncrementalStrategy;
import mb.statix.modular.module.IModule;
import mb.statix.modular.module.ModuleManager;
import mb.statix.modular.module.ModulePaths;
import mb.statix.modular.scopegraph.reference.ModuleDelayException;
import mb.statix.modular.solver.completeness.RedirectingIncrementalCompleteness;
import mb.statix.modular.solver.coordinator.ISolverCoordinator;
import mb.statix.modular.solver.state.IMState;
import mb.statix.solver.IConstraint;
import mb.statix.solver.log.NullDebugContext;
import mb.statix.spec.Spec;

/**
 * Class to represent the context. The context keeps track of all information necessary for
 * modular solving.
 */
public class Context implements IContext, Serializable {
    private static final long serialVersionUID = 1L;
    
    private final IncrementalStrategy strategy;
    private final Spec spec;
    private final ModuleManager manager = new ModuleManager();
    private final DependencyManager<?> dependencies;
    private final IncrementalManager incrementalManager;
    private transient ISolverCoordinator coordinator;
    private transient Context oldContext;
    private transient IChangeSet changeSet;
    private transient Map<String, IConstraint> initConstraints;
    
    private Map<IModule, MSolverResult> solverResults = hashMap();
    private Map<String, IMState> states = hashMap();
    
    private Context(IncrementalStrategy strategy, Spec spec, @Nullable Context oldContext) {
        this.strategy = strategy;
        this.spec = spec;
        this.oldContext = oldContext;
        this.incrementalManager = strategy.createManager();
        this.dependencies = strategy.createDependencyManager(oldContext);
    }
    
    public IncrementalStrategy getStrategy() {
        return strategy;
    }
    
    @Override
    public @Nullable Context getOldContext() {
        return oldContext;
    }
    
    /**
     * @return
     *      the constraints
     */
    public Map<String, IConstraint> getInitialConstraints() {
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
     */
    protected Map<String, IConstraint> fixInitConstraints(Map<String, IConstraint> moduleConstraints) {
        for (Entry<String, IConstraint> entry : moduleConstraints.entrySet()) {
            if (!(entry.getValue() instanceof CTrue)) continue;
            
            String childNameOrId = entry.getKey();

            //Scope substitution does not have to occur here, since the global scope remains constant.
            //If there is no constraint available, use the initialization constraint for the child
            if (oldContext == null) {
                throw new IllegalStateException("Encountered a module without initialization but no previous context is available: " + childNameOrId);
            }
            
            IModule child = oldContext.getModuleByNameOrId(childNameOrId);
            if (child == null) {
                throw new IllegalStateException("Encountered a module without initialization that was not present in the previous context: " + childNameOrId);
            }

            entry.setValue(child.getInitialization());
        }
        return moduleConstraints;
    }
    
    public Spec getSpec() {
        return spec;
    }
    
    public IChangeSet getChangeSet() {
        return changeSet;
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
    
    public IModule getChildModuleByName(IModule requester, String name) throws ModuleDelayException {
        String id = ModulePaths.build(requester.getId(), name);
        
        if (isInitPhase()) return getModuleUnchecked(id);
        
        if (!getIncrementalManager().isAllowedAccess(requester.getId(), id)) {
            throw new ModuleDelayException(requester.getId(), id);
        }
        
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
    
    public IModule getModule(IModule requester, String id) throws ModuleDelayException {
        return getModule(requester.getId(), id);
    }
    
    /**
     * @param requester
     *      the id of the module requesting the module with the given id
     * @param id
     *      the id of the requested module
     * 
     * @return
     *      the requested module, if it exists, or null otherwise
     * 
     * @throws ModuleDelayException
     *      If the given requester is not allowed to access modules or is not allowed to access
     *      the given module in particular.
     */
    public @Nullable IModule getModule(String requester, String id) throws ModuleDelayException {
        if (isInitPhase()) return getModuleUnchecked(id);
        
        if (!getIncrementalManager().isAllowedAccess(requester, id)) {
            throw new ModuleDelayException(requester, id);
        }
        
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
        
        module = oldContext.manager.getModule(id);
        
        //If the module was removed, don't return it
        if (changeSet.removed().contains(module)) return null;
        return module;
    }
    
    /**
     * @param id
     *      the id of the module
     * 
     * @return
     *      the module with the given id from the previous analysis
     */
    public IModule getOldModule(String id) {
        if (oldContext == null) return null;
        return oldContext.manager.getModule(id);
    }
    
    /**
     * @return
     *      the root module
     */
    public IModule getRootModule() {
        if (coordinator != null) return coordinator.getRootModule();
        return manager.getModulesOnLevel(0).values().stream().findFirst().get();
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
    
    /**
     * @return
     *      a map from module NAME to module
     * 
     * @see ModuleManager#getModulesOnLevel(int, boolean)
     */
    public Map<String, IModule> getModulesOnLevel(int level, boolean includeSplitModules) {
        return manager.getModulesOnLevel(level, includeSplitModules);
    }
    
    /**
     * If a module id is supplied, gets the module with the given id.
     * Otherwise, if the given string is a name, the module at the <b>first level</b> with the
     * given name is requested.
     * <p>
     * This method returns modules from the current context only (not from the old context).
     * 
     * @param nameOrId
     *      the name or id of the module
     * 
     * @return
     *      the module, or null if no such module exists
     */
    public IModule getModuleByNameOrId(String nameOrId) {
        IModule module;
        if (ModulePaths.containsPathSeparator(nameOrId)) {
            module = manager.getModule(nameOrId);
        } else {
            module = manager.getModuleByName(nameOrId, 1);
        }
        
        return module;
    }
    
    /**
     * Transfers the given module from the old context to the next context.
     * 
     * @param oldModule
     *      the old module
     * @param transferDependencies
     *      if dependencies should be transferred
     * 
     * @return
     *      the state for the module
     * 
     * @throws IllegalStateException
     *      If the old module could not be transferred
     */
    public IMState transferModule(IModule oldModule, boolean transferDependencies) {
        if (oldContext == null) throw new IllegalStateException("Cannot transfer module " + oldModule + ": there is no old context to transfer from!");
        if (!oldContext.manager.hasModule(oldModule.getId())) throw new IllegalStateException("Cannot transfer module " + oldModule + ": module is unknown in the old context!");
        if (manager.hasModule(oldModule.getId())) throw new IllegalStateException("Cannot transfer module " + oldModule + ": there is already a new module with the same id!");
        
        IMState state = reuseOldState(oldModule);
        if (transferDependencies) transferDependencies(oldModule.getId());
        return state;
    }
    
    // --------------------------------------------------------------------------------------------
    // States
    // --------------------------------------------------------------------------------------------
    
    @Override
    public IMState getState(String moduleId) {
        return states.get(moduleId);
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
        IMState old = states.put(module.getId(), state);
        if (old != null) System.err.println("Overridden state of " + module);
    }
    
    /**
     * Reuses the state of an old module (module from the old context). The reused state is copied
     * and the copy is set as current state of the given module. The given module is also added to
     * this context if it was not already present.
     * 
     * @param oldModule
     *      the module
     * 
     * @return
     *      the copied state for the module
     */
    public IMState reuseOldState(IModule oldModule) {
        if (oldContext == null) throw new IllegalStateException("Old context is null!");
        
        IMState oldState = oldContext.getState(oldModule);
        if (oldState == null) throw new IllegalStateException("Old state of the module is null!");
        
        IMState newState = oldState.copy();
        setState(oldModule, newState);
        if (!manager.hasModule(oldModule.getId())) addModule(oldModule);
        return newState;
    }
    
    // --------------------------------------------------------------------------------------------
    // Dependencies
    // --------------------------------------------------------------------------------------------
    
    /**
     * Gets the dependencies object for the given module. If the given module does not currently
     * have a dependencies object, a new dependencies object will be created.
     * 
     * @param moduleId
     *      the id of the module
     * 
     * @return
     *      the dependencies object for the given module
     */
    @SuppressWarnings("unchecked")
    public <T extends Dependencies> T getDependencies(String moduleId) {
        return (T) dependencies.getDependencies(moduleId);
    }
    
    /**
     * Gets the dependencies object for the given module. If the given module does not currently
     * have a dependencies object, a new dependencies object will be created.
     * 
     * @param module
     *      the module
     * 
     * @return
     *      the dependencies object for the given module
     */
    public <T extends Dependencies> T getDependencies(IModule module) {
        return getDependencies(module.getId());
    }
    
    /**
     * Gets the dependencies of the given module in the previous context.
     * If there is no old context or if the old context does not know about the given module, then
     * this method will return null.
     * 
     * TODO Ensure that the dependencies are initialized.
     * 
     * @param moduleId
     *      the id of the module
     * 
     * @return
     *      the dependencies of the given module
     */
    @SuppressWarnings("unchecked")
    public <T extends Dependencies> T getOldDependencies(String moduleId) {
        if (oldContext == null || !oldContext.getModuleManager().hasModule(moduleId)) return null;
        
        return (T) oldContext.dependencies.getDependencies(moduleId);
    }
    
    /**
     * Transfers dependencies for this module from the old context to the new context.
     * 
     * @param moduleId
     *      the id of the module
     * 
     * @return
     *      the transferred dependencies
     *      
     * @throws IllegalStateException
     *      If there is no old context, or the old context is not aware of the given module.
     */
    public <T extends Dependencies> T transferDependencies(String moduleId) {
        if (oldContext == null) throw new IllegalStateException("The old context is null!");
        T old = getOldDependencies(moduleId);
        if (old == null) throw new IllegalStateException("The given module is unknown in the old context.");
        
        getDependencyManager().setDependencies(moduleId, old.copy());
        return old;
    }
    
    /**
     * Resets the dependencies of the module with the given id.
     * 
     * @param moduleId
     *      the id of the module
     * 
     * @return
     *      the new dependencies
     */
    @SuppressWarnings("unchecked")
    public <T extends Dependencies> T resetDependencies(String moduleId) {
        return (T) dependencies.resetDependencies(moduleId);
    }
    
    @SuppressWarnings("unchecked")
    public <T extends Dependencies> DependencyManager<T> getDependencyManager() {
        return (DependencyManager<T>) dependencies;
    }
    
    // --------------------------------------------------------------------------------------------
    // Phase
    // --------------------------------------------------------------------------------------------
    
    public <T> T getPhase() {
        return incrementalManager.getPhase();
    }
    
    public void setPhase(Object phase) {
        incrementalManager.setPhase(phase);
        
    }
    
    public boolean isInitPhase() {
        return incrementalManager.isInitPhase();
    }
    
    public void finishInitPhase() {
        incrementalManager.finishInitPhase();
    }
    
    @SuppressWarnings("unchecked")
    public <T extends IncrementalManager> T getIncrementalManager() {
        return (T) incrementalManager;
    }
    
    // --------------------------------------------------------------------------------------------
    // Solver coordinator
    // --------------------------------------------------------------------------------------------
    
    public ISolverCoordinator getCoordinator() {
        return coordinator;
    }
    
    public void setCoordinator(ISolverCoordinator coordinator) {
        this.coordinator = coordinator;
        coordinator.setContext(this);
    }
    
    // --------------------------------------------------------------------------------------------
    // Solver results
    // --------------------------------------------------------------------------------------------
    
    public void addResult(IModule module, MSolverResult result) {
        solverResults.put(module, result);
    }
    
    public MSolverResult getResult(IModule module) {
        return solverResults.get(module);
    }

    public Map<IModule, MSolverResult> getResults() {
        return solverResults;
    }
    
    public void setResults(Map<IModule, MSolverResult> results) {
        this.solverResults = hashMap(results);
    }
    
    // --------------------------------------------------------------------------------------------
    // Context transfer
    // --------------------------------------------------------------------------------------------

    /**
     * Commits the changes with regards to the previous context.
     * This call copies over any information from the old context that is relevant and then removes
     * the links to the old context and the change set, finalizing this context.
     */
    public void commitChanges() {
//        //We need to determine if a module has been reused.
//        for (IModule module : oldContext.manager.getModules()) {
//            if (changeSet.removed().contains(module)) continue;
//            
//            //If we have a new version for the module already, skip it
//            if (manager.hasModule(module.getId())) continue;
//            
//            assert module.getTopCleanliness() == ModuleCleanliness.CLEAN : "module flag should be clean if it is not in the new context";
//            module.setFlag(Flag.CLEAN);
//            //TODO The module should get the new context and stuff.
//            manager.addModule(module);
//        }
        
        //For all modules for which we have state, migrate the module itself as well
        //Also clear solvers
        for (IMState state : states.values()) {
            if (!manager.hasModule(state.owner().getId())) {
                System.err.println("Migrating module " + state.owner() + ": state is present, but module is not in current context!");
                addModule(state.owner());
            }
            state.setSolver(null);
        }
        
        //Transfer all dependencies that are not present yet, create dependencies for other modules
        for (IModule module : manager._getModules()) {
            String id = module.getId();
            if (!dependencies.hasDependencies(id)) {
                System.err.println("There are no dependencies for module " + id + "!!!");
            }
        }
        
        //Clean the world
        for (IModule module : getModules()) {
            module.setFlag(Flag.CLEAN);
        }
        
        for (IModule module : Sets.difference(oldContext.manager.getModules(), getModules())) {
            System.err.println("Removed module " + module);
        }
        
        //Clear now unnecessary fields
        oldContext = null;
        changeSet = null;
        coordinator.wipe();
        coordinator = null;
        incrementalManager.wipe();
        //TODO probably need more here
    }
    
    public void wipe() {
        this.changeSet = null;
        if (this.coordinator != null) this.coordinator.wipe();
        this.coordinator = null;
        this.dependencies.wipe();
        this.incrementalManager.wipe();
        this.initConstraints = null;
        this.manager.clearModules();
        this.oldContext = null;
        this.solverResults = null;
        if (this.states != null) this.states.clear();
        this.states = null;
        RedirectingIncrementalCompleteness.RECOVERY.clear();
    }
    
    // --------------------------------------------------------------------------------------------
    // Object methods
    // --------------------------------------------------------------------------------------------
    
    @Override
    public String toString() {
        return "Context(" + hashCode() + ") [strategy=" + strategy
                + ", manager=" + manager
                + ", oldContext=" + oldContext
                + ", changeSet=" + changeSet
                + ", incrementalManager=" + incrementalManager + "]";
//                + ", solverResults=" + solverResults + "]";
    }
    
    // --------------------------------------------------------------------------------------------
    // Creation
    // --------------------------------------------------------------------------------------------
    
    /**
     * Creates a context for when a clean build is requested or no previous results are available.
     * 
     * @param strategy
     *      the incremental strategy to use
     * @param spec
     *      the spec
     * 
     * @return
     *      the new context
     */
    public static Context initialContext(IncrementalStrategy strategy, Spec spec) {
        Context newContext = new Context(strategy, spec, null);
        setContext(newContext);
        return newContext;
    }

    /**
     * Creates a context for when a previous context is available as well as the changeset with
     * regards to that previous context.
     * 
     * @param strategy
     *      the incremental strategy to employ
     * @param previousContext
     *      the previous context
     * @param changeSet
     *      the changeset
     * 
     * @return
     *      the new context
     */
    public static Context incrementalContext(
            IncrementalStrategy strategy, Context previousContext, IMState previousRootState,
            IChangeSet changeSet, Map<String, IConstraint> initConstraints, Spec spec) {
        Context newContext = new Context(strategy, spec, previousContext);
        newContext.changeSet = changeSet;
        newContext.initConstraints = newContext.fixInitConstraints(initConstraints);
        
        //TODO IMPORTANT validate that the state used here is the correct one (should it be the one stored, or the one in the context corresponding to the root?)
        IMState newState = newContext.transferModule(previousRootState.getOwner(), false);
        newContext.resetDependencies(newState.owner().getId()); //Reset dependencies of the top level
        //TODO Important we need to reset the dependants as well
        setContext(newContext);
        
        //Prune removed children
        for (IModule child : changeSet.removed()) {
            newState.scopeGraph().removeChild(child);
        }
        
        ModuleSolver.topLevelSolver(newState, null, new NullDebugContext()); //TODO Does not happen in the clean context, why?
        return newContext;
    }
    
    /**
     * Creates a context specially for testing purposes.
     * This method should not be used outside testing.
     */
    public static Context testContext(IncrementalStrategy strategy, Spec spec,
            @Nullable Context oldContext, @Nullable IChangeSet changeSet) {
        Context newContext = new Context(strategy, spec, oldContext);
        newContext.changeSet = changeSet;
        setContext(newContext);
        return newContext;
    }
    
    // --------------------------------------------------------------------------------------------
    // Context access
    // --------------------------------------------------------------------------------------------
    private static volatile Context currentContext;
    private static volatile ThreadLocal<Context> currentContextThreadSensitive;
    
    /**
     * @return
     *      the current context
     */
    public static Context context() {
        final ThreadLocal<Context> tsContext;
        if ((tsContext = currentContextThreadSensitive) != null) {
            return tsContext.get();
        }
        
        return currentContext;
    }
    
    /**
     * Sets the current global context.
     * 
     * @param context
     *      the context
     */
    public static synchronized void setContext(Context context) {
        currentContext = context;
    }
    
    private static Context _getCurrentContext() {
        return currentContext;
    }
    
    /**
     * Executes the given runnable in the given context. Please note that other threads will not
     * see the given context. In other words, the runnable must not depend on the given context
     * with more than one thread. If multiple threads are required, use
     * {@link #executeInContext(Context, Consumer)}.
     * <p>
     * NOTE: The given runnable must not call {@link #setContext(Context)} or behavior will become
     * undefined.
     * 
     * @param context
     *      the context
     * @param runnable
     *      the runnable to execute
     */
    public static synchronized void executeInContext(Context context, Runnable runnable) {
        currentContextThreadSensitive = ThreadLocal.withInitial(Context::_getCurrentContext);
        currentContextThreadSensitive.set(context);
        try {
            runnable.run();
        } finally {
            currentContextThreadSensitive = null;
        }
    }
    
    /**
     * Executes the given supplier in the given context. Please note that other threads will not
     * see the given context. In other words, the supplier must not depend on the given context
     * with more than one thread. If multiple threads are required, use
     * {@link #executeInContext(Context, Function)}.
     * <p>
     * NOTE: The given supplier must not call {@link #setContext(Context)} or behavior will become
     * undefined.
     * 
     * @param context
     *      the context
     * @param supplier
     *      the supplier to execute
     */
    public static synchronized <T> T executeInContext(Context context, Supplier<T> supplier) {
        currentContextThreadSensitive = ThreadLocal.withInitial(Context::_getCurrentContext);
        currentContextThreadSensitive.set(context);
        try {
            return supplier.get();
        } finally {
            currentContextThreadSensitive = null;
        }
    }
    
    /**
     * Executes the given consumer in the given context. The consumer can create new threads, but
     * these threads need to execute the runnable that is supplied to the consumer in order to run
     * with the given context. Care should be taken that all spawned threads complete before the
     * consumer returns. Whenever the consumer returns, the context will no longer be the given
     * context.
     * <p>
     * NOTE: The given consumer must not call {@link #setContext(Context)} or behavior will become
     * undefined.
     * 
     * @param context
     *      the context
     * @param consumer
     *      the consumer to execute
     */
    public static synchronized void executeInContext(Context context, Consumer<Runnable> consumer) {
        currentContextThreadSensitive = ThreadLocal.withInitial(Context::_getCurrentContext);
        currentContextThreadSensitive.set(context);
        try {
            consumer.accept(() -> {
                final ThreadLocal<Context> tsContext = currentContextThreadSensitive;
                if (tsContext != null) tsContext.set(context);
            });
        } finally {
            currentContextThreadSensitive = null;
        }
    }
    
    /**
     * Executes the given function in the given context. The function can create new threads, but
     * these threads need to execute the runnable that is supplied to the function in order to run
     * with the given context. Care should be taken that all spawned threads complete before the
     * function returns. Whenever the function returns, the context will no longer be the given
     * context and the context will be the original context.
     * <p>
     * NOTE: The given function must not call {@link #setContext(Context)} or behavior will become
     * undefined.
     * 
     * @param context
     *      the context
     * @param function
     *      the function to execute
     */
    public static synchronized <T> T executeInContext(Context context, Function<Runnable, T> function) {
        currentContextThreadSensitive = ThreadLocal.withInitial(Context::_getCurrentContext);
        currentContextThreadSensitive.set(context);
        try {
            return function.apply(() -> {
                final ThreadLocal<Context> tsContext = currentContextThreadSensitive;
                if (tsContext != null) tsContext.set(context);
            });
        } finally {
            currentContextThreadSensitive = null;
        }
    }
}
