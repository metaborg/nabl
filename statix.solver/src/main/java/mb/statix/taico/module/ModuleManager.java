package mb.statix.taico.module;

import java.util.WeakHashMap;

public class ModuleManager {
    private static WeakHashMap<String, IModule> modules = new WeakHashMap<>();
    
    private ModuleManager() {}
    
    public static synchronized IModule getModule(String id) {
        return modules.get(id);
    }
    
    public static synchronized void addModule(IModule module) {
        final IModule old = modules.putIfAbsent(module.getId(), module);
        if (old == null) return;
        
        if (old == module) {
            System.err.println("[ModuleManager] Added module " + module.getId() + " twice");
        } else {
            throw new IllegalStateException("[ModuleManager] Duplicate ID " + old.getId() + " discovered when adding module.");
        }
    }
    
    public static synchronized void removeModule(IModule module) {
        modules.remove(module.getId());
    }
    
    /**
     * Removes this module and all children, direct and indirect.
     * 
     * @param module
     */
    public static synchronized void purgeModules(IModule module) {
        modules.remove(module.getId());
        for (IModule child : module.getChildren()) {
            purgeModules(child);
        }
    }
}
