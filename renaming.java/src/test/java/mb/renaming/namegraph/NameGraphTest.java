package mb.renaming.namegraph;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;

import mb.renaming.namegraph.NameGraph;
import mb.renaming.namegraph.NameIndex;
import mb.renaming.namegraph.ResolutionPair;


public class NameGraphTest {

	@Test
	public void testOneCluster() {
		NameGraph graph = new NameGraph();
		
		NameIndex ref1 = new NameIndex(2, "foo");
		NameIndex dec1 = new NameIndex(1, "foo");
		ResolutionPair pair1 = new ResolutionPair(ref1, dec1);
		
		NameIndex ref2 = new NameIndex(2, "foo");
		NameIndex dec2 = new NameIndex(3, "foo");
		ResolutionPair pair2 = new ResolutionPair(ref2, dec2);
		
		graph.addResolutionPair(pair1);
		graph.addResolutionPair(pair2);
		
		Set<NameIndex> cluster = graph.find(ref1).get();
		assertTrue(cluster.contains(ref1));
		assertTrue(cluster.contains(ref2));
		assertTrue(cluster.contains(dec1));
		assertTrue(cluster.contains(dec2));
	}
	
	@Test
	public void testMultipeCluster() {
		NameIndex[] nodes = createNodes(9, "foo");
		ResolutionPair[] edges = new ResolutionPair[6];
		
		//Build Cluster 1 ( n - 1)
		edges[0] = new ResolutionPair(nodes[1], nodes[0]);
		edges[1] = new ResolutionPair(nodes[2], nodes[0]);
		
		//Build Cluster 2 ( 1- n)
		edges[2] = new ResolutionPair(nodes[3], nodes[4]);
		edges[3] = new ResolutionPair(nodes[3], nodes[5]);
		
		//Build Cluster 3 (chain)
		edges[4] = new ResolutionPair(nodes[6], nodes[7]);
		edges[5] = new ResolutionPair(nodes[7], nodes[8]);
		
		//Build Graph
		NameGraph graph = new NameGraph();
		for(int i = 0; i < edges.length; i++) {
			graph.addResolutionPair(edges[i]);
		}
		
		Set<NameIndex> testCluster;
		
		//Test cluster 1
		Set<NameIndex> cluster1 = graph.find(nodes[0]).get();
		assertTrue(cluster1.contains(nodes[0]));
		assertTrue(cluster1.contains(nodes[1]));
		assertTrue(cluster1.contains(nodes[2]));
		
		testCluster = graph.find(nodes[1]).get();
		assertEquals(cluster1, testCluster);
		testCluster = graph.find(nodes[2]).get();
		assertEquals(cluster1, testCluster);
		
		//Test cluster 2
		Set<NameIndex> cluster2 = graph.find(nodes[3]).get();
		assertTrue(cluster2.contains(nodes[3]));
		assertTrue(cluster2.contains(nodes[4]));
		assertTrue(cluster2.contains(nodes[5]));
		
		testCluster = graph.find(nodes[4]).get();
		assertEquals(cluster2, testCluster);
		testCluster = graph.find(nodes[5]).get();
		assertEquals(cluster2, testCluster);
		
		//Test cluster 3
		Set<NameIndex> cluster3 = graph.find(nodes[6]).get();
		assertTrue(cluster3.contains(nodes[6]));
		assertTrue(cluster3.contains(nodes[7]));
		assertTrue(cluster3.contains(nodes[8]));
		
		testCluster = graph.find(nodes[7]).get();
		assertEquals(cluster3, testCluster);
		testCluster = graph.find(nodes[8]).get();
		assertEquals(cluster3, testCluster);
		
	}
	
	@Test
	public void testMultipeClusterComplex() {
		NameIndex[] nodes = createNodes(12, "foo");
		ResolutionPair[] edges = new ResolutionPair[10];
		
		//Build Cluster 1 ( n - 1)
		edges[0] = new ResolutionPair(nodes[1], nodes[0]);
		edges[1] = new ResolutionPair(nodes[2], nodes[0]);
		
		//Build Cluster 2 ( 1- n)
		edges[2] = new ResolutionPair(nodes[3], nodes[4]);
		edges[3] = new ResolutionPair(nodes[3], nodes[5]);
		edges[4] = new ResolutionPair(nodes[6], nodes[4]);
		edges[5] = new ResolutionPair(nodes[6], nodes[5]);
		
		//Build Cluster 3 (chain)
		edges[6] = new ResolutionPair(nodes[7], nodes[9]);
		edges[7] = new ResolutionPair(nodes[8], nodes[9]);
		edges[8] = new ResolutionPair(nodes[9], nodes[10]);
		edges[9] = new ResolutionPair(nodes[11], nodes[10]);

		
		//Build Graph
		NameGraph graph = new NameGraph();
		for(int i = 0; i < edges.length; i++) {
			graph.addResolutionPair(edges[i]);
		}
		
		Set<NameIndex> testCluster;
		
		//Test cluster 1
		Set<NameIndex> cluster1 = graph.find(nodes[0]).get();
		assertTrue(cluster1.contains(nodes[0]));
		assertTrue(cluster1.contains(nodes[1]));
		assertTrue(cluster1.contains(nodes[2]));
		
		testCluster = graph.find(nodes[1]).get();
		assertEquals(cluster1, testCluster);
		testCluster = graph.find(nodes[2]).get();
		assertEquals(cluster1, testCluster);
		
		//Test cluster 2
		Set<NameIndex> cluster2 = graph.find(nodes[3]).get();
		assertTrue(cluster2.contains(nodes[3]));
		assertTrue(cluster2.contains(nodes[4]));
		assertTrue(cluster2.contains(nodes[5]));
		assertTrue(cluster2.contains(nodes[6]));
		
		testCluster = graph.find(nodes[4]).get();
		assertEquals(cluster2, testCluster);
		testCluster = graph.find(nodes[5]).get();
		assertEquals(cluster2, testCluster);
		testCluster = graph.find(nodes[6]).get();
		assertEquals(cluster2, testCluster);
		
		//Test cluster 3
		Set<NameIndex> cluster3 = graph.find(nodes[7]).get();
		assertTrue(cluster3.contains(nodes[7]));
		assertTrue(cluster3.contains(nodes[8]));
		assertTrue(cluster3.contains(nodes[9]));
		assertTrue(cluster3.contains(nodes[10]));
		assertTrue(cluster3.contains(nodes[11]));
		
		testCluster = graph.find(nodes[7]).get();
		assertEquals(cluster3, testCluster);
		testCluster = graph.find(nodes[8]).get();
		assertEquals(cluster3, testCluster);
		testCluster = graph.find(nodes[9]).get();
		assertEquals(cluster3, testCluster);
		testCluster = graph.find(nodes[10]).get();
		assertEquals(cluster3, testCluster);
		testCluster = graph.find(nodes[11]).get();
		assertEquals(cluster3, testCluster);
		
	}
	
	private NameIndex[] createNodes(int number, String path) {
		NameIndex[] nodes = new NameIndex[number];
		for(int i = 0; i < number; i++) {
			nodes[i] = new NameIndex(i, path);
		}
		return nodes;
	}
	
	@Test
	public void testUnion() {
		ResolutionPair pair1 = new ResolutionPair("foo", 2, 1);
		ResolutionPair pair2 = new ResolutionPair("foo", 3, 4);
		ResolutionPair pair3 = new ResolutionPair("foo", 4, 1);
		
		NameGraph graph = new NameGraph();
		graph.addResolutionPair(pair1);
		graph.addResolutionPair(pair2);
		graph.addResolutionPair(pair3);

		Set<NameIndex> cluster = graph.find(pair1.getReference()).get();
		assertTrue(cluster.contains(pair1.getReference()));
		assertTrue(cluster.contains(pair1.getDeclaration()));
		assertTrue(cluster.contains(pair2.getReference()));
		assertTrue(cluster.contains(pair2.getReference()));

	}
	
	@Test
	public void testSelfReferencePairs() {
		ResolutionPair pair1 = new ResolutionPair("demo/example.mjv", 30, 40);
		ResolutionPair pair2 = new ResolutionPair("demo/example.mjv", 23, 23);
		ResolutionPair pair3 = new ResolutionPair("demo/example.mjv", 31, 12);
		ResolutionPair pair4 = new ResolutionPair("demo/example.mjv", 12, 12);
		
		NameGraph graph = new NameGraph();
		graph.addResolutionPair(pair1);
		graph.addResolutionPair(pair2);
		graph.addResolutionPair(pair3);
		graph.addResolutionPair(pair4);

		Set<NameIndex> cluster = graph.find(pair3.getReference()).get();
		assertTrue(cluster.contains(pair3.getReference()));
		assertTrue(cluster.contains(pair3.getDeclaration()));
		assertTrue(cluster.contains(pair4.getReference()));
		assertTrue(cluster.contains(pair4.getReference()));

	}


}
