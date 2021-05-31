package mb.scopegraph.pepm16.esop.bottomup;

import static mb.nabl2.terms.build.TermBuild.B;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.junit.Test;
import org.metaborg.util.task.NullCancel;
import org.metaborg.util.task.NullProgress;

import mb.nabl2.terms.stratego.TermIndex;
import mb.scopegraph.pepm16.IResolutionParameters.Strategy;
import mb.scopegraph.pepm16.bottomup.BUNameResolution;
import mb.scopegraph.pepm16.esop15.IEsopNameResolution;
import mb.scopegraph.pepm16.esop15.IEsopScopeGraph;
import mb.scopegraph.pepm16.terms.Label;
import mb.scopegraph.pepm16.terms.Namespace;
import mb.scopegraph.pepm16.terms.Occurrence;
import mb.scopegraph.pepm16.terms.OccurrenceIndex;
import mb.scopegraph.pepm16.terms.ResolutionParameters;
import mb.scopegraph.pepm16.terms.Scope;
import mb.scopegraph.regexp.IAlphabet;
import mb.scopegraph.regexp.IRegExp;
import mb.scopegraph.regexp.IRegExpBuilder;
import mb.scopegraph.regexp.impl.FiniteAlphabet;
import mb.scopegraph.regexp.impl.RegExpBuilder;
import mb.scopegraph.relations.IRelation;
import mb.scopegraph.relations.RelationDescription;
import mb.scopegraph.relations.impl.Relation;

public class BUNameResolutionTest {

    final Scope s1 = Scope.of("", "s1");
    final Scope s2 = Scope.of("", "s2");
    final Scope s3 = Scope.of("", "s3");
    final Scope s4 = Scope.of("", "s4");
    final Scope s5 = Scope.of("", "s5");

    final Occurrence x1 = Occurrence.of(Namespace.of(""), B.newString("x"), OccurrenceIndex.of(TermIndex.of("", 1)));
    final Occurrence x2 = Occurrence.of(Namespace.of(""), B.newString("x"), OccurrenceIndex.of(TermIndex.of("", 2)));

    final Label R = Label.of("R");
    final Label D = Label.of("D");
    final Label LEX = Label.of("LEX");
    final IAlphabet<Label> labels = new FiniteAlphabet<>(D, LEX);

    @Test(timeout = 3000) public void singleScope() throws Throwable {
        IEsopScopeGraph.Transient<Scope, Label, Occurrence, ?> scopeGraph = IEsopScopeGraph.builder();
        IRegExpBuilder<Label> wfB = new RegExpBuilder<>();
        IRegExp<Label> wf = wfB.complement(wfB.emptySet());
        IRelation.Transient<Label> ord = Relation.Transient.of(RelationDescription.STRICT_PARTIAL_ORDER);
        ord.add(D, LEX);
        ResolutionParameters params =
                ResolutionParameters.of(labels, D, R, wf, ord.freeze(), Strategy.ENVIRONMENTS, false);
        BUNameResolution<Scope, Label, Occurrence> nr = BUNameResolution.of(params, scopeGraph, (s, l) -> true);

        scopeGraph.addDecl(s1, x1);

        Collection<Occurrence> ans = nr.visible(s1, new NullCancel(), new NullProgress());
        assertEquals(1, ans.size());
        assertTrue(ans.contains(x1));
    }

    @Test(timeout = 3000) public void parentScope() throws Throwable {
        IEsopScopeGraph.Transient<Scope, Label, Occurrence, ?> scopeGraph = IEsopScopeGraph.builder();
        IRegExpBuilder<Label> wfB = new RegExpBuilder<>();
        IRegExp<Label> wf = wfB.complement(wfB.emptySet());
        IRelation.Transient<Label> ord = Relation.Transient.of(RelationDescription.STRICT_PARTIAL_ORDER);
        ord.add(D, LEX);
        ResolutionParameters params =
                ResolutionParameters.of(labels, D, R, wf, ord.freeze(), Strategy.ENVIRONMENTS, false);
        BUNameResolution<Scope, Label, Occurrence> nr = BUNameResolution.of(params, scopeGraph, (s, l) -> true);

        scopeGraph.addDecl(s1, x1);

        scopeGraph.addDirectEdge(s2, LEX, s1);

        Collection<Occurrence> ans = nr.visible(s2, new NullCancel(), new NullProgress());
        assertEquals(1, ans.size());
        assertTrue(ans.contains(x1));
    }

    @Test(timeout = 3000) public void parentScopeShadowed() throws Throwable {
        IEsopScopeGraph.Transient<Scope, Label, Occurrence, ?> scopeGraph = IEsopScopeGraph.builder();
        IRegExpBuilder<Label> wfB = new RegExpBuilder<>();
        IRegExp<Label> wf = wfB.complement(wfB.emptySet());
        IRelation.Transient<Label> ord = Relation.Transient.of(RelationDescription.STRICT_PARTIAL_ORDER);
        ord.add(D, LEX);
        ResolutionParameters params =
                ResolutionParameters.of(labels, D, R, wf, ord.freeze(), Strategy.ENVIRONMENTS, false);
        BUNameResolution<Scope, Label, Occurrence> nr = BUNameResolution.of(params, scopeGraph, (s, l) -> true);

        scopeGraph.addDecl(s1, x1);

        scopeGraph.addDecl(s2, x2);
        scopeGraph.addDirectEdge(s2, LEX, s1);

        Collection<Occurrence> ans = nr.visible(s2, new NullCancel(), new NullProgress());
        assertEquals(1, ans.size());
        assertTrue(ans.contains(x2));
    }

    @Test(timeout = 3000) public void grandParentScopeShadowed() throws Throwable {
        IEsopScopeGraph.Transient<Scope, Label, Occurrence, ?> scopeGraph = IEsopScopeGraph.builder();
        IRegExpBuilder<Label> wfB = new RegExpBuilder<>();
        IRegExp<Label> wf = wfB.complement(wfB.emptySet());
        IRelation.Transient<Label> ord = Relation.Transient.of(RelationDescription.STRICT_PARTIAL_ORDER);
        ord.add(D, LEX);
        ResolutionParameters params =
                ResolutionParameters.of(labels, D, R, wf, ord.freeze(), Strategy.ENVIRONMENTS, false);
        BUNameResolution<Scope, Label, Occurrence> nr = BUNameResolution.of(params, scopeGraph, (s, l) -> true);

        scopeGraph.addDecl(s1, x1);

        scopeGraph.addDecl(s2, x2);
        scopeGraph.addDirectEdge(s2, LEX, s1);

        scopeGraph.addDirectEdge(s3, LEX, s2);

        Collection<Occurrence> ans = nr.visible(s3, new NullCancel(), new NullProgress());
        assertEquals(1, ans.size());
        assertContains(x2, ans);
    }

    @Test(timeout = 3000) public void indirectCycle() throws Throwable {
        IEsopScopeGraph.Transient<Scope, Label, Occurrence, ?> scopeGraph = IEsopScopeGraph.builder();
        IRegExpBuilder<Label> wfB = new RegExpBuilder<>();
        IRegExp<Label> wf = wfB.complement(wfB.emptySet());
        IRelation.Transient<Label> ord = Relation.Transient.of(RelationDescription.STRICT_PARTIAL_ORDER);
        ord.add(D, LEX);
        ResolutionParameters params =
                ResolutionParameters.of(labels, D, R, wf, ord.freeze(), Strategy.ENVIRONMENTS, false);
        BUNameResolution<Scope, Label, Occurrence> nr = BUNameResolution.of(params, scopeGraph, (s, l) -> true);

        scopeGraph.addDecl(s1, x1);
        scopeGraph.addDirectEdge(s1, LEX, s2);

        scopeGraph.addDecl(s2, x2);
        scopeGraph.addDirectEdge(s2, LEX, s3);

        scopeGraph.addDirectEdge(s3, LEX, s1);

        {
            Collection<Occurrence> ans1 = nr.visible(s1, new NullCancel(), new NullProgress());
            assertEquals(1, ans1.size());
            assertContains(x1, ans1);
        }
        {
            Collection<Occurrence> ans2 = nr.visible(s2, new NullCancel(), new NullProgress());
            assertEquals(1, ans2.size());
            assertContains(x2, ans2);
        }
        {
            Collection<Occurrence> ans3 = nr.visible(s3, new NullCancel(), new NullProgress());
            assertEquals(1, ans3.size());
            assertContains(x1, ans3);
        }
    }

    @Test(timeout = 3000) public void directCycle() throws Throwable {
        IEsopScopeGraph.Transient<Scope, Label, Occurrence, ?> scopeGraph = IEsopScopeGraph.builder();
        IRegExpBuilder<Label> wfB = new RegExpBuilder<>();
        IRegExp<Label> wf = wfB.complement(wfB.emptySet());
        IRelation.Transient<Label> ord = Relation.Transient.of(RelationDescription.STRICT_PARTIAL_ORDER);
        ord.add(D, LEX);
        ResolutionParameters params =
                ResolutionParameters.of(labels, D, R, wf, ord.freeze(), Strategy.ENVIRONMENTS, false);
        BUNameResolution<Scope, Label, Occurrence> nr = BUNameResolution.of(params, scopeGraph, (s, l) -> true);

        scopeGraph.addDecl(s1, x1);
        scopeGraph.addDirectEdge(s1, LEX, s2);

        scopeGraph.addDecl(s2, x2);
        scopeGraph.addDirectEdge(s2, LEX, s1);

        {
            Collection<Occurrence> ans1 = nr.visible(s1, new NullCancel(), new NullProgress());
            assertEquals(1, ans1.size());
            assertContains(x1, ans1);
        }
        {
            Collection<Occurrence> ans2 = nr.visible(s2, new NullCancel(), new NullProgress());
            assertEquals(1, ans2.size());
            assertContains(x2, ans2);
        }
    }

    @Test(timeout = 3000) public void diamond() throws Throwable {
        IEsopScopeGraph.Transient<Scope, Label, Occurrence, ?> scopeGraph = IEsopScopeGraph.builder();
        IRegExpBuilder<Label> wfB = new RegExpBuilder<>();
        IRegExp<Label> wf = wfB.complement(wfB.emptySet());
        IRelation.Transient<Label> ord = Relation.Transient.of(RelationDescription.STRICT_PARTIAL_ORDER);
        ord.add(D, LEX);
        ResolutionParameters params =
                ResolutionParameters.of(labels, D, R, wf, ord.freeze(), Strategy.ENVIRONMENTS, false);
        BUNameResolution<Scope, Label, Occurrence> nr = BUNameResolution.of(params, scopeGraph, (s, l) -> true);

        scopeGraph.addDecl(s1, x1);

        nr.visible(s1, new NullCancel(), new NullProgress());

        scopeGraph.addDecl(s2, x2);
        scopeGraph.addDirectEdge(s2, LEX, s1);

        scopeGraph.addDirectEdge(s3, LEX, s2);
        scopeGraph.addDirectEdge(s4, LEX, s2);

        scopeGraph.addDirectEdge(s5, LEX, s3);
        scopeGraph.addDirectEdge(s5, LEX, s4);

        Collection<Occurrence> ans = nr.visible(s5, new NullCancel(), new NullProgress());
        assertEquals(1, ans.size());
        assertContains(x2, ans);
    }

    @Test(timeout = 3000) public void chasingTest() throws Throwable {
        IEsopScopeGraph.Transient<Scope, Label, Occurrence, ?> scopeGraph = IEsopScopeGraph.builder();
        IRegExpBuilder<Label> wfB = new RegExpBuilder<>();
        IRegExp<Label> wf = wfB.complement(wfB.emptySet());
        IRelation.Transient<Label> ord = Relation.Transient.of(RelationDescription.STRICT_PARTIAL_ORDER);
        ord.add(D, LEX);
        ResolutionParameters params =
                ResolutionParameters.of(labels, D, R, wf, ord.freeze(), Strategy.ENVIRONMENTS, false);
        IEsopNameResolution<Scope, Label, Occurrence> nr = BUNameResolution.of(params, scopeGraph, (s, l) -> true);

        scopeGraph.addDecl(s1, x1);

        scopeGraph.addDecl(s2, x2);
        scopeGraph.addDirectEdge(s2, LEX, s1);

        scopeGraph.addDirectEdge(s3, LEX, s2);
        scopeGraph.addDirectEdge(s4, LEX, s2);

        scopeGraph.addDirectEdge(s3, LEX, s4);
        scopeGraph.addDirectEdge(s4, LEX, s3);

        {
            Collection<Occurrence> ans = nr.visible(s3, new NullCancel(), new NullProgress());
            assertEquals(1, ans.size());
            assertContains(x2, ans);
        }
        {
            Collection<Occurrence> ans = nr.visible(s4, new NullCancel(), new NullProgress());
            assertEquals(1, ans.size());
            assertContains(x2, ans);
        }
    }

    private static <T> void assertContains(T obj, Collection<T> coll) {
        if(!coll.contains(obj)) {
            throw new AssertionError("Missing " + obj + " in " + coll);
        }
    }

}