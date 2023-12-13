package mb.statix.spoofax;

import static mb.nabl2.terms.matching.TermMatch.M;
import static mb.nabl2.terms.build.TermBuild.B;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.metaborg.util.future.IFuture;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;


import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.u.IUnifier;
import mb.p_raffrayi.IUnitResult;
import mb.p_raffrayi.impl.Result;
import mb.p_raffrayi.impl.diagnostics.AmbigousEdgeMatch;
import mb.p_raffrayi.impl.diagnostics.AmbigousEdgeMatch.Match;
import mb.p_raffrayi.impl.diagnostics.AmbigousEdgeMatch.Report;
import mb.p_raffrayi.impl.diff.IDifferOps;
import mb.scopegraph.oopsla20.IScopeGraph;
import org.metaborg.util.collection.BiMap.Immutable;
import mb.statix.concurrent.IStatixResult;
import mb.statix.concurrent.nameresolution.ScopeImpl;
import mb.statix.scopegraph.Scope;
import mb.statix.solver.tracer.EmptyTracer.Empty;

public class STX_incremental_diagnostics extends StatixPrimitive {

    @jakarta.inject.Inject @javax.inject.Inject public STX_incremental_diagnostics() {
        super(STX_incremental_diagnostics.class.getSimpleName(), 1);
    }

    @Override protected Optional<? extends ITerm> call(IContext env, ITerm term, List<ITerm> terms)
            throws InterpreterException {

        // Diagnostic one: possibility for inaccurate edge matches.

        @SuppressWarnings("unchecked") final IUnitResult<Scope, ITerm, ITerm, Result<Scope, ITerm, ITerm, IStatixResult<Empty>, ?>> analysis =
                M.blobValue(IUnitResult.class).match(terms.get(0))
                        .orElseThrow(() -> new InterpreterException("Expected solver result."));

        final AnalyticsDifferOps differOps = new AnalyticsDifferOps(analysis.scopes());
        final IScopeGraph.Immutable<Scope, ITerm, ITerm> scopeGraph =
                new ExplicatedScopeGraph(analysis.scopeGraph(), analysis.result().analysis().solveResult().state().unifier());
        final AmbigousEdgeMatch<Scope, ITerm, ITerm> aemDiagnostics =
                new AmbigousEdgeMatch<>(scopeGraph, analysis.rootScopes(), differOps);

        final Report<Scope, ITerm, ITerm> report = aemDiagnostics.analyze();

        // Report formatting

        final List<ITerm> matchEntries = new ArrayList<>();
        for(Scope scope : report.scopes()) {
            final List<ITerm> labels = new ArrayList<>();
            for(ITerm label : report.labels(scope)) {
                final List<ITerm> matches = new ArrayList<>();
                for(Match<Scope, ITerm> match : report.matches(scope, label)) {
                    final ITerm d1 = match.getDatum1().map(d -> B.newAppl("Some", d)).orElse(B.newAppl("None"));
                    final ITerm d2 = match.getDatum2().map(d -> B.newAppl("Some", d)).orElse(B.newAppl("None"));
                    matches.add(B.newTuple(match.getScope1(), d1, match.getScope2(), d2));
                }
                labels.add(B.newTuple(label, B.newList(matches)));
            }
            matchEntries.add(B.newTuple(scope, B.newList(labels)));
        }

        return Optional.of(B.newAppl("Diagnostics", B.newList(B.newList(matchEntries))));
    }

    private static final class ExplicatedScopeGraph implements IScopeGraph.Immutable<Scope, ITerm, ITerm> {

        private final IScopeGraph.Immutable<Scope, ITerm, ITerm> scopeGraph;
        private final IUnifier.Immutable unifier;

        public ExplicatedScopeGraph(IScopeGraph.Immutable<Scope, ITerm, ITerm> scopeGraph, IUnifier.Immutable unifier) {
            this.scopeGraph = scopeGraph;
            this.unifier = unifier;
        }

        @Override public Map<? extends Entry<Scope, ITerm>, ? extends Collection<Scope>> getEdges() {
            return scopeGraph.getEdges();
        }

        @Override public Collection<Scope> getEdges(Scope scope, ITerm label) {
            return scopeGraph.getEdges(scope, label);
        }

        @Override public Map<Scope, ITerm> getData() {
            return scopeGraph.getData().entrySet().stream()
                    .collect(Collectors.toMap(Entry::getKey, e -> unifier.findRecursive(e.getValue())));
        }

        @Override public Optional<ITerm> getData(Scope scope) {
            return scopeGraph.getData(scope).map(unifier::findRecursive);
        }

        @Override public Set.Immutable<ITerm> getLabels() {
            return scopeGraph.getLabels();
        }

        @Override public Immutable<Scope, ITerm, ITerm> addEdge(Scope sourceScope, ITerm label, Scope targetScope) {
            return new ExplicatedScopeGraph(scopeGraph.addEdge(sourceScope, label, targetScope), unifier);
        }

        @Override public Immutable<Scope, ITerm, ITerm> setDatum(Scope scope, ITerm datum) {
            return new ExplicatedScopeGraph(scopeGraph.setDatum(scope, datum), unifier);
        }

        @Override public Immutable<Scope, ITerm, ITerm> addAll(IScopeGraph<Scope, ITerm, ITerm> other) {
            return new ExplicatedScopeGraph(scopeGraph.addAll(other), unifier);
        }

        @Override public Transient<Scope, ITerm, ITerm> melt() {
            // Can be fixed by implementing ExplicatedScopeGraph.Transient
            throw new UnsupportedOperationException("Cannot melt scope graph with accompanied unifier.");
        }

    }

    private static class AnalyticsDifferOps implements IDifferOps<Scope, ITerm, ITerm> {

        private static final ScopeImpl scopeImpl = new ScopeImpl();

        private final Collection<Scope> ownScopes;

        public AnalyticsDifferOps(Collection<Scope> ownScopes) {
            this.ownScopes = ownScopes;
        }

        @Override public boolean isMatchAllowed(Scope currentScope, Scope previousScope) {
            return currentScope.getResource().equals(previousScope.getResource());
        }

        @Override public Optional<Immutable<Scope>> matchDatums(ITerm currentDatum, ITerm previousDatum) {
            return scopeImpl.matchDatums(currentDatum, previousDatum);
        }

        @Override public Collection<Scope> getScopes(ITerm d) {
            return scopeImpl.getScopes(d);
        }

        @Override public ITerm embed(Scope scope) {
            return scopeImpl.embed(scope);
        }

        @Override public boolean ownScope(Scope scope) {
            return ownScopes.contains(scope);
        }

        @Override public boolean ownOrSharedScope(Scope currentScope) {
            throw new UnsupportedOperationException();
        }

        @Override public IFuture<Optional<Scope>> externalMatch(Scope previousScope) {
            throw new UnsupportedOperationException();
        }

    }

}
