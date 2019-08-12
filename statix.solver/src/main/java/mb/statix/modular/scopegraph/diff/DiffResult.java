package mb.statix.modular.scopegraph.diff;

import static mb.statix.modular.util.TPrettyPrinter.*;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.Map.Entry;

import mb.nabl2.terms.ITerm;
import mb.nabl2.util.Tuple2;
import mb.statix.modular.dependencies.Dependency;
import mb.statix.modular.dependencies.DependencyManager;
import mb.statix.modular.name.Name;
import mb.statix.modular.scopegraph.IMInternalScopeGraph;
import mb.statix.modular.solver.Context;
import mb.statix.modular.unifier.DistributedUnifier;
import mb.statix.modular.util.TPrettyPrinter;
import mb.statix.scopegraph.terms.Scope;

/**
 * Class which holds the results of a diff on scope graph. This diff contains
 * {@link ScopeGraphDiff}s for all requested modules, as well as a set of all modules that were
 * removed and a set of all modules that were added.
 */
public class DiffResult implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Map<String, IScopeGraphDiff<Scope, ITerm, ITerm>> diffs = Collections.synchronizedMap(new HashMap<>());
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
    
    public Map<String, IScopeGraphDiff<Scope, ITerm, ITerm>> getDiffs() {
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

    public void addDiff(String module, IScopeGraphDiff<Scope, ITerm, ITerm> diff) {
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
        return (ScopeGraphDiff) diffs.computeIfAbsent(module, m -> ScopeGraphDiff.empty());
    }
    
    public DiffResult toEffectiveDiff() {
        DiffResult effective = new DiffResult(addedModules, removedModules);
        
        //We need to move the different declarations to the owners of the scopes
        synchronized (diffs) {
            for (Entry<String, IScopeGraphDiff<Scope, ITerm, ITerm>> entry : diffs.entrySet()) {
                final String module = entry.getKey();
                final ScopeGraphDiff diff = entry.getValue().retainScopes();
                effective.addDiff(module, diff);
            }
        }
        
        synchronized (diffs) {
            for (IScopeGraphDiff<Scope, ITerm, ITerm> diff : diffs.values()) {
                diff.toEffectiveDiff(effective);
            }
        }
        
        return effective;
    }
    
    public void print(PrintStream stream) {
        final Context context = Context.context();
        
        stream.println("Diff:");
        stream.println("| Added Modules:");
        for (String module : addedModules.keySet()) {
            stream.println("| | " + printModule(module, true));
        }
        stream.println("| Removed Modules:");
        for (String module : removedModules.keySet()) {
            stream.println("| | " + printModule(module, true));
        }
        stream.println("| Scope graph diffs");
        for (Entry<String, IScopeGraphDiff<Scope, ITerm, ITerm>> entry : diffs.entrySet()) {
            final String module = entry.getKey();
            
            if (entry.getValue().isEmpty()) {
                stream.println("| | " + printModule(module, true) + ": UNCHANGED");
            } else {
                stream.println("| | " + printModule(module, true) + ":");
                entry.getValue().print(stream, 3,
                        context.getUnifierOrDefault(module, DistributedUnifier.NULL_UNIFIER),
                        context,
                        context.getOldUnifierOrDefault(module, DistributedUnifier.NULL_UNIFIER),
                        context.getOldContext());
            }
        }
    }
    
    /**
     * Determines the dependencies that are affected by the given diff.
     * 
     * Please note that the given function can be called multiple times with the same dependency.
     * 
     * @param context
     *      the context to get the dependencies from
     * @param function
     *      a PURE function converting the dependency to some value R
     * 
     * @return
     *      a set with all the converted dependencies
     */
    public <R> Set<R> getDependencies(Context context, Function<Dependency, R> function) {
        Set<R> tbr = new HashSet<>();
        DependencyManager<?> manager = context.getDependencyManager();
        
        boolean ea = manager.edgeAdditionAffectScore() != -1;
        boolean er = manager.edgeRemovalAffectScore() != -1;
        boolean da = manager.dataAdditionAffectScore() != -1;
        boolean dr = manager.dataRemovalAffectScore() != -1;
        boolean dna = manager.dataNameAdditionAffectScore() != -1;
        boolean dnroc = manager.dataNameRemovalOrChangeAffectScore() != -1;
        for (IScopeGraphDiff<Scope, ITerm, ITerm> diff : diffs.values()) {
            System.out.println("Checking diff...");
            if (ea) {
                for (Tuple2<Scope, ITerm> edge : diff.getAddedEdges()._getForwardMap().keySet()) {
                    for (Dependency dependency : manager.affectedByEdgeAddition(edge._1(), edge._2())) {
                        System.out.println("Found dependency for edge addition: " + TPrettyPrinter.printEdge(edge._1(), edge._2()));
                        tbr.add(function.apply(dependency));
                    }
                }
            }
            
            if (er) {
                for (Tuple2<Scope, ITerm> edge : diff.getRemovedEdges()._getForwardMap().keySet()) {
                    for (Dependency dependency : manager.affectedByEdgeRemoval(edge._1(), edge._2())) {
                        System.out.println("Found dependency for edge removal: " + TPrettyPrinter.printEdge(edge._1(), edge._2()));
                        tbr.add(function.apply(dependency));
                    }
                }
            }
            
            if (da) {
                for (Tuple2<Scope, ITerm> edge : diff.getAddedData()._getForwardMap().keySet()) {
                    for (Dependency dependency : manager.affectedByDataAddition(edge._1(), edge._2())) {
                        System.out.println("Found dependency for data addition: " + TPrettyPrinter.printEdge(edge._1(), edge._2()));
                        tbr.add(function.apply(dependency));
                    }
                }
            }
            
            if (dr) {
                for (Tuple2<Scope, ITerm> edge : diff.getRemovedData()._getForwardMap().keySet()) {
                    for (Dependency dependency : manager.affectedByDataRemoval(edge._1(), edge._2())) {
                        System.out.println("Found dependency for data removal: " + TPrettyPrinter.printEdge(edge._1(), edge._2()));
                        tbr.add(function.apply(dependency));
                    }
                }
            }
            
            //TODO Do not use names but just straight data?
            if (dna) {
                for (Tuple2<Scope, ITerm> edge : diff.getAddedDataNames()._getForwardMap().keySet()) {
                    final Scope scope = edge._1();
                    final ITerm label = edge._2();
                    for (Name name : diff.getAddedDataNames().get(scope, label)) {
                        for (Dependency dependency : manager.affectedByDataNameAddition(name.withRelation(label), scope)) {
                            System.out.println("Found dependency for data addition: " + TPrettyPrinter.printEdge(scope, label));
                            tbr.add(function.apply(dependency));
                        }
                    }
                }
            }
            
            if (dnroc) {
                for (Tuple2<Scope, ITerm> edge : diff.getRemovedDataNames()._getForwardMap().keySet()) {
                    final Scope scope = edge._1();
                    final ITerm label = edge._2();
                    for (Name name : diff.getRemovedDataNames().get(scope, label)) {
                        for (Dependency dependency : manager.affectedByDataNameRemovalOrChange(name.withRelation(label), scope)) {
                            System.out.println("Found dependency for data removal: " + TPrettyPrinter.printEdge(scope, label));
                            tbr.add(function.apply(dependency));
                        }
                    }
                }
                
                for (Tuple2<Scope, ITerm> edge : diff.getChangedDataNames()._getForwardMap().keySet()) {
                    final Scope scope = edge._1();
                    final ITerm label = edge._2();
                    for (Name name : diff.getChangedDataNames().get(scope, label)) {
                        for (Dependency dependency : manager.affectedByDataNameRemovalOrChange(name.withRelation(label), scope)) {
                            System.out.println("Found dependency for data change: " + TPrettyPrinter.printEdge(scope, label));
                            tbr.add(function.apply(dependency));
                        }
                    }
                }
            }
        }
        
        return tbr;
    }
}
