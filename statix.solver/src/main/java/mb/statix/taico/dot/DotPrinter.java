package mb.statix.taico.dot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import mb.nabl2.terms.ITerm;
import mb.nabl2.util.collections.HashTrieRelation3;
import mb.nabl2.util.collections.IRelation3;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.taico.module.IModule;
import mb.statix.taico.solver.MSolverResult;

public class DotPrinter {
    protected final MSolverResult initial;
    protected final IModule root;
    protected final Set<IModule> modules;
    protected IRelation3<Scope, ITerm, Scope> edges;
    protected IRelation3<Scope, ITerm, ITerm> data;
    protected List<Scope> scopes;
    
    protected StringBuilder sb;
    protected String result;
    protected String resultNonModular;
    
    public DotPrinter(MSolverResult initial, String file) {
        this.initial = initial;
        if (file == null) {
            //Use the root module and print all modules
            this.root = initial.state().getOwner();
            this.modules = initial.context().getModules();
        } else {
            this.root = findFileModule(file);
            this.modules = this.root.getDescendantsIncludingSelf().collect(Collectors.toSet());
        }
        
        determineEdgesAndData(modules);
    }
    
    /**
     * Finds the given module as child of the root module.
     * 
     * @param name
     *      the name of the file
     * 
     * @return
     *      the module
     * 
     * @throws NullPointerException
     *      if the given module cannot be found.
     */
    protected IModule findFileModule(String name) {
        IModule module = initial.context().getModulesOnLevel(1).get(name);
        if (module == null) throw new NullPointerException("Module " + name + " not found (in " + initial.context().getModulesOnLevel(1) + ")");
        return module;
    }
    
    /**
     * Determines the edges and the data from the collection of modules.
     * 
     * @param modules
     *      the modules to use the edges and data from
     */
    protected void determineEdgesAndData(Collection<IModule> modules) {
        List<Scope> scopes = new ArrayList<>();
        IRelation3.Transient<Scope, ITerm, Scope> edges = HashTrieRelation3.Transient.of();
        IRelation3.Transient<Scope, ITerm, ITerm> data = HashTrieRelation3.Transient.of();
        modules.stream().map(IModule::getScopeGraph).forEach(m -> {
            scopes.addAll(m.getScopes());
            edges.putAll(m.getOwnEdges());
            data.putAll(m.getOwnData());
        });
        
        this.scopes = scopes;
        this.edges = edges;
        this.data = data;
    }
    
    public synchronized String printDot() {
        if (result != null) return result;
        
        sb = new StringBuilder(128);
        startHeader();
        printModuleHierarchy(root, 2);
        printEdges(2);
        printDataEdges(2);
        endHeader();
        result = sb.toString();
        sb = null;
        return result;
    }
    
    public synchronized String printNonModularDot() {
        //TODO
        if (resultNonModular != null) return resultNonModular;
        
        sb = new StringBuilder(128);
        startHeader();
        printModuleHierarchy(root, 2);
        printEdges(2);
        printDataEdges(2);
        endHeader();
        
        resultNonModular = sb.toString();
        sb = null;
        return resultNonModular;
    }
    
    // --------------------------------------------------------------------------------------------
    // Header
    // --------------------------------------------------------------------------------------------
    
    /**
     * <pre>
     * digraph scope_graph {
     *   layout=fdp;
     *   overlap=scale;
     *   rankdir="BT";
     *   &lt;body&gt;
     * }
     * </pre>
     */
    protected void startHeader() {
        appendLn("digraph scope_graph {");
//        appendLn("  layout=fdp;");
//        appendLn("  overlap=scale;");
        appendLn("  rankdir=\"BT\";");
    }
    
    protected void endHeader() {
        appendLn("}");
    }
    
    // --------------------------------------------------------------------------------------------
    // 
    // --------------------------------------------------------------------------------------------
    
    public void printScopes(Iterable<Scope> scopes, int indent) {
        for (Scope scope : scopes) {
            printScope(scope, indent);
        }
    }
    
    /**
     * Prints a single scope.
     * 
     * <pre>
     * "{name}" [shape="ellipse"];
     * {edges}
     * {data-edges}
     * 
//     * { rank="same";
//     *   {decls}
//     * }
     * </pre>
     * 
     * @param scope
     *      the scope to print
     * @param indent
     *      the indent
     *///        List<String> items = new ArrayList<>();
//  module.getScopeGraph().getScopes().stream().map(DotPrinter::name).map(DotPrinter::quote).forEach(items::add);
//  module.getScopeGraph().getOwnData().valueSet().stream().map(i -> quote(escape(trim(i.toString())))).forEach(items::add);
////  module.getChildren().stream().map(IModule::getName).map(DotPrinter::quote).forEach(items::add);
  
    public void printScope(Scope scope, int indent) {
        indent(indent).append(quote(name(scope))).append(" [shape=\"ellipse\"];");
        ln();
//        printEdges(name(scope), edges.get(scope), indent);
//        printDataEdges(name(scope), data.get(scope), indent);
    }
    
    /**
     * Prints the given edges for the given scope.
     * 
     * <pre>
     * "{scope-name}" -> "{name}" [label="{lbl}"];
     * </pre>
     * 
     * @param scopeName
     *      the name of the scope
     * @param edges
     *      the edges of the scope
     * @param indent
     *      the indent
     */
    public void printEdges(String scopeName, Set<? extends Entry<ITerm, Scope>> edges, int indent) {
        for (Entry<ITerm, Scope> entry : edges) {
            String label = label(entry.getKey());
            String targetName = name(entry.getValue());
            
            //"{scope-name}" -> "{name}" [label="{lbl}"];
            indent(indent).append(quote(scopeName)).append(" -> ").append(quote(targetName)).append(" [label=").append(quote(label)).append("];");
            ln();
        }
    }
    
    /**
     * Prints all the edges by calling {@link #printEdges(String, Set, int)}.
     * 
     * @param indent
     *      the indent
     */
    public void printEdges(int indent) {
        for (Scope base : edges.keySet()) {
            printEdges(name(base), edges.get(base), indent);
        }
    }
    
    /**
     * Prints the given data for the given scope.
     * 
     * <pre>
     * "{scope-name}" -> "{name}" [label="{lbl}", arrowhead="box"];
     * </pre>
     * 
     * @param scopeName
     *      the name of the scope
     * @param data
     *      the data of the scope
     * @param indent
     *      the indent
     */
    public void printDataEdges(String scopeName, Set<? extends Entry<ITerm, ITerm>> data, int indent) {
        for (Entry<ITerm, ITerm> entry : data) {
            String label = label(entry.getKey());
            String targetName = escape(trim(entry.getValue().toString()));
            
            //"{scope-name}" -> "{name}" [label="{lbl}", arrowhead="box"];
            indent(indent).append(quote(scopeName)).append(" -> ").append(quote(targetName)).append(" [label=").append(quote(label)).append(", arrowhead=\"box\"];");
            ln();
        }
    }
    
    /**
     * Prints all the edges by calling {@link #printEdges(String, Set, int)}.
     * 
     * @param indent
     *      the indent
     */
    public void printDataEdges(int indent) {
        for (Scope base : data.keySet()) {
            printDataEdges(name(base), data.get(base), indent);
        }
    }
    
    /**
     * 
     * <pre>
     * "{name}" [shape="box"];
     * "{scope-name}" -> "{name}" [label="{lbl}", arrowhead="box"];
     * </pre>
     * @param scopeName
     * @param data
     * @param indent
     */
    @Deprecated
    public void printDecls(String scopeName, Set<? extends Entry<ITerm, ITerm>> data, int indent) {
        for (Entry<ITerm, ITerm> entry : data) {
            String label = label(entry.getKey());
            String name = escape(trim(entry.getValue().toString()));
            
            //"{name}" [shape="box"];
            indent(indent).append(quote(name)).append(" [shape=\"box\"];");
            ln();
            
            //"{scope-name}" -> "{name}" [label="{lbl}", arrowhead="box"];
            indent(indent).append(quote(scopeName)).append(" -> ").append(quote(name)).append(" [label=").append(quote(label)).append(", arrowhead=\"box\"];");
            ln();
        }
    }
    
    /**
     * <pre>
     * "{name}" [shape="box"];
     * </pre>
     * 
     * @param declarations
     *      the declarations to print
     * @param indent
     *      the indent
     */
    public void printDeclarations(Set<ITerm> declarations, int indent) {
        for (ITerm declaration : declarations) {
            String name = escape(trim(declaration.toString()));
            
            //"{name}" [shape="box"];
            indent(indent).append(quote(name)).append(" [shape=\"box\"];");
            ln();
        }
    }
    
    /**
     * Prints the module hierarchy.
     * 
     * @param module
     *      the module to print
     * @param indent
     *      the indent
     */
    public void printModuleHierarchy(IModule module, int indent) {
        startModule(module, indent);
        for (IModule child : module.getChildren()) {
            printModuleHierarchy(child, indent + 2);
        }
        endModule(indent);
    }
    
    /**
     * Starts a module. A module is represented with a cluster.
     * 
     * @param module
     *      the module
     * @param indent
     *      the indent
     */
    public void startModule(IModule module, int indent) {
//        List<String> items = new ArrayList<>();
//        module.getScopeGraph().getScopes().stream().map(DotPrinter::name).map(DotPrinter::quote).forEach(items::add);
//        module.getScopeGraph().getOwnData().valueSet().stream().map(i -> quote(escape(trim(i.toString())))).forEach(items::add);
////        module.getChildren().stream().map(IModule::getName).map(DotPrinter::quote).forEach(items::add);
        
        startCluster(module.getId(), module.getName(), indent);
        printScopes(module.getScopeGraph().getScopes(), indent + 2);
        printDeclarations(module.getScopeGraph().getOwnData().valueSet(), indent + 2);
        
        //{ rank=same {clusters} }
//        indent(indent + 2).append("{ rank=same");
//        for (IModule child : module.getChildren()) {
//            String clusterName = "cluster_" + child.getId().replaceAll("[^\\w]", "_");
//            sb.append(' ').append(clusterName);
//        }
//        appendLn("}");
    }
    
    public void endModule(int indent) {
        endCluster(indent);
    }
    
    /**
     * <pre>
     * subgraph cluster_{uniqueId} {
     *   label = "{name}";
     *   
     *   [scopes]
     *   [declarations]
     *   
//     *   { rank=same {items} }
     * }
     * </pre>
     * 
     * @param uniqueId
     *      the unique id of the cluster
     * @param name
     *      the name of the cluster
     * @param indent
     *      the indentation
     */
    public void startCluster(String uniqueId, String name, int indent) {
        indent(indent).append("subgraph cluster_").append(uniqueId.replaceAll("[^\\w]", "_")).append(" {");
        ln();
        
        indent(indent + 2).append("label = \"").append(escape(name)).append("\";");
        ln();
        
//      indent(sb, indent + 2).append("{ rank=same");
//      for (String item : items) {
//          sb.append(' ').append(item);
//      }
//      appendLn(sb, "}");
    }
    
    public void endCluster(int indent) {
        indent(indent).append('}');
        ln();
    }
    
    /**
     * @param scope
     *      the scope
     * @return
     *      the name of the scope
     */
    private static final String name(Scope scope) {
        //return escape(scope.getResource() + "-" + scope.getName())
        return escape(scope.getName());
    }
    
    /**
     * @param label
     *      the label
     * @return
     *      the string label represented by the given term
     */
    private static final String label(ITerm label) {
        return escape(trim(label.toString()));
    }
    
    protected StringBuilder appendLn(String string) {
        return sb.append(string).append('\n');
    }
    
    protected final StringBuilder ln() {
        return sb.append('\n');
    }
    
    private static final String trim(String str) {
        if (str.startsWith("\"") && str.endsWith("\"")) {
            return str.substring(1, str.length() - 1);
        }
        return str;
    }
    
    private static final String quote(String str) {
        return "\"" + str + "\"";
    }
    
    private static final String escape(String str) {
        return str.replace("\"", "\\\"");
    }
    
    protected final StringBuilder indent(int amount) {
        for (int i = 0; i < amount; i++) sb.append(' ');
        return sb;
    }
}
