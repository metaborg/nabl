package mb.statix.concurrent.actors.deadlock;

import java.util.Set;

import mb.nabl2.util.Tuple2;
import mb.nabl2.util.collections.MultiSetMap;
import mb.nabl2.util.graph.alg.misc.memory.IMemoryView;
import mb.nabl2.util.graph.graphimpl.Graph;
import mb.nabl2.util.graph.igraph.IBiDirectionalGraphDataSource;
import mb.nabl2.util.graph.igraph.IGraphDataSource;
import mb.nabl2.util.graph.igraph.IGraphObserver;

public class LabeledGraph<V, L> implements IGraphDataSource<V>, IBiDirectionalGraphDataSource<V> {

    private final Graph<V> graph;
    private final MultiSetMap.Transient<Tuple2<V, V>, L> edges;

    public LabeledGraph() {
        this.graph = new Graph<>();
        this.edges = MultiSetMap.Transient.of();
    }

    public MultiSetMap.Immutable<V, L> getOutgoingEdges(V source) {
        if(!graph.getAllNodes().contains(source)) {
            return MultiSetMap.Immutable.of();
        }
        final MultiSetMap.Transient<V, L> outgoingEdges = MultiSetMap.Transient.of();
        for(V target : graph.getTargetNodes(source)) {
            outgoingEdges.putAll(target, edges.get(Tuple2.of(source, target)));
        }
        return outgoingEdges.freeze();
    }

    public MultiSetMap.Immutable<V, L> getIncomingEdges(V target) {
        if(!graph.getAllNodes().contains(target)) {
            return MultiSetMap.Immutable.of();
        }
        final MultiSetMap.Transient<V, L> incomingEdges = MultiSetMap.Transient.of();
        for(V source : graph.getSourceNodes(target)) {
            incomingEdges.putAll(source, edges.get(Tuple2.of(source, target)));
        }
        return incomingEdges.freeze();
    }

    public void addEdge(V source, L label, V target) {
        addNodeIfAbsent(source);
        addNodeIfAbsent(target);
        graph.insertEdge(source, target);
        edges.put(Tuple2.of(source, target), label);
    }

    public void removeEdge(V source, L label, V target) {
        graph.deleteEdgeThatExists(source, target);
        edges.remove(Tuple2.of(source, target), label);
        removeNodeIfObsolete(source);
        removeNodeIfObsolete(target);
    }

    private void addNodeIfAbsent(V node) {
        if(!graph.getAllNodes().contains(node)) {
            graph.insertNode(node);
        }
    }

    private void removeNodeIfObsolete(V node) {
        if(graph.getTargetNodes(node).isEmpty() && graph.getSourceNodes(node).isEmpty()) {
            graph.deleteNode(node);
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // IGraphDataSource
    ////////////////////////////////////////////////////////////////////////////

    @Override public void attachObserver(IGraphObserver<V> observer) {
        graph.attachObserver(observer);
    }

    @Override public void attachAsFirstObserver(IGraphObserver<V> observer) {
        graph.attachAsFirstObserver(observer);
    }

    @Override public void detachObserver(IGraphObserver<V> observer) {
        graph.detachObserver(observer);
    }

    @Override public Set<V> getAllNodes() {
        return graph.getAllNodes();
    }

    @Override public IMemoryView<V> getTargetNodes(V source) {
        return graph.getTargetNodes(source);
    }

    @Override public IMemoryView<V> getSourceNodes(V target) {
        return graph.getSourceNodes(target);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Object
    ////////////////////////////////////////////////////////////////////////////

    @Override public String toString() {
        return "Graph[" + edges.toString() + "]";
    }

}
