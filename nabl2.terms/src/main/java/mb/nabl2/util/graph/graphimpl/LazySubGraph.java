package mb.nabl2.util.graph.graphimpl;

import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.Maps;
import com.google.common.collect.Streams;

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
    private final Map<V, IMemoryView<V>> targetNodes;
    private final Map<V, IMemoryView<V>> sourceNodes;

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
            return new MapBackedMemoryView<>(Streams.stream(graph.getTargetNodes(src).entriesWithMultiplicities())
                    .filter(e -> nodesInSubGraph.contains(e.getKey()))
                    .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue())));
        });
    }

    @Override public IMemoryView<V> getSourceNodes(V target) {
        if(!nodesInSubGraph.contains(target)) {
            return EmptyMemory.instance();
        }
        return sourceNodes.computeIfAbsent(target, src -> {
            return new MapBackedMemoryView<>(Streams.stream(graph.getSourceNodes(src).entriesWithMultiplicities())
                    .filter(e -> nodesInSubGraph.contains(e.getKey()))
                    .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue())));
        });
    }

}