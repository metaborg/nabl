package mb.statix.taico.ndependencies;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

import com.google.common.collect.ListMultimap;

import mb.statix.taico.dependencies.Dependencies;
import mb.statix.taico.dependencies.Dependency;
import mb.statix.taico.dependencies.details.IDependencyDetail;
import mb.statix.taico.module.IModule;
import mb.statix.taico.solver.Context;
import mb.statix.taico.util.TPrettyPrinter;

/**
 * Class to represent dependencies of a module.
 */
public class NDependencies extends Dependencies implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public NDependencies(String owner) {
        super(owner);
    }
    
    @Override
    public Dependency addDependency(String module, IDependencyDetail... details) {
        Dependency dependency = new Dependency(owner, module, details);
        dependencies.put(module, dependency);
        
        Context.context().getDependencyManager().onDependencyAdded(dependency);
        
        return dependency;
    }
    
    // --------------------------------------------------------------------------------------------
    // Dependants
    // --------------------------------------------------------------------------------------------
    
    @Override
    public ListMultimap<String, Dependency> getDependants() {
        return dependants; //Is always empty
    }
    
    @Override
    public Set<IModule> getModuleDependants() {
        return Collections.emptySet();
    }
    
    @Override
    public Set<String> getModuleDependantIds() {
        return Collections.emptySet();
    }
    
    @Override
    public Dependency addDependant(String module, Dependency dependency) {
        throw new UnsupportedOperationException();
    }
    
    // --------------------------------------------------------------------------------------------
    // Copy
    // --------------------------------------------------------------------------------------------
    
    @Override
    public NDependencies copy() {
        NDependencies copy = new NDependencies(owner);
        copy.dependencies.putAll(dependencies);
        return copy;
    }
    
    // --------------------------------------------------------------------------------------------
    // Object methods
    // --------------------------------------------------------------------------------------------
    
    @Override
    public String toString() {
        return dependencies.toString();
    }

    @Override
    public String print(boolean pretty, int indent) {
        StringBuilder base = new StringBuilder();
        for (int i = 0; i < indent; i++) base.append("| ");
        final String s = base.toString();
        
        final StringBuilder sb = new StringBuilder();
        sb.append(s + "Dependencies of ");
        sb.append(pretty ? TPrettyPrinter.printModule(owner) : owner);
        sb.append(" {\n");
        
        for (Dependency dependency : dependencies.values()) {
            sb.append(s + "| ");
            sb.append(dependency.print(pretty));
            sb.append("\n");
        }
        sb.append(s + "}");
        return sb.toString();
    }
}