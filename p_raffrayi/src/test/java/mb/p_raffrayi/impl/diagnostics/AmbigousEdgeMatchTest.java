package mb.p_raffrayi.impl.diagnostics;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.annotation.Nullable;

import org.junit.Test;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.future.IFuture;

import mb.p_raffrayi.impl.diagnostics.AmbigousEdgeMatch.Match;
import mb.p_raffrayi.impl.diagnostics.AmbigousEdgeMatch.Report;
import mb.p_raffrayi.impl.diff.IDifferOps;
import mb.scopegraph.oopsla20.IScopeGraph;
import org.metaborg.util.collection.BiMap;
import org.metaborg.util.collection.BiMap.Immutable;
import mb.scopegraph.oopsla20.reference.ScopeGraph;

import static mb.p_raffrayi.impl.diagnostics.AmbigousEdgeMatchTest.Label.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AmbigousEdgeMatchTest {

    private static final Scope root = new Scope(0, 0);

    private static final IScopeGraph.Immutable<Scope, Label, IDatum> empty = ScopeGraph.Immutable.of();

    // Regular edge ambiguity tests.

    @Test public void testSingleEdge() {
        final Scope tgt = new Scope(0, 1);
        final IScopeGraph.Immutable<Scope, Label, IDatum> sg = empty.addEdge(root, L1, tgt);

        final Report<Scope, Label, IDatum> report =
                new AmbigousEdgeMatch<>(sg, Arrays.asList(root), new TestDifferOps(0)).analyze();

        assertEmpty(report);
    }

    @Test public void testSingleEdgeWithData() {
        final Scope tgt = new Scope(0, 1);
        // @formatter:off
        final IScopeGraph.Immutable<Scope, Label, IDatum> sg = empty
                .addEdge(root, L1, tgt)
                .setDatum(tgt, tgt);
        // @formatter:on

        final Report<Scope, Label, IDatum> report =
                new AmbigousEdgeMatch<>(sg, Arrays.asList(root), new TestDifferOps(0)).analyze();

        assertEmpty(report);
    }

    @Test public void testAmbiguousEdgeNoData() {
        final Scope tgt1 = new Scope(0, 1);
        final Scope tgt2 = new Scope(0, 2);

        // @formatter:off
        final IScopeGraph.Immutable<Scope, Label, IDatum> sg = empty
                .addEdge(root, L1, tgt1)
                .addEdge(root, L1, tgt2);
        // @formatter:on

        final Report<Scope, Label, IDatum> report =
                new AmbigousEdgeMatch<>(sg, Arrays.asList(root), new TestDifferOps(0)).analyze();

        assertSize(report, 1);
        assertContains(report, root, L1, tgt1, null, tgt2, null);
    }

    @Test public void testAmbiguousEdgeData() {
        final Scope tgt1 = new Scope(0, 1);
        final Scope tgt2 = new Scope(0, 2);

        // @formatter:off
        final IScopeGraph.Immutable<Scope, Label, IDatum> sg = empty
                .addEdge(root, L1, tgt1)
                .addEdge(root, L1, tgt2)
                .setDatum(tgt1, tgt1)
                .setDatum(tgt2, tgt2);
        // @formatter:on

        final Report<Scope, Label, IDatum> report =
                new AmbigousEdgeMatch<>(sg, Arrays.asList(root), new TestDifferOps(0)).analyze();

        assertSize(report, 1);
        assertContains(report, root, L1, tgt1, null, tgt2, null);
    }

    @Test public void testDifferentLabel() {
        final Scope tgt1 = new Scope(0, 1);
        final Scope tgt2 = new Scope(0, 2);

        // @formatter:off
        final IScopeGraph.Immutable<Scope, Label, IDatum> sg = empty
                .addEdge(root, L1, tgt1)
                .addEdge(root, L2, tgt2);
        // @formatter:on

        final Report<Scope, Label, IDatum> report =
                new AmbigousEdgeMatch<>(sg, Arrays.asList(root), new TestDifferOps(0)).analyze();

        assertEmpty(report);
    }

    @Test public void testDifferentData() {
        final Scope tgt1 = new Scope(0, 1);
        final Scope tgt2 = new Scope(0, 2);

        // @formatter:off
        final IScopeGraph.Immutable<Scope, Label, IDatum> sg = empty
                .addEdge(root, L1, tgt1)
                .addEdge(root, L1, tgt2)
                .setDatum(tgt2, tgt2);
        // @formatter:on

        final Report<Scope, Label, IDatum> report =
                new AmbigousEdgeMatch<>(sg, Arrays.asList(root), new TestDifferOps(0)).analyze();

        assertEmpty(report);
    }

    @Test public void testDoubleExternalEdge() {
        final Scope tgt1 = new Scope(1, 0);
        final Scope tgt2 = new Scope(1, 1);

        // @formatter:off
        final IScopeGraph.Immutable<Scope, Label, IDatum> sg = empty
                .addEdge(root, L1, tgt1)
                .addEdge(root, L1, tgt2);
        // @formatter:on

        final Report<Scope, Label, IDatum> report =
                new AmbigousEdgeMatch<>(sg, Arrays.asList(root), new TestDifferOps(0)).analyze();

        assertEmpty(report);
    }

    @Test public void testCycle() {
        final Scope s1 = new Scope(0, 1);
        final Scope s2 = new Scope(0, 1);

        // @formatter:off
        final IScopeGraph.Immutable<Scope, Label, IDatum> sg = empty
                .addEdge(root, L1, s1)
                .addEdge(s1, L1, s2)
                .addEdge(s2, L2, root);
        // @formatter:on

        final Report<Scope, Label, IDatum> report =
                new AmbigousEdgeMatch<>(sg, Arrays.asList(root), new TestDifferOps(0)).analyze();

        assertEmpty(report);
    }

    // Declaration edge ambiguity tests.

    @Test public void testSingleDecl() {
        final Scope tgt = new Scope(0, 1);
        final Datum d = new Datum("T", Arrays.asList());

        // @formatter:off
        final IScopeGraph.Immutable<Scope, Label, IDatum> sg = empty
                .addEdge(root, R1, tgt)
                .setDatum(tgt, d);
        // @formatter:on

        final Report<Scope, Label, IDatum> report =
                new AmbigousEdgeMatch<>(sg, Arrays.asList(root), new TestDifferOps(0)).analyze();

        assertEmpty(report);
    }

    @Test public void testDoubleDecl() {
        final Scope tgt1 = new Scope(0, 1);
        final Scope tgt2 = new Scope(0, 2);
        final Datum d = new Datum("T", Arrays.asList());

        // @formatter:off
        final IScopeGraph.Immutable<Scope, Label, IDatum> sg = empty
                .addEdge(root, R1, tgt1)
                .setDatum(tgt1, d)
                .addEdge(root, R1, tgt2)
                .setDatum(tgt2, d);
        // @formatter:on

        final Report<Scope, Label, IDatum> report =
                new AmbigousEdgeMatch<>(sg, Arrays.asList(root), new TestDifferOps(0)).analyze();

        assertSize(report, 1);
        assertContains(report, root, R1, tgt1, d, tgt2, d);
    }

    @Test public void testDifferentDeclData() {
        final Scope tgt1 = new Scope(0, 1);
        final Scope tgt2 = new Scope(0, 2);
        final Datum d1 = new Datum("T", Arrays.asList());
        final Datum d2 = new Datum("V", Arrays.asList());

        // @formatter:off
        final IScopeGraph.Immutable<Scope, Label, IDatum> sg = empty
                .addEdge(root, R1, tgt1)
                .setDatum(tgt1, d1)
                .addEdge(root, R1, tgt2)
                .setDatum(tgt2, d2);
        // @formatter:on

        final Report<Scope, Label, IDatum> report =
                new AmbigousEdgeMatch<>(sg, Arrays.asList(root), new TestDifferOps(0)).analyze();

        assertEmpty(report);
    }

    @Test public void testDoubleDecl_FreeScopes() {
        final Scope tgt1 = new Scope(0, 1);
        final Scope tgt2 = new Scope(0, 2);
        final Scope ds1 = new Scope(0, 3);
        final Scope ds2 = new Scope(0, 4);

        final Datum d1 = new Datum("T", Arrays.asList(ds1));
        final Datum d2 = new Datum("T", Arrays.asList(ds2));

        // @formatter:off
        final IScopeGraph.Immutable<Scope, Label, IDatum> sg = empty
                .addEdge(root, R1, tgt1)
                .setDatum(tgt1, d1)
                .addEdge(root, R1, tgt2)
                .setDatum(tgt2, d2);
        // @formatter:on

        final Report<Scope, Label, IDatum> report =
                new AmbigousEdgeMatch<>(sg, Arrays.asList(root), new TestDifferOps(0)).analyze();

        assertSize(report, 1);
        assertContains(report, root, R1, tgt1, d1, tgt2, d2);
    }

    @Test public void testDoubleDecl_FreeScopes_DifferentOwners() {
        final Scope tgt1 = new Scope(0, 1);
        final Scope tgt2 = new Scope(0, 2);
        final Scope ds1 = new Scope(0, 3);
        final Scope ds2 = new Scope(1, 4);

        final Datum d1 = new Datum("T", Arrays.asList(ds1));
        final Datum d2 = new Datum("T", Arrays.asList(ds2));

        // @formatter:off
        final IScopeGraph.Immutable<Scope, Label, IDatum> sg = empty
                .addEdge(root, R1, tgt1)
                .setDatum(tgt1, d1)
                .addEdge(root, R1, tgt2)
                .setDatum(tgt2, d2);
        // @formatter:on

        final Report<Scope, Label, IDatum> report =
                new AmbigousEdgeMatch<>(sg, Arrays.asList(root), new TestDifferOps(0)).analyze();

        assertEmpty(report);
    }

    @Test public void testDoubleDecl_FreeScopes_ReferBack() {
        final Scope tgt1 = new Scope(0, 1);
        final Scope tgt2 = new Scope(0, 2);
        final Scope ds1 = new Scope(0, 3);

        final Datum d1 = new Datum("T", Arrays.asList(ds1));
        final Datum d2 = new Datum("T", Arrays.asList(root));

        // @formatter:off
        final IScopeGraph.Immutable<Scope, Label, IDatum> sg = empty
                .addEdge(root, R1, tgt1)
                .setDatum(tgt1, d1)
                .addEdge(root, R1, tgt2)
                .setDatum(tgt2, d2);
        // @formatter:on

        final Report<Scope, Label, IDatum> report =
                new AmbigousEdgeMatch<>(sg, Arrays.asList(root), new TestDifferOps(0)).analyze();

        assertEmpty(report);
    }

    @Test public void testDoubleDecl_NestedConflict() {
        final Scope tgt1 = new Scope(0, 1);
        final Scope tgt2 = new Scope(0, 2);
        final Scope ds1 = new Scope(0, 3);
        final Scope ds2 = new Scope(0, 4);

        final Datum d1 = new Datum("T", Arrays.asList(ds1));
        final Datum d2 = new Datum("T", Arrays.asList(ds2));

        final Datum d3 = new Datum("T", Arrays.asList());
        final Datum d4 = new Datum("V", Arrays.asList());

        // @formatter:off
        final IScopeGraph.Immutable<Scope, Label, IDatum> sg = empty
                .addEdge(root, R1, tgt1)
                .addEdge(root, R1, tgt2)
                .setDatum(tgt1, d1)
                .setDatum(tgt2, d2)
                .setDatum(ds1, d3)
                .setDatum(ds2, d4);
        // @formatter:on

        final Report<Scope, Label, IDatum> report =
                new AmbigousEdgeMatch<>(sg, Arrays.asList(root), new TestDifferOps(0)).analyze();

        assertEmpty(report);
    }

    @Test public void testDoubleDecl_FreeScopes_DifferentExternal() {
        final Scope tgt1 = new Scope(0, 1);
        final Scope tgt2 = new Scope(0, 2);
        final Scope ds1 = new Scope(1, 3);
        final Scope ds2 = new Scope(1, 4);

        final Datum d1 = new Datum("T", Arrays.asList(ds1));
        final Datum d2 = new Datum("T", Arrays.asList(ds2));

        // @formatter:off
        final IScopeGraph.Immutable<Scope, Label, IDatum> sg = empty
                .addEdge(root, R1, tgt1)
                .setDatum(tgt1, d1)
                .addEdge(root, R1, tgt2)
                .setDatum(tgt2, d2);
        // @formatter:on

        final Report<Scope, Label, IDatum> report =
                new AmbigousEdgeMatch<>(sg, Arrays.asList(root), new TestDifferOps(0)).analyze();

        assertEmpty(report);
    }

    @Test public void testAmbInDataScope() {
        final Scope tgt = new Scope(0, 1);
        final Scope ts = new Scope(0, 2);
        final Scope ttgt1 = new Scope(0, 3);
        final Scope ttgt2 = new Scope(0, 3);

        final Datum d = new Datum("T", Arrays.asList(ts));

        // @formatter:off
        final IScopeGraph.Immutable<Scope, Label, IDatum> sg = empty
                .addEdge(root, R1, tgt)
                .setDatum(tgt, d)
                .addEdge(ts, L1, ttgt1)
                .addEdge(ts, L1, ttgt2);
        // @formatter:on

        final Report<Scope, Label, IDatum> report =
                new AmbigousEdgeMatch<>(sg, Arrays.asList(root), new TestDifferOps(0)).analyze();

        assertSize(report, 1);
        assertContains(report, ts, L1, ttgt1, null, ttgt2, null);
    }

    // Helper assertions

    private void assertEmpty(Report<Scope, Label, IDatum> report) {
        assertTrue("Expected empty report, but got " + report + ".", report.isEmpty());
    }

    private void assertSize(Report<Scope, Label, IDatum> report, int expectedSize) {
        assertEquals(
                "Expected report of size " + expectedSize + ", but got " + report.size() + ".",
                expectedSize, report.size());

    }

    private void assertContains(Report<Scope, Label, IDatum> report, Scope src, Label lbl, Scope tgt1,
            @Nullable IDatum d1, Scope tgt2, @Nullable IDatum d2) {
        final Match<Scope, IDatum> match1 = new Match<>(tgt1, d1, tgt2, d2);
        final Match<Scope, IDatum> match2 = new Match<>(tgt2, d2, tgt1, d1);

        final String msg = "Expected report to contain " + src + " -" + lbl + "-> " + match1 + ", but got " + report + ".";

        assertTrue(msg, report.contains(src, lbl, match1) || report.contains(src, lbl, match2));
    }

    // Scope graph parameter implementations.

    interface IDatum {
        <R> R match(Function1<Scope, R> onScope, Function1<Datum, R> onData);
    }

    static final class Scope implements IDatum {

        private final int owner;

        private int number;

        protected Scope(int owner, int number) {
            this.owner = owner;
            this.number = number;
        }

        @Override public String toString() {
            return "#" + owner + "-" + number;
        }

        @Override public <R> R match(Function1<Scope, R> onScope, Function1<Datum, R> onDatum) {
            return onScope.apply(this);
        }

    }

    enum Label {
        L1, L2, R1, R2
    }

    static final class Datum implements IDatum {

        private String cons;

        private List<Scope> arguments;

        protected Datum(String cons, List<Scope> arguments) {
            this.cons = cons;
            this.arguments = arguments;
        }

        public String getCons() {
            return cons;
        }

        public List<Scope> getArguments() {
            return arguments;
        }

        @Override public <R> R match(Function1<Scope, R> onScope, Function1<Datum, R> onData) {
            return onData.apply(this);
        }

        @Override public String toString() {
            // TODO Auto-generated method stub
            return cons + "(" + arguments.stream().map(Object::toString).collect(Collectors.joining(",")) + ")";
        }

        @Override public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((arguments == null) ? 0 : arguments.hashCode());
            result = prime * result + ((cons == null) ? 0 : cons.hashCode());
            return result;
        }

        @Override public boolean equals(Object obj) {
            if(this == obj) {
                return true;
            }
            if(obj == null) {
                return false;
            }
            if(getClass() != obj.getClass()) {
                return false;
            }
            final Datum other = (Datum) obj;
            return Objects.equals(cons, other.cons) && Objects.equals(arguments, other.arguments);
        }

    }

    private static final class TestDifferOps implements IDifferOps<Scope, Label, IDatum> {

        private final int ownId;

        public TestDifferOps(int ownId) {
            this.ownId = ownId;
        }

        @Override public boolean isMatchAllowed(Scope currentScope, Scope previousScope) {
            return currentScope.owner == previousScope.owner;
        }

        @Override public Optional<Immutable<Scope>> matchDatums(IDatum currentDatum, IDatum previousDatum) {
            // @formatter:off
            return currentDatum.match(
                cScope -> previousDatum.match(
                    pScope -> Optional.of(BiMap.Immutable.of(cScope, pScope)),
                    pDatum -> Optional.empty()
                ),
                cDatum -> previousDatum.match(
                    pScope -> Optional.empty(),
                    pDatum -> {
                        if(pDatum.cons.equals(cDatum.cons) && pDatum.arguments.size() == cDatum.arguments.size()) {
                            final BiMap.Transient<Scope> scopeMatches = BiMap.Transient.of();
                            final Iterator<Scope> pIter = pDatum.arguments.iterator();
                            final Iterator<Scope> cIter = cDatum.arguments.iterator();
                            while(pIter.hasNext()) {
                                final Scope pMatch = pIter.next();
                                final Scope cMatch = cIter.next();
                                if(!scopeMatches.canPut(cMatch, pMatch)) {
                                    return Optional.empty();
                                } else {
                                    scopeMatches.put(cMatch, pMatch);
                                }
                            }
                            return Optional.of(scopeMatches.freeze());
                        }
                        return Optional.empty();
                    }
                )
            );
            // @formatter:on
        }

        @Override public Collection<Scope> getScopes(IDatum d) {
            return d.match(Collections::singleton, Datum::getArguments);
        }

        @Override public IDatum embed(Scope scope) {
            return scope;
        }

        @Override public boolean ownScope(Scope scope) {
            return scope.owner == ownId;
        }

        @Override public boolean ownOrSharedScope(Scope currentScope) {
            throw new UnsupportedOperationException();
        }

        @Override public IFuture<Optional<Scope>> externalMatch(Scope previousScope) {
            throw new UnsupportedOperationException();
        }

    }

}
