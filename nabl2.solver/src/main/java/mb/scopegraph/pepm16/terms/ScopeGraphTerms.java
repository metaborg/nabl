package mb.scopegraph.pepm16.terms;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.metaborg.util.collection.ImList;
import org.metaborg.util.tuple.Tuple2;

import mb.nabl2.constraints.namebinding.DeclProperties;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.u.IUnifier;
import mb.nabl2.util.collections.IProperties;
import mb.scopegraph.pepm16.esop15.IEsopScopeGraph;

public final class ScopeGraphTerms {

    private static final String NO_TYPE = "NoType";
    private static final String TYPE = "Type";

    private final IEsopScopeGraph<Scope, Label, Occurrence, ? extends ITerm> scopeGraph;
    private final IProperties<Occurrence, ITerm, ITerm> properties;
    private final IUnifier unifier;

    private ScopeGraphTerms(IEsopScopeGraph<Scope, Label, Occurrence, ? extends ITerm> scopeGraph,
            IProperties<Occurrence, ITerm, ITerm> properties, IUnifier unifier) {
        this.scopeGraph = scopeGraph;
        this.properties = properties;
        this.unifier = unifier;
    }

    private ITerm build() {
        List<ITerm> scopes =
                scopeGraph.getAllScopes().stream().map(this::buildScope).collect(ImList.Immutable.toImmutableList());
        return B.newAppl("ScopeGraph", (ITerm) B.newList(scopes));
    }

    private ITerm buildScope(Scope scope) {
        List<ITerm> parts = new ArrayList<>();

        List<ITerm> decls = scopeGraph.getDecls().inverse().get(scope).stream().map(this::buildDecl)
                .collect(ImList.Immutable.toImmutableList());
        if(!decls.isEmpty()) {
            parts.add(B.newAppl("Decls", (ITerm) B.newList(decls)));
        }

        List<ITerm> refs = scopeGraph.getRefs().inverse().get(scope).stream().map(this::buildRef)
                .collect(ImList.Immutable.toImmutableList());
        if(!refs.isEmpty()) {
            parts.add(B.newAppl("Refs", (ITerm) B.newList(refs)));
        }

        List<ITerm> directEdges = Stream
                .concat(scopeGraph.getDirectEdges().get(scope).stream(),
                        scopeGraph.incompleteDirectEdges().stream().filter(e -> e.getKey()._1().equals(scope))
                                .map(e -> Tuple2.of(e.getKey()._2(), e.getValue())))
                .map(this::buildDirectEdge).collect(Collectors.toList());
        if(!directEdges.isEmpty()) {
            parts.add(B.newAppl("DirectEdges", (ITerm) B.newList(directEdges)));
        }

        List<ITerm> importEdges = Stream
                .concat(scopeGraph.getImportEdges().get(scope).stream(),
                        scopeGraph.incompleteImportEdges().stream().filter(e -> e.getKey()._1().equals(scope))
                                .map(e -> Tuple2.of(e.getKey()._2(), e.getValue())))
                .map(this::buildImportEdge).collect(Collectors.toList());
        if(!importEdges.isEmpty()) {
            parts.add(B.newAppl("ImportEdges", (ITerm) B.newList(importEdges)));
        }

        List<ITerm> exportEdges = scopeGraph.getExportEdges().inverse().get(scope).stream().map(this::buildExportEdge)
                .collect(Collectors.toList());
        if(!exportEdges.isEmpty()) {
            parts.add(B.newAppl("AssocEdges", (ITerm) B.newList(exportEdges)));
        }

        return B.newAppl("Scope", scope, (ITerm) B.newList(parts));
    }

    private ITerm buildDecl(Occurrence decl) {
        return B.newAppl("Decl", decl, buildType(properties.getValue(decl, DeclProperties.TYPE_KEY)));
    }

    private ITerm buildType(Optional<ITerm> type) {
        return type.map(unifier::findRecursive).map(t -> B.newAppl(TYPE, t)).orElseGet(() -> B.newAppl(NO_TYPE));
    }

    private ITerm buildRef(Occurrence ref) {
        return B.newAppl("Ref", ref);
    }

    private ITerm buildDirectEdge(Map.Entry<Label, ? extends ITerm> edge) {
        return B.newAppl("DirectEdge", edge.getKey(), edge.getValue());
    }

    private ITerm buildImportEdge(Map.Entry<Label, ? extends ITerm> edge) {
        return B.newAppl("ImportEdge", edge.getKey(), edge.getValue());
    }

    private ITerm buildExportEdge(Map.Entry<Label, Occurrence> edge) {
        return B.newAppl("AssocEdge", edge.getKey(), edge.getValue());
    }

    // static interface

    public static ITerm build(IEsopScopeGraph<Scope, Label, Occurrence, ? extends ITerm> scopeGraph,
            IProperties<Occurrence, ITerm, ITerm> properties, IUnifier unifier) {
        return new ScopeGraphTerms(scopeGraph, properties, unifier).build();
    }

}
