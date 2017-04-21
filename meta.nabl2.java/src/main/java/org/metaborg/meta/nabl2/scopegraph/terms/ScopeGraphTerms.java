package org.metaborg.meta.nabl2.scopegraph.terms;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.metaborg.meta.nabl2.scopegraph.IScopeGraph;
import org.metaborg.meta.nabl2.solver.IProperties;
import org.metaborg.meta.nabl2.spoofax.analysis.AnalysisTerms;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.generic.TB;
import org.metaborg.meta.nabl2.unification.IUnifier;

import com.google.common.collect.Lists;

public final class ScopeGraphTerms {

    private static final String NO_TYPE = "NoType";
    private static final String TYPE = "Type";

    private final IScopeGraph<Scope, Label, Occurrence> scopeGraph;
    private final IProperties<Occurrence> properties;
    private final IUnifier unifier;

    private ScopeGraphTerms(IScopeGraph<Scope, Label, Occurrence> scopeGraph, IProperties<Occurrence> properties,
            IUnifier unifier) {
        this.scopeGraph = scopeGraph;
        this.properties = properties;
        this.unifier = unifier;
    }

    private ITerm build() {
        List<ITerm> scopes = scopeGraph.getAllScopes().stream().map(this::buildScope).collect(Collectors.toList());
        return TB.newAppl("ScopeGraph", (ITerm) TB.newList(scopes));
    }

    private ITerm buildScope(Scope scope) {
        List<ITerm> parts = Lists.newArrayList();

        List<ITerm> decls =
                scopeGraph.getDecls().inverse().get(scope).stream().map(this::buildDecl).collect(Collectors.toList());
        if(!decls.isEmpty()) {
            parts.add(TB.newAppl("Decls", (ITerm) TB.newList(decls)));
        }

        List<ITerm> refs =
                scopeGraph.getRefs().inverse().get(scope).stream().map(this::buildRef).collect(Collectors.toList());
        if(!refs.isEmpty()) {
            parts.add(TB.newAppl("Refs", (ITerm) TB.newList(refs)));
        }

        List<ITerm> directEdges =
                scopeGraph.getDirectEdges().get(scope).stream().map(this::buildDirectEdge).collect(Collectors.toList());
        if(!directEdges.isEmpty()) {
            parts.add(TB.newAppl("DirectEdges", (ITerm) TB.newList(directEdges)));
        }

        List<ITerm> importEdges =
                scopeGraph.getImportEdges().get(scope).stream().map(this::buildImportEdge).collect(Collectors.toList());
        if(!importEdges.isEmpty()) {
            parts.add(TB.newAppl("ImportEdges", (ITerm) TB.newList(importEdges)));
        }

        List<ITerm> exportEdges = scopeGraph.getExportEdges().inverse().get(scope).stream().map(this::buildExportEdge)
                .collect(Collectors.toList());
        if(!exportEdges.isEmpty()) {
            parts.add(TB.newAppl("AssocEdges", (ITerm) TB.newList(exportEdges)));
        }

        return TB.newAppl("Scope", scope, (ITerm) TB.newList(parts));
    }

    private ITerm buildDecl(Occurrence decl) {
        return TB.newAppl("Decl", decl, buildType(properties.getValue(decl, AnalysisTerms.TYPE_KEY)));
    }

    private ITerm buildType(Optional<ITerm> type) {
        return type.map(unifier::find).map(t -> TB.newAppl(TYPE, t)).orElseGet(() -> TB.newAppl(NO_TYPE));
    }

    private ITerm buildRef(Occurrence ref) {
        return TB.newAppl("Ref", ref);
    }

    private ITerm buildDirectEdge(Map.Entry<Label, Scope> edge) {
        return TB.newAppl("DirectEdge", edge.getKey(), edge.getValue());
    }

    private ITerm buildImportEdge(Map.Entry<Label, Occurrence> edge) {
        return TB.newAppl("ImportEdge", edge.getKey(), edge.getValue());
    }

    private ITerm buildExportEdge(Map.Entry<Label, Occurrence> edge) {
        return TB.newAppl("AssocEdge", edge.getKey(), edge.getValue());
    }

    // static interface

    public static ITerm build(IScopeGraph<Scope, Label, Occurrence> scopeGraph, IProperties<Occurrence> properties,
            IUnifier unifier) {
        return new ScopeGraphTerms(scopeGraph, properties, unifier).build();
    }

}