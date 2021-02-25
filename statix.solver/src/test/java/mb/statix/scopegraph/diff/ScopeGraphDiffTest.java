package mb.statix.scopegraph.diff;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.junit.Test;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import mb.nabl2.terms.ITerm;
import static mb.nabl2.terms.build.TermBuild.B;
import mb.nabl2.terms.unification.u.PersistentUnifier;
import mb.statix.scopegraph.IScopeGraph;
import mb.statix.scopegraph.reference.EdgeOrData;
import mb.statix.scopegraph.reference.ScopeGraph;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.scopegraph.terms.StatixDifferOps;

public class ScopeGraphDiffTest {

    private static final Scope s0 = Scope.of("", "s0");
    private static final Scope s1 = Scope.of("", "s1");
    private static final Scope s2 = Scope.of("", "s2");
    private static final Scope s3 = Scope.of("", "s3");
    private static final Scope s4 = Scope.of("", "s4");

    private static final Scope d1 = Scope.of("", "d1");
    private static final Scope d2 = Scope.of("", "d2");
    private static final Scope d3 = Scope.of("", "d3");
    private static final Scope d4 = Scope.of("", "d4");

    private static final ITerm P = B.newAppl("Label", B.newString("P"));
    private static final ITerm var = B.newAppl("Label", B.newString("var"));

    private IScopeGraph.Immutable<Scope, ITerm, ITerm> current = ScopeGraph.Immutable.of();
    private IScopeGraph.Immutable<Scope, ITerm, ITerm> previous = ScopeGraph.Immutable.of();

    private static final ScopeGraphDifferOps<Scope, ITerm> ops =
        new StatixDifferOps(PersistentUnifier.Immutable.of(), PersistentUnifier.Immutable.of());

    // Full diffs (no open scopes)

    @Test public void testEqual() {
        current = current.addEdge(s0, P, s1);
        previous = previous.addEdge(s0, P, s1);

        ScopeGraphDiff<Scope, ITerm, ITerm> diff = diff(current, previous);

        assertScopeMatches(diff, s0, s0, s1, s1);
        assertEdgeMatches(diff, edge(s0, P, s1), edge(s0, P, s1));
    }

    @Test public void testEquivDifferentNames() {
        current = current.addEdge(s0, P, s1);
        previous = previous.addEdge(s0, P, s2);

        ScopeGraphDiff<Scope, ITerm, ITerm> diff = diff(current, previous);

        assertScopeMatches(diff, s0, s0, s1, s2);
        assertEdgeMatches(diff, edge(s0, P, s1), edge(s0, P, s2));
    }

    @Test public void testTransitiveChange() {
        // @formatter:off
        current = current.addEdge(s0, P, s2)
            .addEdge(s2, P, s1)
            .addEdge(s1, var, d1)
            .setDatum(d1, B.newList(B.newString("x"), B.newAppl("INT")));
        // @formatter:on

        // @formatter:off
        previous = previous.addEdge(s0, P, s4)
            .addEdge(s4, P, s3)
            .addEdge(s3, var, d2)
            .setDatum(d2, B.newList(B.newString("y"), B.newAppl("INT")));
        // @formatter:on

        ScopeGraphDiff<Scope, ITerm, ITerm> diff = ScopeGraphDiffer.fullDiff(s0, s0, previous, current, ops);

        assertScopeMatches(diff, s0, s0, s4, s2, s3, s1);
        assertEdgeMatches(diff, edge(s0, P, s4), edge(s0, P, s2), edge(s4, P, s3), edge(s2, P, s1));

        assertEquals(1, diff.added().scopes().size());
        assertTrue(diff.added().scopes().containsKey(d2));
        assertEquals(B.newList(B.newString("y"), B.newAppl("INT")), diff.added().scopes().get(d2).get());

        assertEquals(1, diff.added().edges().size());
        assertTrue(diff.added().edges().contains(edge(s3, var, d2)));

        assertEquals(1, diff.removed().scopes().size());
        assertTrue(diff.removed().scopes().containsKey(d1));
        assertEquals(B.newList(B.newString("x"), B.newAppl("INT")), diff.removed().scopes().get(d1).get());

        assertEquals(1, diff.removed().edges().size());
        assertTrue(diff.removed().edges().contains(edge(s1, var, d1)));
    }

    @Test public void testScopeInData() {
        // @formatter:off
        current = current.addEdge(s0, P, s1)
            .addEdge(s1, var, d1)
            .setDatum(d1, B.newAppl("D", s3));
        // @formatter:on

        // @formatter:off
        previous = previous.addEdge(s0, P, s2)
            .addEdge(s2, var, d2)
            .setDatum(d2, B.newAppl("D", s4));
        // @formatter:on

        ScopeGraphDiff<Scope, ITerm, ITerm> diff = ScopeGraphDiffer.fullDiff(s0, s0, previous, current, ops);

        assertTrue(diff.matchedScopes().containsEntry(s4, s3));
    }

    // Stepwise close scopes, graph equal

    @Test public void testStepwiseScopeClose_NoChange() {
        // @formatter:off
        current = current.addEdge(s0, P, s1)
            .addEdge(s1, var, d1)
            .setDatum(d1, B.newAppl("D", s3));
        // @formatter:on

        // @formatter:off
        previous = previous.addEdge(s0, P, s2)
            .addEdge(s2, var, d2)
            .setDatum(d2, B.newAppl("D", s4));
        // @formatter:on

        Multimap<Scope, ITerm> allLabels = ArrayListMultimap.create();
        allLabels.put(s0, P);
        allLabels.put(s1, var);

        Multimap<Scope, EdgeOrData<ITerm>> closed = ArrayListMultimap.create();
        closed.put(s0, EdgeOrData.data());
        closed.put(s1, EdgeOrData.data());

        // Init only matches s0
        ScopeGraphDiffer<Scope, ITerm, ITerm> differ =
            ScopeGraphDiffer.of(previous, ops, new CollectionStatusOps<>(allLabels, closed));
        DifferState.Immutable<Scope, ITerm, ITerm> initialState = differ.initDiff(s0, s0);

        assertScopeMatches(initialState, s0, s0);
        assertEdgeMatches(initialState);

        // Diff on unchanged state does not modify state
        DifferState.Immutable<Scope, ITerm, ITerm> state1 = differ.doDiff(current, initialState, closed);

        assertScopeMatches(state1, s0, s0);
        assertEdgeMatches(state1);

        // Closing (s0, P) matches edge s0 -P-> {s1, s2}
        closed.put(s0, EdgeOrData.edge(P));
        DifferState.Immutable<Scope, ITerm, ITerm> state2 = differ.doDiff(current, state1, closed);

        assertScopeMatches(state2, s0, s0, s1, s2);
        assertEdgeMatches(state2, edge(s0, P, s1), edge(s0, P, s2));

        // Closing cannot yet match edge s1 -var-> {D(s3), D(s4)} because (d1, $) is not closed
        closed.put(s1, EdgeOrData.edge(var));
        DifferState.Immutable<Scope, ITerm, ITerm> state3 = differ.doDiff(current, state2, closed);

        assertScopeMatches(state3, s0, s0, s1, s2);
        assertEdgeMatches(state3, edge(s0, P, s1), edge(s0, P, s2));

        // Closing (d1, $) matches data
        closed.put(d1, EdgeOrData.data());
        DifferState.Immutable<Scope, ITerm, ITerm> state4 = differ.doDiff(current, state3, closed);

        assertScopeMatches(state4, s0, s0, s1, s2, d1, d2, s3, s4);
        assertEdgeMatches(state4, edge(s0, P, s1), edge(s0, P, s2), edge(s1, var, d1), edge(s2, var, d2));

        // Finalizing does not change state
        ScopeGraphDiff<Scope, ITerm, ITerm> diff = differ.finalize(current, state4);

        assertScopeMatches(diff, s0, s0, s1, s2, d1, d2, s3, s4);
        assertEdgeMatches(diff, edge(s0, P, s1), edge(s0, P, s2), edge(s1, var, d1), edge(s2, var, d2));
        assertDiffEmpty(diff);
    }

    @Test public void testStepwiseScopeClose_TargetEarlier() {
        // @formatter:off
        current = current.addEdge(s0, P, s1)
            .addEdge(s1, var, d1)
            .setDatum(d1, B.newAppl("D", s3));
        // @formatter:on

        // @formatter:off
        previous = previous.addEdge(s0, P, s2)
            .addEdge(s2, var, d2)
            .setDatum(d2, B.newAppl("D", s4));
        // @formatter:on

        Multimap<Scope, ITerm> allLabels = ArrayListMultimap.create();
        allLabels.put(s0, P);
        allLabels.put(s1, var);

        Multimap<Scope, EdgeOrData<ITerm>> closed = ArrayListMultimap.create();
        closed.put(s1, EdgeOrData.edge(var));
        closed.put(s0, EdgeOrData.data());
        closed.put(s1, EdgeOrData.data());
        closed.put(d1, EdgeOrData.data());

        // Init only matches s0
        ScopeGraphDiffer<Scope, ITerm, ITerm> differ =
            ScopeGraphDiffer.of(previous, ops, new CollectionStatusOps<>(allLabels, closed));

        DifferState.Immutable<Scope, ITerm, ITerm> state1 = differ.doDiff(current, differ.initDiff(s0, s0), closed);

        assertScopeMatches(state1, s0, s0);
        assertEdgeMatches(state1);

        // Closing (s0, P) matches edges s0 -P-> {s1, s2} and s1 -var-> {D(s3), D(s4)}
        closed.put(s0, EdgeOrData.edge(P));
        DifferState.Immutable<Scope, ITerm, ITerm> state2 = differ.doDiff(current, state1, closed);

        assertScopeMatches(state2, s0, s0, s1, s2, d1, d2, s3, s4);
        assertEdgeMatches(state2, edge(s0, P, s1), edge(s0, P, s2), edge(s1, var, d1), edge(s2, var, d2));

        // Finalizing does not change state
        ScopeGraphDiff<Scope, ITerm, ITerm> diff = differ.finalize(current, state2);

        assertScopeMatches(diff, s0, s0, s1, s2, d1, d2, s3, s4);
        assertEdgeMatches(diff, edge(s0, P, s1), edge(s0, P, s2), edge(s1, var, d1), edge(s2, var, d2));
        assertDiffEmpty(diff);
    }

    // Growing current scope graph

    @Test public void testGraphAdditions_TargetEarlier() {
        // @formatter:off
        previous = previous.addEdge(s0, P, s2)
            .addEdge(s2, var, d2)
            .setDatum(d2, B.newAppl("D", s4));
        // @formatter:on

        Multimap<Scope, ITerm> allLabels = ArrayListMultimap.create();
        allLabels.put(s0, P);
        allLabels.put(s1, var);

        Multimap<Scope, EdgeOrData<ITerm>> closed = ArrayListMultimap.create();
        closed.put(s0, EdgeOrData.data());
        closed.put(s1, EdgeOrData.data());
        closed.put(s3, EdgeOrData.data());

        // Init only matches s0
        ScopeGraphDiffer<Scope, ITerm, ITerm> differ =
            ScopeGraphDiffer.of(previous, ops, new CollectionStatusOps<>(allLabels, closed));
        DifferState.Immutable<Scope, ITerm, ITerm> state1 =
            differ.doDiff(current, differ.initDiff(s0, s0), ImmutableMultimap.of(s0, EdgeOrData.data()));

        assertScopeMatches(state1, s0, s0);
        assertEdgeMatches(state1);

        // Adding datum to d1 does not change status
        current = current.setDatum(d1, B.newAppl("D", s3));
        DifferState.Immutable<Scope, ITerm, ITerm> state2 = differ.doDiff(current, state1, ImmutableMultimap.of());

        assertScopeMatches(state2, s0, s0);
        assertEdgeMatches(state2);

        // closing (d1, $) does not change status
        closed.put(d1, EdgeOrData.data());
        DifferState.Immutable<Scope, ITerm, ITerm> state3 =
            differ.doDiff(current, state2, ImmutableMultimap.of(d1, EdgeOrData.data()));

        assertScopeMatches(state3, s0, s0);
        assertEdgeMatches(state3);

        // Adding s1 -var-> d1 does not change status
        current = current.addEdge(s1, var, d1);
        DifferState.Immutable<Scope, ITerm, ITerm> state4 = differ.doDiff(current, state3, ImmutableMultimap.of());

        assertScopeMatches(state4, s0, s0);
        assertEdgeMatches(state4);

        // closing (s1, var) does not change status
        closed.put(s1, EdgeOrData.edge(var));
        DifferState.Immutable<Scope, ITerm, ITerm> state5 =
            differ.doDiff(current, state4, ImmutableMultimap.of(s1, EdgeOrData.edge(var)));

        assertScopeMatches(state5, s0, s0);
        assertEdgeMatches(state5);

        // adding s0 -P-> s1 does not change state
        current = current.addEdge(s0, P, s1);
        DifferState.Immutable<Scope, ITerm, ITerm> state6 = differ.doDiff(current, state5, ImmutableMultimap.of());

        assertScopeMatches(state6, s0, s0);
        assertEdgeMatches(state6);

        // Closing (s0, P) matches edges s0 -P-> {s1, s2} and s1 -var-> {D(s3), D(s4)}
        closed.put(s0, EdgeOrData.edge(P));
        DifferState.Immutable<Scope, ITerm, ITerm> state7 =
            differ.doDiff(current, state6, ImmutableMultimap.of(s0, EdgeOrData.edge(P)));

        assertScopeMatches(state7, s0, s0, s1, s2, d1, d2, s3, s4);
        assertEdgeMatches(state7, edge(s0, P, s1), edge(s0, P, s2), edge(s1, var, d1), edge(s2, var, d2));

        // Finalizing does not change state
        ScopeGraphDiff<Scope, ITerm, ITerm> diff = differ.finalize(current, state7);

        assertScopeMatches(diff, s0, s0, s1, s2, d1, d2, s3, s4);
        assertEdgeMatches(diff, edge(s0, P, s1), edge(s0, P, s2), edge(s1, var, d1), edge(s2, var, d2));
        assertDiffEmpty(diff);
    }

    @Test public void testGraphAdditions_MoreEdgesOfLabel() {
        // @formatter:off
        current = current.addEdge(s0, var, d1)
            .addEdge(s0, var, d2)
            .setDatum(d1, B.newAppl("A"));
        // @formatter:on

        // @formatter:off
        previous = previous.addEdge(s0, var, d3)
            .addEdge(s0, var, d4)
            .setDatum(d3, B.newAppl("A"))
            .setDatum(d4, B.newAppl("B"));
        // @formatter:on

        Multimap<Scope, ITerm> allLabels = ArrayListMultimap.create();
        allLabels.put(s0, var);

        Multimap<Scope, EdgeOrData<ITerm>> closed = ArrayListMultimap.create();
        closed.put(s0, EdgeOrData.edge(var));
        closed.put(s0, EdgeOrData.data());
        closed.put(d1, EdgeOrData.data());

        // Init only matches s0
        ScopeGraphDiffer<Scope, ITerm, ITerm> differ =
            ScopeGraphDiffer.of(previous, ops, new CollectionStatusOps<>(allLabels, closed));
        DifferState.Immutable<Scope, ITerm, ITerm> state1 = differ.doDiff(current, differ.initDiff(s0, s0), closed);

        assertScopeMatches(state1, s0, s0);
        assertEdgeMatches(state1);

        current = current.setDatum(d2, B.newAppl("B"));
        closed.put(d2, EdgeOrData.data());

        ScopeGraphDiff<Scope, ITerm, ITerm> diff = differ.finalize(current, differ.doDiff(current, state1, closed));

        assertScopeMatches(diff, s0, s0, d1, d3, d2, d4);
        assertEdgeMatches(diff, edge(s0, var, d1), edge(s0, var, d3), edge(s0, var, d2), edge(s0, var, d4));
        assertDiffEmpty(diff);
    }

    @Test public void testGraphAdditions_Circle_MiddleLast() throws Exception {
        // @formatter:off
        current = current.addEdge(s0, P, s1)
            .addEdge(s2, P, s0);
        // @formatter:on

        // @formatter:off
        previous = previous.addEdge(s0, P, s3)
            .addEdge(s3, P, s4)
            .addEdge(s4, P, s0);
        // @formatter:on

        Multimap<Scope, ITerm> allLabels = ArrayListMultimap.create();
        allLabels.put(s0, P);
        allLabels.put(s1, P);
        allLabels.put(s2, P);

        Multimap<Scope, EdgeOrData<ITerm>> closed = ArrayListMultimap.create();
        closed.put(s0, EdgeOrData.edge(P));
        closed.put(s2, EdgeOrData.edge(P));
        closed.put(s0, EdgeOrData.data());
        closed.put(s1, EdgeOrData.data());
        closed.put(s2, EdgeOrData.data());

        // Init only matches s0
        ScopeGraphDiffer<Scope, ITerm, ITerm> differ =
            ScopeGraphDiffer.of(previous, ops, new CollectionStatusOps<>(allLabels, closed));
        DifferState.Immutable<Scope, ITerm, ITerm> state1 = differ.doDiff(current, differ.initDiff(s0, s0), closed);

        assertScopeMatches(state1, s0, s0, s1, s3);
        assertEdgeMatches(state1, edge(s0, P, s1), edge(s0, P, s3));

        // Finish cycle matches all three edges
        current = current.addEdge(s1, P, s2);
        closed.put(s1, EdgeOrData.edge(P));
        DifferState.Immutable<Scope, ITerm, ITerm> state2 = differ.doDiff(current, state1, closed);

        assertScopeMatches(state2, s0, s0, s1, s3, s2, s4);
        assertEdgeMatches(state2, edge(s0, P, s1), edge(s0, P, s3), edge(s1, P, s2), edge(s3, P, s4), edge(s2, P, s0),
            edge(s4, P, s0));
    }

    @Test public void testGraphAdditions_Circle_IncomingLast() throws Exception {
        // @formatter:off
        current = current.addEdge(s0, P, s1)
            .addEdge(s1, P, s2);
        // @formatter:on

        // @formatter:off
        previous = previous.addEdge(s0, P, s3)
            .addEdge(s3, P, s4)
            .addEdge(s4, P, s0);
        // @formatter:on

        Multimap<Scope, ITerm> allLabels = ArrayListMultimap.create();
        allLabels.put(s0, P);
        allLabels.put(s1, P);
        allLabels.put(s2, P);

        Multimap<Scope, EdgeOrData<ITerm>> closed = ArrayListMultimap.create();
        closed.put(s0, EdgeOrData.edge(P));
        closed.put(s1, EdgeOrData.edge(P));
        closed.put(s0, EdgeOrData.data());
        closed.put(s1, EdgeOrData.data());
        closed.put(s2, EdgeOrData.data());

        // Init only matches s0
        ScopeGraphDiffer<Scope, ITerm, ITerm> differ =
            ScopeGraphDiffer.of(previous, ops, new CollectionStatusOps<>(allLabels, closed));
        DifferState.Immutable<Scope, ITerm, ITerm> state1 = differ.doDiff(current, differ.initDiff(s0, s0), closed);

        assertScopeMatches(state1, s0, s0, s1, s3, s2, s4);
        assertEdgeMatches(state1, edge(s0, P, s1), edge(s0, P, s3), edge(s1, P, s2), edge(s3, P, s4));

        // Finish cycle matches all three edges
        current = current.addEdge(s2, P, s0);
        closed.put(s2, EdgeOrData.edge(P));
        DifferState.Immutable<Scope, ITerm, ITerm> state2 = differ.doDiff(current, state1, closed);

        assertScopeMatches(state2, s0, s0, s1, s3, s2, s4);
        assertEdgeMatches(state2, edge(s0, P, s1), edge(s0, P, s3), edge(s1, P, s2), edge(s3, P, s4), edge(s2, P, s0),
            edge(s4, P, s0));
    }

    @Test public void testGraphAdditions_Circle_OutgoingLast() throws Exception {
        // @formatter:off
        current = current.addEdge(s2, P, s0)
            .addEdge(s1, P, s2);
        // @formatter:on

        // @formatter:off
        previous = previous.addEdge(s0, P, s3)
            .addEdge(s3, P, s4)
            .addEdge(s4, P, s0);
        // @formatter:on

        Multimap<Scope, ITerm> allLabels = ArrayListMultimap.create();
        allLabels.put(s0, P);
        allLabels.put(s1, P);
        allLabels.put(s2, P);

        Multimap<Scope, EdgeOrData<ITerm>> closed = ArrayListMultimap.create();
        closed.put(s1, EdgeOrData.edge(P));
        closed.put(s2, EdgeOrData.edge(P));
        closed.put(s0, EdgeOrData.data());
        closed.put(s1, EdgeOrData.data());
        closed.put(s2, EdgeOrData.data());

        // Init only matches s0
        ScopeGraphDiffer<Scope, ITerm, ITerm> differ =
            ScopeGraphDiffer.of(previous, ops, new CollectionStatusOps<>(allLabels, closed));
        DifferState.Immutable<Scope, ITerm, ITerm> state1 = differ.doDiff(current, differ.initDiff(s0, s0), closed);

        assertScopeMatches(state1, s0, s0);
        assertEdgeMatches(state1);

        // Finish cycle matches all three edges
        current = current.addEdge(s0, P, s1);
        closed.put(s0, EdgeOrData.edge(P));
        DifferState.Immutable<Scope, ITerm, ITerm> state2 = differ.doDiff(current, state1, closed);

        assertScopeMatches(state2, s0, s0, s1, s3, s2, s4);
        assertEdgeMatches(state2, edge(s0, P, s1), edge(s0, P, s3), edge(s1, P, s2), edge(s3, P, s4), edge(s2, P, s0),
            edge(s4, P, s0));
    }

    // Asserts

    @SafeVarargs private static <S, L, D> void assertScopeMatches(DifferState.Immutable<S, L, D> state, S... scopes) {
        assertEquals(
            "Expected " + scopes.length / 2 + " scope matches, but was " + state.matchedScopes().keySet().size(),
            scopes.length / 2, state.matchedScopes().keySet().size());
        for(int i = 0; i < scopes.length; i += 2) {
            assertTrue("Expected " + scopes[i] + " to match " + scopes[i + 1],
                state.matchedScopes().containsEntry(scopes[i], scopes[i + 1]));
        }
    }

    @SafeVarargs private static <S, L, D> void assertScopeMatches(ScopeGraphDiff<S, L, D> diff, S... scopes) {
        assertEquals(
            "Expected " + scopes.length / 2 + " scope matches, but was " + diff.matchedScopes().keySet().size(),
            scopes.length / 2, diff.matchedScopes().keySet().size());
        for(int i = 0; i < scopes.length; i += 2) {
            assertTrue("Expected " + scopes[i] + " to match " + scopes[i + 1],
                diff.matchedScopes().containsEntry(scopes[i], scopes[i + 1]));
        }
    }

    @SafeVarargs private static <S, L, D> void assertEdgeMatches(DifferState.Immutable<S, L, D> state,
        Edge<S, L>... edges) {
        assertEquals("Expected " + edges.length / 2 + " edge matches, but was " + state.matchedEdges().keySet().size(),
            edges.length / 2, state.matchedEdges().keySet().size());
        for(int i = 0; i < edges.length; i += 2) {
            assertTrue("Expected " + edges[i] + " to match " + edges[i + 1],
                state.matchedEdges().containsEntry(edges[i], edges[i + 1]));
        }
    }

    @SafeVarargs private static <S, L, D> void assertEdgeMatches(ScopeGraphDiff<S, L, D> diff, Edge<S, L>... edges) {
        assertEquals("Expected " + edges.length / 2 + " edge matches, but was " + diff.matchedEdges().keySet().size(),
            edges.length / 2, diff.matchedEdges().keySet().size());
        for(int i = 0; i < edges.length; i += 2) {
            assertTrue("Expected " + edges[i] + " to match " + edges[i + 1],
                diff.matchedEdges().containsEntry(edges[i], edges[i + 1]));
        }
    }

    private <S, L, D> void assertDiffEmpty(ScopeGraphDiff<S, L, D> diff) {
        assertEquals("Expected no added scopes", 0, diff.added().scopes().size());
        assertEquals("Expected no added edges", 0, diff.added().edges().size());
        assertEquals("Expected no removed scopes", 0, diff.removed().scopes().size());
        assertEquals("Expected no removed edges", 0, diff.removed().edges().size());
    }

    // Helper functions
    private <S, L> Edge<S, L> edge(S src, L lbl, S tgt) {
        return new Edge<>(src, lbl, tgt);
    }

    private static ScopeGraphDiff<Scope, ITerm, ITerm> diff(IScopeGraph.Immutable<Scope, ITerm, ITerm> sc1,
        IScopeGraph.Immutable<Scope, ITerm, ITerm> sc2) {
        return ScopeGraphDiffer.fullDiff(s0, s0, sc1, sc2,
            new StatixDifferOps(PersistentUnifier.Immutable.of(), PersistentUnifier.Immutable.of()));
    }

    private static class CollectionStatusOps<S, L> implements ScopeGraphStatusOps<S, L> {

        private final Multimap<S, L> allLabels;
        private final Multimap<S, EdgeOrData<L>> closedLabels;

        public CollectionStatusOps(Multimap<S, L> allLabels, Multimap<S, EdgeOrData<L>> closedLabels) {
            this.allLabels = allLabels;
            this.closedLabels = closedLabels;
        }

        @Override public boolean closed(S scope, EdgeOrData<L> label) {
            return closedLabels.containsEntry(scope, label);
        }

        @Override public Collection<L> allLabels(S scope) {
            return allLabels.get(scope);
        }

    }
}
