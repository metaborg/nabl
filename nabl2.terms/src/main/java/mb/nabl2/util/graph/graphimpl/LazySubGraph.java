package mb.nabl2.util.graph.graphimpl;

import com.google.common.collect.Maps;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import mb.nabl2.util.CapsuleUtil;
import mb.nabl2.util.graph.alg.misc.memory.EmptyMemory;
import mb.nabl2.util.graph.alg.misc.memory.IMemoryView;
import mb.nabl2.util.graph.alg.misc.memory.MapBackedMemoryView;
import mb.nabl2.util.graph.igraph.IBiDirectionalGraphDataSource;
import mb.nabl2.util.graph.igraph.IGraphObserver;

public class LazySubGraph<V> implements IBiDirectionalGraphDataSource<V> {

    private final IBiDirectionalGraphDataSource<V> graph;
    private final Set.Immutable<V> nodesInSubGraph;
    private final java.util.Map<V, IMemoryView<V>> targetNodes;
    private final java.util.Map<V, IMemoryView<V>> sourceNodes;

    public LazySubGraph(IBiDirectionalGraphDataSource<V> graph, Iterable<V> nodesInSubGraph) {
        this.graph = graph;
        this.nodesInSubGraph = CapsuleUtil.toSet(nodesInSubGraph);
        this.targetNodes = Maps.newHashMap();
        this.sourceNodes = Maps.newHashMap();
    }

    @Override public void attachObserver(IGraphObserver<V> observer) {
        throw new UnsupportedOperationException();
    }

    @Override public void attachAsFirstObserver(IGraphObserver<V> observer) {
        throw new UnsupportedOperationException();
    }

    @Override public void detachObserver(IGraphObserver<V> observer) {
        throw new UnsupportedOperationException();
    }

    @Override public java.util.Set<V> getAllNodes() {
        return nodesInSubGraph;
    }

    @Override public IMemoryView<V> getTargetNodes(V source) {
        if(!nodesInSubGraph.contains(source)) {
            return EmptyMemory.instance();
        }
        return targetNodes.computeIfAbsent(source, src -> {
            final Map.Transient<V, Integer> targetNodes = graph.getTargetNodes(src).asMap().asTransient();
            CapsuleUtil.filter(targetNodes, node -> nodesInSubGraph.contains(node));
            return new MapBackedMemoryView<>(targetNodes.freeze());
        });
    }

    @Override public IMemoryView<V> getSourceNodes(V target) {
        if(!nodesInSubGraph.contains(target)) {
            return EmptyMemory.instance();
        }
        return sourceNodes.computeIfAbsent(target, tgt -> {
            final Map.Transient<V, Integer> sourceNodes = graph.getSourceNodes(tgt).asMap().asTransient();
            CapsuleUtil.filter(sourceNodes, node -> nodesInSubGraph.contains(node));
            return new MapBackedMemoryView<>(sourceNodes.freeze());
        });
    }

}