package mb.nabl2.scopegraph.terms;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.List;

import com.google.common.collect.ImmutableList;

import io.usethesource.capsule.Set;
import mb.nabl2.scopegraph.INameResolution;
import mb.nabl2.scopegraph.IScopeGraph;
import mb.nabl2.scopegraph.path.IResolutionPath;
import mb.nabl2.scopegraph.terms.path.Paths;
import mb.nabl2.terms.ITerm;

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
                scopeGraph.getAllRefs().stream().map(this::buildRef).collect(ImmutableList.toImmutableList());
        return B.newAppl("NameResolution", (ITerm) B.newList(resolutions));
    }

    private ITerm buildRef(Occurrence ref) {
        final List<ITerm> paths = nameResolution.resolve(ref).orElseGet(() -> Set.Immutable.of()).stream()
                .map(this::buildPath).collect(ImmutableList.toImmutableList());
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