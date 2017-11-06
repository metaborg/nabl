package org.metaborg.meta.nabl2.scopegraph;

import java.util.Map.Entry;
import java.util.Set;

import org.metaborg.meta.nabl2.scopegraph.esop.IEsopScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.esop.reference.EsopScopeGraph;

import com.google.common.collect.Sets;

public final class ScopeGraphSummarizer<S extends IScope, L extends ILabel, O extends IOccurrence, V> {

    private final IEsopScopeGraph<S, L, O, V> scopeGraph;
    private final Set<S> visitedScopes = Sets.newHashSet();
    private final Set<O> visitedDecls = Sets.newHashSet();
    private final Set<O> visitedRefs = Sets.newHashSet();

    public ScopeGraphSummarizer(IEsopScopeGraph<S, L, O, V> scopeGraph) {
        this.scopeGraph = scopeGraph;
    }

    public IEsopScopeGraph.Immutable<S, L, O, V> summarize(final Iterable<? extends S> scopes,
            final Iterable<? extends O> decls) {
        IEsopScopeGraph.Transient<S, L, O, V> summaryGraph = IEsopScopeGraph.builder();
        for(S scope : scopes) {
            summarizeScope(scope, summaryGraph);
        }
        for(O decl : decls) {
            summarizeDecl(decl, summaryGraph);
        }
        return summaryGraph.freeze();

    }

    private void summarizeScope(final S scope, IEsopScopeGraph.Transient<S, L, O, V> summaryGraph) {
        if(!visitedScopes.contains(scope)) {
            visitedScopes.add(scope);
            for(O decl : scopeGraph.getDecls().inverse().get(scope)) {
                summaryGraph.addDecl(scope, decl);
                summarizeDecl(decl, summaryGraph);
            }
            for(Entry<L, S> next : scopeGraph.getDirectEdges().get(scope)) {
                summaryGraph.addDirectEdge(scope, next.getKey(), next.getValue());
                summarizeScope(next.getValue(), summaryGraph);
            }
            for(Entry<L, V> next : scopeGraph.incompleteDirectEdges().get(scope)) {
                summaryGraph.addIncompleteDirectEdge(scope, next.getKey(), next.getValue());
            }
            for(Entry<L, O> next : scopeGraph.getImportEdges().get(scope)) {
                summaryGraph.addImportEdge(scope, next.getKey(), next.getValue());
                summarizeRef(next.getValue(), summaryGraph);
            }
            for(Entry<L, V> next : scopeGraph.incompleteImportEdges().get(scope)) {
                summaryGraph.addIncompleteImportEdge(scope, next.getKey(), next.getValue());
            }
        }
    }

    private void summarizeDecl(final O decl, IEsopScopeGraph.Transient<S, L, O, V> summaryGraph) {
        if(!visitedDecls.contains(decl)) {
            visitedDecls.add(decl);
            for(Entry<L, S> next : scopeGraph.getExportEdges().get(decl)) {
                summaryGraph.addExportEdge(decl, next.getKey(), next.getValue());
                summarizeScope(next.getValue(), summaryGraph);
            }
        }
    }

    private void summarizeRef(final O ref, IEsopScopeGraph.Transient<S, L, O, V> summaryGraph) {
        if(!visitedRefs.contains(ref)) {
            visitedRefs.add(ref);
            scopeGraph.getRefs().get(ref).ifPresent(next -> {
                summaryGraph.addRef(ref, next);
                summarizeScope(next, summaryGraph);
            });
        }
    }

}