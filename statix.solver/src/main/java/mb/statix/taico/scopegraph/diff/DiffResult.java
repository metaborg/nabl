package mb.statix.taico.scopegraph.diff;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import mb.nabl2.terms.ITerm;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.taico.scopegraph.IMInternalScopeGraph;

/**
 * Class which holds the results of a diff on scope graph. This diff contains
 * {@link ScopeGraphDiff}s for all requested modules, as well as a set of all modules that were
 * removed and a set of all modules that were added.
 */
public class DiffResult implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Map<String, ScopeGraphDiff> diffs = Collections.synchronizedMap(new HashMap<>());
    private Map<String, IMInternalScopeGraph<Scope, ITerm, ITerm>> addedModules;
    private Map<String, IMInternalScopeGraph<Scope, ITerm, ITerm>> removedModules;
    
    public DiffResult() {
        this.addedModules   = Collections.synchronizedMap(new HashMap<>());
        this.removedModules = Collections.synchronizedMap(new HashMap<>());
    }
    
    protected DiffResult(Map<String, IMInternalScopeGraph<Scope, ITerm, ITerm>> addedModules, Map<String, IMInternalScopeGraph<Scope, ITerm, ITerm>> removedModules) {
        this.addedModules = addedModules;
        this.removedModules = removedModules;
    }
    
    public Map<String, ScopeGraphDiff> getDiffs() {
        return diffs;
    }
    
    /**
     * @return
     *      a map of added modules 
     */
    public Map<String, IMInternalScopeGraph<Scope, ITerm, ITerm>> getAddedModules() {
        return addedModules;
    }
    
    public Map<String, IMInternalScopeGraph<Scope, ITerm, ITerm>> getRemovedModules() {
        return removedModules;
    }

    public void addDiff(String module, ScopeGraphDiff diff) {
        diffs.put(module, diff);
    }
    
    public boolean hasDiffResult(String module) {
        return diffs.containsKey(module);
    }
    
    public void addRemovedChild(String id, IMInternalScopeGraph<Scope, ITerm, ITerm> graph) {
        removedModules.put(id, graph);
    }
    
    public void addAddedChild(String id, IMInternalScopeGraph<Scope, ITerm, ITerm> graph) {
        addedModules.put(id, graph);
    }
    
    /**
     * Gets the diff for the given module, or creates an empty one.
     * 
     * @param module
     *      the module
     * 
     * @return
     *      the diff of the given module
     */
    ScopeGraphDiff getOrCreateDiff(String module) {
        return diffs.computeIfAbsent(module, m -> ScopeGraphDiff.empty());
    }
    
    public DiffResult toEffectiveDiff() {
        DiffResult effective = new DiffResult(addedModules, removedModules);
        
        //We need to move the different declarations to the owners of the scopes
        synchronized (diffs) {
            for (Entry<String, ScopeGraphDiff> entry : diffs.entrySet()) {
                final String module = entry.getKey();
                final ScopeGraphDiff diff = entry.getValue().retainScopes();
                effective.addDiff(module, diff);
            }
        }
        
        synchronized (diffs) {
            for (ScopeGraphDiff diff : diffs.values()) {
                diff.toEffectiveDiff(effective);
            }
        }
        
        return effective;
    }
    
    public void print(PrintStream stream) {
        stream.println("Diff:");
        stream.println("| Added Modules:");
        for (String module : addedModules.keySet()) {
            stream.println("| | " + module);
        }
        stream.println("| Removed Modules:");
        for (String module : removedModules.keySet()) {
            stream.println("| | " + module);
        }
        stream.println("| Scope graph diffs");
        for (Entry<String, ScopeGraphDiff> entry : diffs.entrySet()) {
            if (entry.getValue().isEmpty()) {
                stream.println("| | " + entry.getKey() + ": UNCHANGED");
            } else {
                stream.println("| | " + entry.getKey() + ":");
                entry.getValue().print(stream, 3);
            }
        }
    }
}
