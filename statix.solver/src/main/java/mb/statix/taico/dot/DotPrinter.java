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
import mb.statix.taico.scopegraph.IMInternalScopeGraph;
import mb.statix.taico.solver.MSolverResult;
import mb.statix.taico.solver.SolverContext;

public class DotPrinter {
    protected final IMInternalScopeGraph<Scope, ITerm, ITerm> rootGraph;
    protected final Set<IModule> modules;
    protected IRelation3<Scope, ITerm, Scope> edges;
    protected IRelation3<Scope, ITerm, ITerm> data;
    protected List<Scope> scopes;
    
    protected StringBuilder sb;
    protected String result;
    protected String resultNonModular;
    protected boolean includeChildren = true;
    
    /**
     * DotPrinter for the given solver result.
     * 
     * @param initial
     *      the solver result
     * @param file
     *      the file to start the graph at, or null to start at the root
     */
    public DotPrinter(MSolverResult initial, String file) {
        if (file == null) {
            //Use the root module and print all modules
            this.rootGraph = initial.state().getOwner().getScopeGraph();
            this.modules = initial.context().getModules();
        } else {
            IModule root = findFileModule(initial.context(), file);
            this.rootGraph = root.getScopeGraph();
            this.modules = root.getDescendantsIncludingSelf().collect(Collectors.toSet());
        }
        
        determineEdgesAndData(modules);
    }
    
    /**
     * A dot printer that prints the given file as root.
     * If the given file is null, the root module is used instead.
     * 
     * This method uses the globally accessible context.
     * 
     * @param file
     *      the file to start the graph at, or null to indicate the root module
     */
    public DotPrinter(String file) {
        if (file == null) {
            //Use the root module and print all modules
            this.rootGraph = SolverContext.context().getRootModule().getScopeGraph();
            this.modules = SolverContext.context().getModules();
        } else {
            IModule root = findFileModule(SolverContext.context(), file);
            this.rootGraph = root.getScopeGraph();
            this.modules = root.getDescendantsIncludingSelf().collect(Collectors.toSet());
        }
        
        determineEdgesAndData(modules);
    }
    
    /**
     * Creates a dot printer for the given scope graph (+ children).
     * 
     * @param graph
     *      the graph to print
     * @param includeChildren
     *      if child scope graphs should be included
     */
    public DotPrinter(IMInternalScopeGraph<Scope, ITerm, ITerm> graph, boolean includeChildren) {
        this.rootGraph = graph;
        this.modules = graph.getOwner().getDescendantsIncludingSelf().collect(Collectors.toSet());
        this.includeChildren = includeChildren;
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
    protected IModule findFileModule(SolverContext context, String name) {
        IModule module = context.getModulesOnLevel(1).get(name);
        if (module == null) throw new NullPointerException("Module " + name + " not found (in " + context.getModulesOnLevel(1) + ")");
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
        printModuleHierarchy(rootGraph, 2);
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
        printModuleHierarchy(rootGraph, 2);
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
     * Prints the module hierarchy starting at the given scope graph.
     * 
     * @param graph
     *      the scope graph of the module to print the hierarchy for
     * @param indent
     *      the indent
     */
    public void printModuleHierarchy(IMInternalScopeGraph<Scope, ITerm, ITerm> graph, int indent) {
        startModule(graph, indent);
        if (includeChildren) {
            for (IMInternalScopeGraph<Scope, ITerm, ITerm> child : graph.getChildren()) {
                printModuleHierarchy(child, indent + 2);
            }
        }
        endModule(indent);
    }
    
    /**
     * Starts a module. A module is represented with a cluster.
     * 
     * @param graph
     *      the scope graph of the module
     * @param indent
     *      the indent
     */
    public void startModule(IMInternalScopeGraph<Scope, ITerm, ITerm> graph, int indent) {
//        List<String> items = new ArrayList<>();
//        module.getScopeGraph().getScopes().stream().map(DotPrinter::name).map(DotPrinter::quote).forEach(items::add);
//        module.getScopeGraph().getOwnData().valueSet().stream().map(i -> quote(escape(trim(i.toString())))).forEach(items::add);
////        module.getChildren().stream().map(IModule::getName).map(DotPrinter::quote).forEach(items::add);
        
        startCluster(graph.getOwner().getId(), graph.getOwner().getName(), indent);
        printScopes(graph.getScopes(), indent + 2);
        printDeclarations(graph.getOwnData().valueSet(), indent + 2);
        
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
