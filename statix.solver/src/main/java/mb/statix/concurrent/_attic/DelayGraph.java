package mb.statix.concurrent._attic;

import java.util.Collections;
import java.util.Optional;

import org.eclipse.viatra.query.runtime.base.itc.alg.incscc.IncSCCAlg;
import org.eclipse.viatra.query.runtime.base.itc.graphimpl.Graph;

import com.google.common.collect.Sets;

import io.usethesource.capsule.SetMultimap;
import mb.nabl2.util.Tuple2;
import mb.nabl2.util.collections.HashTrieRelation2;
import mb.nabl2.util.collections.IRelation2;
import mb.statix.concurrent._attic.messages.Query;
import mb.statix.scopegraph.reference.EdgeOrData;

public class DelayGraph<S, L, D> {

    private final ScopeImpl<S> scopeImpl;

    private final SetMultimap.Transient<String, Query<S, L, D>> unitDelays = SetMultimap.Transient.of();
    private final Graph<String> delayGraph = new Graph<>();
    private final IncSCCAlg<String> sccGraph = new IncSCCAlg<>(delayGraph);
    private final IRelation2.Transient<Tuple2<S, EdgeOrData<L>>, Query<S, L, D>> delays =
            HashTrieRelation2.Transient.of();

    public DelayGraph(ScopeImpl<S> scopeImpl) {
        this.scopeImpl = scopeImpl;
    }

    public void addUnits(Iterable<String> nodes) {
        for(String node : nodes) {
            delayGraph.insertNode(node);
        }
    }

    /**
     * Add query delayed on given edge.
     */
    public void addDelayOnEdge(Query<S, L, D> query, S scope, EdgeOrData<L> edge) {
        final String source = query.client().resource();
        final String target = scopeImpl.resource(scope);
        if(delays.put(Tuple2.of(scope, edge), query)) {
            unitDelays.__insert(source, query);
            delayGraph.insertEdge(source, target);
        }
    }

    /**
     * Remove edge, and return any queries that are not delayed on any edge anymore.
     */
    public java.util.Set<Query<S, L, D>> removeEdge(S scope, EdgeOrData<L> edge) {
        final java.util.Set<Query<S, L, D>> removed = Sets.newHashSet();
        for(Query<S, L, D> query : delays.removeKey(Tuple2.of(scope, edge))) {
            final String source = query.client().resource();
            final String target = scopeImpl.resource(scope);
            unitDelays.__remove(source, query);
            delayGraph.deleteEdgeThatExists(source, target);
            if(!delays.containsValue(query)) {
                removed.add(query);
            }
        }
        return removed;
    }

    /**
     * Return all delayed queries of the given unit.
     */
    public java.util.Set<Query<S, L, D>> getDelaysOfUnit(String source) {
        return unitDelays.get(source);
    }

    /**
     * Remove the given unit, and return all delayed queries of the given unit.
     */
    public java.util.Set<Query<S, L, D>> removeUnit(String source) {
        final java.util.Set<Query<S, L, D>> removed = unitDelays.get(source);
        unitDelays.__remove(source);
        for(Query<S, L, D> query : removed) {
            for(Tuple2<S, EdgeOrData<L>> scopeEdge : delays.removeValue(query)) {
                final String target = scopeImpl.resource(scopeEdge._1());
                delayGraph.deleteEdgeThatExists(source, target);
            }
        }
        return removed;
    }

    public boolean inPeninsula(String resource) {
        final String representative = sccGraph.getRepresentative(resource);
        return !sccGraph.hasOutgoingEdges(representative);
    }

    public java.util.Set<String> getComponent(String resource) {
        return Optional.ofNullable(sccGraph.sccs.getPartition(resource)).orElse(Collections.singleton(resource));
    }

}