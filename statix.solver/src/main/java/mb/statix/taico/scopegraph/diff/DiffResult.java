package mb.statix.taico.scopegraph.diff;

import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import mb.statix.taico.scopegraph.IMInternalScopeGraph;

/**
 * Class which holds the results of a diff on scope graph. This diff contains
 * {@link ScopeGraphDiff}s for all requested modules, as well as a set of all modules that were
 * removed and a set of all modules that were added.
 *
 * @param <S>
 *      the type of scopes
 * @param <L>
 *      the type of labels
 * @param <D>
 *      the type of data
 */
public class DiffResult<S extends D, L, D> {
    private Map<String, ScopeGraphDiff<S, L, D>> diffs = Collections.synchronizedMap(new HashMap<>());
    private Map<String, IMInternalScopeGraph<S, L, D>> removedModules = Collections.synchronizedMap(new HashMap<>());
    private Map<String, IMInternalScopeGraph<S, L, D>> addedModules   = Collections.synchronizedMap(new HashMap<>());
    
    public Map<String, ScopeGraphDiff<S, L, D>> getDiffs() {
        return diffs;
    }
    
    /**
     * @return
     *      a map of added modules 
     */
    public Map<String, IMInternalScopeGraph<S, L, D>> getAddedModules() {
        return addedModules;
    }
    
    public Map<String, IMInternalScopeGraph<S, L, D>> getRemovedModules() {
        return removedModules;
    }

    public void addDiff(String module, ScopeGraphDiff<S, L, D> diff) {
        diffs.put(module, diff);
    }
    
    public boolean hasDiffResult(String module) {
        return diffs.containsKey(module);
    }
    
    public void addRemovedChild(String id, IMInternalScopeGraph<S, L, D> graph) {
        removedModules.put(id, graph);
    }
    
    public void addAddedChild(String id, IMInternalScopeGraph<S, L, D> graph) {
        addedModules.put(id, graph);
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
        for (Entry<String, ScopeGraphDiff<S, L, D>> entry : diffs.entrySet()) {
            if (entry.getValue().isEmpty()) {
                stream.println("| | " + entry.getKey() + ": UNCHANGED");
            } else {
                stream.println("| | " + entry.getKey() + ":");
                entry.getValue().print(stream, 3);
            }
        }
    }
}
