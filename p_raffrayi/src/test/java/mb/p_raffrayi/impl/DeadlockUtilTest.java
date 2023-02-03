package mb.p_raffrayi.impl;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;

import org.junit.Test;

import com.google.common.collect.Sets;

import mb.p_raffrayi.impl.DeadlockUtils.GraphBuilder;
import mb.p_raffrayi.impl.DeadlockUtils.IGraph;

public class DeadlockUtilTest {

    // SCC tests

    @Test public void SCC_singletonGraph() {
        // @formatter:off
        final IGraph<Integer> graph = GraphBuilder.<Integer>of()
            .addVertex(1)
            .build();
        // @formatter:on

        final HashSet<Integer> scc1 = Sets.newHashSet(1);
        final HashSet<HashSet<Integer>> clusters = new HashSet<HashSet<Integer>>();
        clusters.add(scc1);

        assertEquals(clusters, DeadlockUtils.sccs(graph));
    }

    @Test public void SCC_incomingEdge() {
        // @formatter:off
        final IGraph<Integer> graph = GraphBuilder.<Integer>of()
            .addEdge(2, 1)
            .build();
        // @formatter:on

        final HashSet<Integer> scc1 = Sets.newHashSet(1);
        final HashSet<HashSet<Integer>> clusters = new HashSet<HashSet<Integer>>();
        clusters.add(scc1);

        assertEquals(clusters, DeadlockUtils.sccs(graph));
    }

    @Test public void SCC_disconnected() {
        // @formatter:off
        final IGraph<Integer> graph = GraphBuilder.<Integer>of()
            .addVertex(1)
            .addVertex(2)
            .build();
        // @formatter:on

        final HashSet<Integer> scc1 = Sets.newHashSet(1);
        final HashSet<Integer> scc2 = Sets.newHashSet(2);
        @SuppressWarnings("unchecked") final HashSet<HashSet<Integer>> clusters = Sets.newHashSet(scc1, scc2);

        assertEquals(clusters, DeadlockUtils.sccs(graph));
    }

    @Test public void SCC_cluster() {
        // @formatter:off
        final IGraph<Integer> graph = GraphBuilder.<Integer>of()
            .addEdge(1, 2)
            .addEdge(2, 1)
            .build();
        // @formatter:on

        final HashSet<Integer> scc1 = Sets.newHashSet(1, 2);
        final HashSet<HashSet<Integer>> clusters = new HashSet<HashSet<Integer>>();
        clusters.add(scc1);

        assertEquals(clusters, DeadlockUtils.sccs(graph));
    }

    @Test public void SCC_clusterToCluster() {
        // @formatter:off
        final IGraph<Integer> graph = GraphBuilder.<Integer>of()
            .addEdge(1, 2)
            .addEdge(2, 1)
            .addEdge(3, 4)
            .addEdge(4, 3)
            .addEdge(3, 2)
            .build();
        // @formatter:on

        final HashSet<Integer> scc1 = Sets.newHashSet(1, 2);
        final HashSet<HashSet<Integer>> clusters = new HashSet<HashSet<Integer>>();
        clusters.add(scc1);

        assertEquals(clusters, DeadlockUtils.sccs(graph));
    }

    @Test public void SCC_cycle() {
        // @formatter:off
        final IGraph<Integer> graph = GraphBuilder.<Integer>of()
            .addEdge(1, 2)
            .addEdge(2, 3)
            .addEdge(3, 4)
            .addEdge(4, 5)
            .addEdge(5, 1)
            .addEdge(1, 4)
            .build();
        // @formatter:on

        final HashSet<Integer> scc1 = Sets.newHashSet(1, 2, 3, 4, 5);
        final HashSet<HashSet<Integer>> clusters = new HashSet<HashSet<Integer>>();
        clusters.add(scc1);

        assertEquals(clusters, DeadlockUtils.sccs(graph));
    }

    @Test public void SCC_twoClustersWithIncoming() {
        // @formatter:off
        final IGraph<Integer> graph = GraphBuilder.<Integer>of()
            .addEdge(1, 2)
            .addEdge(2, 3)
            .addEdge(3, 1)
            .addEdge(4, 5)
            .addEdge(5, 6)
            .addEdge(6, 4)
            .addEdge(7, 2)
            .addEdge(7, 4)
            .addVertex(8)
            .build();
        // @formatter:on

        final HashSet<Integer> scc1 = Sets.newHashSet(1, 2, 3);
        final HashSet<Integer> scc2 = Sets.newHashSet(4, 5, 6);
        final HashSet<Integer> scc3 = Sets.newHashSet(8);
        @SuppressWarnings("unchecked") final HashSet<HashSet<Integer>> clusters = Sets.newHashSet(scc1, scc2, scc3);
        clusters.add(scc1);

        assertEquals(clusters, DeadlockUtils.sccs(graph));
    }

}
