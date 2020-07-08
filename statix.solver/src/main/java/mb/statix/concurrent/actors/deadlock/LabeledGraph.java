package mb.statix.concurrent.actors.deadlock;

import java.util.Set;

import org.eclipse.viatra.query.runtime.base.itc.graphimpl.Graph;
import org.eclipse.viatra.query.runtime.base.itc.igraph.IBiDirectionalGraphDataSource;
import org.eclipse.viatra.query.runtime.base.itc.igraph.IGraphDataSource;
import org.eclipse.viatra.query.runtime.base.itc.igraph.IGraphObserver;
import org.eclipse.viatra.query.runtime.matchers.util.IMemoryView;

import io.usethesource.capsule.SetMultimap;
import mb.nabl2.util.Tuple2;
import mb.nabl2.util.collections.MultiSetMap;

public class LabeledGraph<V, L> implements IGraphDataSource<V>, IBiDirectionalGraphDataSource<V> {

    private final Graph<V> graph;
    private final MultiSetMap.Transient<Tuple2<V, V>, L> edges;

    public LabeledGraph() {
        this.graph = new Graph<>();
        this.edges = MultiSetMap.Transient.of();
    }

    public SetMultimap.Immutable<V, L> getOutgoingEdges(V source) {
        if(!graph.getAllNodes().contains(source)) {
            return SetMultimap.Immutable.of();
        }
        final SetMultimap.Transient<V, L> outgoingEdges = SetMultimap.Transient.of();
        for(V target : graph.getTargetNodes(source)) {
            for(L label : edges.get(Tuple2.of(source, target))) {
                outgoingEdges.__insert(target, label);
            }
        }
        return outgoingEdges.freeze();
    }

    public SetMultimap.Immutable<V, L> getIncomingEdges(V target) {
        if(!graph.getAllNodes().contains(target)) {
            return SetMultimap.Immutable.of();
        }
        final SetMultimap.Transient<V, L> incomingEdges = SetMultimap.Transient.of();
        for(V source : graph.getSourceNodes(target)) {
            for(L label : edges.get(Tuple2.of(source, target))) {
                incomingEdges.__insert(source, label);
            }
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

}
