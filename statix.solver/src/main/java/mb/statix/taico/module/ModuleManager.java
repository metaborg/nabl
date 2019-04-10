package mb.statix.taico.module;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;

public class ModuleManager {
    private Map<String, IModule> modules = new HashMap<>();
    private ListMultimap<String, IModule> moduleNames = MultimapBuilder.hashKeys().arrayListValues().build();
    
    public ModuleManager() {}
    
    /**
     * @return
     *      all the modules registered in this manager
     */
    public Collection<IModule> getModules() {
        //TODO Should be a copy
        return modules.values();
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
     * @param module
     *      the module to add
     * 
     * @throws IllegalStateException
     *      If a module with the given uid already exists, which is not the same module as the
     *      given module.
     */
    public synchronized void addModule(IModule module) {
        final IModule old = modules.putIfAbsent(module.getId(), module);
        moduleNames.put(module.getName(), module);
        if (old == null) return;
        
        if (old == module) {
            System.err.println("[ModuleManager] Added module " + module.getId() + " twice");
        } else {
            throw new IllegalStateException("[ModuleManager] Duplicate ID " + old.getId() + " discovered when adding module.");
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
        for (IModule child : module.getChildren()) {
            purgeModules(child);
        }
    }
}
