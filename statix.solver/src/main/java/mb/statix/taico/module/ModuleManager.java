package mb.statix.taico.module;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;

public class ModuleManager implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Map<String, IModule> modules = new ConcurrentHashMap<>();
    private ListMultimap<String, IModule> moduleNames = MultimapBuilder.hashKeys().arrayListValues().build();
    
    public ModuleManager() {}
    
    /**
     * @return
     *      all the modules registered in this manager
     */
    public synchronized Set<IModule> getModules() {
        return new HashSet<>(modules.values());
    }
    
    /**
     * @return
     *      all the modules and their ids
     */
    public synchronized Map<String, IModule> getModulesAndIds() {
        return new HashMap<>(modules);
    }
    
    /**
     * @param id
     *      the unique id of the module
     * 
     * @return
     *      the module with the given id, or null if no such module exists
     */
    public synchronized IModule getModule(String id) {
        return modules.get(id);
    }
    
    /**
     * @param name
     *      the name of the module
     * 
     * @return
     *      the given module, or null if no module with the given name exists
     * 
     * @throws IllegalStateException
     *      If the given name is not globally unique
     */
    public synchronized IModule getModuleByName(String name) {
        System.err.println("getModuleByName request for " + name);
        List<IModule> mods = moduleNames.get(name);
        if (mods.isEmpty()) return null;
        if (mods.size() == 1) return mods.get(0);
        
        throw new IllegalStateException("[ModuleManager] Module " + name + " is not globally unique, use a full id instead");
    }
    
    /**
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
    public synchronized IModule getModuleByName(String name, int level) {
        List<IModule> mods = moduleNames.get(name);
        List<IModule> filtered = mods.stream().filter(m -> ModulePaths.pathLength(m.getId()) - 1 == level).collect(Collectors.toList());
        if (filtered.size() > 1) throw new IllegalStateException("[ModuleManager] Module " + name + " is not unique on its level. Use a full id instead");
        return filtered.isEmpty() ? null : filtered.get(0);
    }
    
    /**
     * @param id
     *      the id of the module to check
     * 
     * @return
     *      true if the given module exists, false otherwise
     */
    public synchronized boolean hasModule(String id) {
        return modules.containsKey(id);
    }
    
    /**
     * @return
     *      a set with all top level modules
     */
    public synchronized Set<IModule> topLevelModules() {
        return modules.values().stream().filter(m -> ModulePaths.pathSegments(m.getId(), 2).length == 1).collect(Collectors.toSet());
    }
    
    /**
     * @param module
     *      the module to add
     * 
     * @throws IllegalStateException
     *      If a module with the given uid already exists, which is not the same module as the
     *      given module.
     */
    public synchronized void addModule(IModule module) {
        System.err.println("[ModuleManager " + this + "] Adding module " + module.getId());
        final IModule old = modules.put(module.getId(), module);
        moduleNames.put(module.getName(), module);
        if (old == null) return;
        
        if (old == module) {
            System.err.println("[ModuleManager] Added module " + module.getId() + " twice");
        } else if (old.getFlag() != ModuleCleanliness.CLEAN) {
            System.err.println("[ModuleManager] Module " + old.getId() + " replaced with new version.");
        } else {
            System.err.println("[ModuleManager] Duplicate ID " + old.getId() + " discovered when adding module.");
        }
    }
    
    /**
     * Removes the given module from this manager.
     * 
     * @param module
     *      the module to remove
     */
    public synchronized void removeModule(IModule module) {
        modules.remove(module.getId());
        moduleNames.remove(module.getName(), module);
    }
    
    /**
     * Removes this module and all children, direct and indirect.
     * 
     * @param module
     *      the module to purge
     */
    public synchronized void purgeModules(IModule module) {
        modules.remove(module.getId());
        moduleNames.remove(module.getName(), module);
        module.getScopeGraph().purgeChildren();
        for (IModule child : module.getChildren()) {
            purgeModules(child);
        }
    }
    
    /**out
     * Removes all modules.
     */
    public synchronized void clearModules() {
        for (IModule module : topLevelModules()) {
            purgeModules(module);
        }
        modules.clear();
        moduleNames.clear();
    }
    
    /**
     * Retains only the given modules.
     * 
     * @param modules
     *      the modules to retain
     */
    public synchronized void retainModules(Collection<IModule> toRetain) {
        modules.values().retainAll(toRetain);
        moduleNames.values().retainAll(toRetain);
    }

    /**
     * Gets the modules on a particular level.
     * For example, a value of 0 yields all top level modules, while a value of 1 would yield all
     * children of top level modules.
     * 
     * @param level
     *      the level of modules to get
     * 
     * @return
     *      a map from module NAME to module
     */
    public synchronized Map<String, IModule> getModulesOnLevel(int level) {
        Map<String, IModule> levelModules = new HashMap<>();
        for (Entry<String, IModule> entry : modules.entrySet()) {
            String id = entry.getKey();
            if (ModulePaths.pathLength(id) - 1 != level) continue;
            IModule module = entry.getValue();
            levelModules.put(module.getName(), module);
        }
        return levelModules;
    }
}