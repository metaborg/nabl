package org.metaborg.meta.nabl2.scopegraph.terms;

import static org.metaborg.meta.nabl2.terms.build.TermBuild.B;

import java.util.List;
import java.util.stream.Collectors;

import org.metaborg.meta.nabl2.scopegraph.INameResolution;
import org.metaborg.meta.nabl2.scopegraph.IScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPath;
import org.metaborg.meta.nabl2.scopegraph.terms.path.Paths;
import org.metaborg.meta.nabl2.terms.ITerm;

import io.usethesource.capsule.Set;

public final class NameResolutionTerms {

    private final IScopeGraph<Scope, Label, Occurrence> scopeGraph;
    private final INameResolution<Scope, Label, Occurrence> nameResolution;

    private NameResolutionTerms(IScopeGraph<Scope, Label, Occurrence> scopeGraph,
            INameResolution<Scope, Label, Occurrence> nameResolution) {
        this.scopeGraph = scopeGraph;
        this.nameResolution = nameResolution;
    }

    private ITerm build() {
        final List<ITerm> resolutions =
                scopeGraph.getAllRefs().stream().map(this::buildRef).collect(Collectors.toList());
        return B.newAppl("NameResolution", (ITerm) B.newList(resolutions));
    }

    private ITerm buildRef(Occurrence ref) {
        final List<ITerm> paths = nameResolution.resolve(ref).orElseGet(() -> Set.Immutable.of()).stream()
                .map(this::buildPath).collect(Collectors.toList());
        final ITerm result;
        if(paths.isEmpty()) {
            result = B.newAppl("NoResolution");
        } else {
            result = B.newAppl("Resolution", (ITerm) B.newList(paths));
        }
        return B.newTuple(ref, result);
    }

    private ITerm buildPath(IResolutionPath<Scope, Label, Occurrence> path) {
        return B.newTuple(path.getDeclaration(), Paths.toTerm(path));
    }

    public static ITerm build(IScopeGraph<Scope, Label, Occurrence> scopeGraph,
            INameResolution<Scope, Label, Occurrence> nameResolution) {
        return new NameResolutionTerms(scopeGraph, nameResolution).build();
    }

}