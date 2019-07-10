package mb.statix.taico.incremental.changeset;

import static mb.statix.taico.module.ModuleCleanliness.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import mb.statix.taico.incremental.Flag;
import mb.statix.taico.module.IModule;
import mb.statix.taico.module.ModuleCleanliness;
import mb.statix.taico.solver.SolverContext;

public abstract class AChangeSet implements IChangeSet {
    private static final long serialVersionUID = 1L;
    
    protected final EnumMap<ModuleCleanliness, Set<IModule>> modules = new EnumMap<>(ModuleCleanliness.class);
    protected final EnumMap<ModuleCleanliness, Set<String>> ids = new EnumMap<>(ModuleCleanliness.class);
    
    public AChangeSet(SolverContext oldContext, Iterable<ModuleCleanliness> supported) {
        this(oldContext, supported, Collections.emptySet(), Collections.emptySet(), Collections.emptySet());
    }
    
    /**
     * The constructor will:
     * 1. Flag all modules as CLEAN
     * 2. Flag all deleted modules as DELETED
     * 3. Flag all changed modules as DIRTY
     * 4. Add the given modules to their respective sets
     * 
     * @param oldContext
     *      the old context
     * @param supported
     *      the iterable of supported module cleanlinesses
     * @param added
     *      the set of added modules (names)
     * @param changed
     *      the set of changed modules (names (top level) or ids)
     * @param removed
     *      the set of removed modules (names (top level) or ids)
     */
    public AChangeSet(SolverContext oldContext, Iterable<ModuleCleanliness> supported, Collection<String> added, Collection<String> changed, Collection<String> removed) {
        //Add all the required sets
        for (ModuleCleanliness mc : supported) {
            modules.put(mc, createSet());
            ids.put(mc, createSet());
        }
        
        //Flag all modules as clean
        oldContext.getModules().forEach(m -> m.setFlag(Flag.CLEAN));
        
        //Add all added modules
        ids.computeIfAbsent(NEW, k -> createSet()).addAll(added);
        
        add(new Flag(DIRTY, 1), FlagCondition.OverrideFlag, changed.stream().map(name -> getModule(oldContext, name)).toArray(IModule[]::new));
        add(Flag.DELETED, FlagCondition.OverrideFlag, removed.stream().map(name -> getModule(oldContext, name)).toArray(IModule[]::new));
    }
    
    /**
     * @param oldContext
     *      the old context
     * @param nameOrId
     *      the name or id of the module
     * 
     * @return
     *      the module
     */
    protected IModule getModule(SolverContext oldContext, String nameOrId) {
        IModule module = oldContext.getModuleByNameOrId(nameOrId);
        
        //TODO Use id by using the name of the parent.
        if (module == null) throw new IllegalStateException("Encountered module that is unknown: " + nameOrId);
        return module;
    }
    
    /**
     * Should be called to initialize the change set.
     * 
     * @param oldContext
     *      the old context
     */
    protected abstract void init(SolverContext oldContext);

    @Override
    public EnumMap<ModuleCleanliness, Set<IModule>> cleanlinessToModule() {
        return modules;
    }

    @Override
    public EnumMap<ModuleCleanliness, Set<String>> cleanlinessToId() {
        return ids;
    }
    
    public Set<ModuleCleanliness> getSupported() {
        return ids.keySet();
    }

    protected boolean add(Flag flag, FlagCondition condition, IModule module) {
        return add(flag, condition, Collections.singletonList(module));
    }
    
    protected boolean add(Flag flag, FlagCondition condition, IModule... modules) {
        return add(flag, condition, Arrays.asList(modules));
    }
    
    protected boolean add(Flag flag, FlagCondition condition, Iterable<IModule> modules) {
        Set<IModule> sModules = getModules(flag.getCleanliness());
        Set<String> sIds = getIds(flag.getCleanliness());
        boolean tbr = false;
        for (IModule module : modules) {
            switch (condition) {
                case AddFlag:
                    module.addFlag(flag);
                    break;
                case AddFlagIfNotSameCause:
                    if (!module.addFlagIfNotSameCause(flag)) continue;
                    break;
                case OverrideFlag:
                    module.setFlag(flag);
                    break;
                case FlagIfClean:
                    if (!module.setFlagIfClean(flag)) continue;
                    break;
                case FlagIfCleanNoReturn:
                    module.setFlagIfClean(flag);
                    break;
                case DontFlag:
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
            sModules.add(module);
            sIds.add(module.getId());
            tbr = true;
        }
        return tbr;
    }
    
    protected boolean add(Flag flag, FlagCondition condition, Stream<IModule> modules) {
        return add(flag, condition, modules::iterator);
    }
    
    /**
     * Overriding implementations can choose the type of set that should be used for storing the
     * modules and ids.
     * 
     * @return
     *      a newly created set
     */
    protected <T> Set<T> createSet() {
        return new HashSet<>();
    }
    
    public static enum FlagCondition {
        FlagIfClean,
        FlagIfCleanNoReturn,
        AddFlag,
        AddFlagIfNotSameCause,
        OverrideFlag,
        DontFlag
    }
}
