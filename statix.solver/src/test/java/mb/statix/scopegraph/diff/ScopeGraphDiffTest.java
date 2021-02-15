package mb.statix.scopegraph.diff;

import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;
import org.metaborg.util.functions.Predicate2;

import mb.nabl2.util.CapsuleUtil;
import mb.statix.scopegraph.IScopeGraph;
import mb.statix.scopegraph.reference.ScopeGraph;

public class ScopeGraphDiffTest {
	
	private static final Integer s0 = 0;
	private static final Integer s1 = 1;
	private static final Integer s2 = 2;
	
	private static final String l1 = "L";
	
	private static final IScopeGraph.Immutable<Integer, String, String> EMPTY_SG = ScopeGraph.Immutable.of();
	
	private IScopeGraph.Immutable<Integer, String, String> sc1 = EMPTY_SG;
	private IScopeGraph.Immutable<Integer, String, String> sc2 = EMPTY_SG;
	
	private ScopeGraphDiff<Integer, String, String> diff;
	
	@Test public void testEqual() {
		sc1 = EMPTY_SG.addEdge(s0, l1, s1);
		sc2 = EMPTY_SG.addEdge(s0, l1, s1);
		
		diff = diff(sc1, sc2);

		assertTrue(diff.matchedScopes().containsEntry(s0, s0));
		assertTrue(diff.matchedScopes().containsEntry(s1, s1));
		assertTrue(diff.matchedEdges().containsEntry(edge(s0, l1, s1), edge(s0, l1, s1)));
	} 
	
	@Test public void testEquivDifferentNames() {
		sc1 = EMPTY_SG.addEdge(s0, l1, s1);
		sc2 = EMPTY_SG.addEdge(s0, l1, s2);
		
		diff = diff(sc1, sc2);

		assertTrue(diff.matchedScopes().containsEntry(s0, s0));
		assertTrue(diff.matchedScopes().containsEntry(s1, s2));
		assertTrue(diff.matchedEdges().containsEntry(edge(s0, l1, s1), edge(s0, l1, s2)));
	}
	
	private Edge<Integer, String> edge(Integer src, String lbl, Integer tgt) {
		return new Edge<>(src, lbl, tgt);
	}

	private static ScopeGraphDiff<Integer, String, String> diff(
			IScopeGraph.Immutable<Integer, String, String> sc1, 
			IScopeGraph.Immutable<Integer, String, String> sc2
	) {
		return ScopeGraphDiffer.diff(s0, sc1, sc2, SimpleScopeGraphDifferOpts.INSTANCE);
	}
	
	
	private static class SimpleScopeGraphDifferOpts implements ScopeGraphDifferOps<Integer, String> {
		
		public static SimpleScopeGraphDifferOpts INSTANCE = new SimpleScopeGraphDifferOpts();
		
		private static final Set<Integer> NO_SCOPES = CapsuleUtil.immutableSet();

		@Override
		public boolean isMatchAllowed(Integer current, Integer previous) {
			return true;
		}

		@Override
		public Set<Integer> getCurrentScopes(String datum) {
			return NO_SCOPES;
		}

		@Override
		public Set<Integer> getPreviousScopes(String datum) {
			return NO_SCOPES;
		}

		@Override
		public boolean matchDatums(String current, String previous, Predicate2<Integer, Integer> matchScopes) {
			return current.equals(previous);
		}
	}
}
