package org.metaborg.meta.nabl2.scopegraph.terms;

import java.util.List;
import java.util.stream.Collectors;

import org.metaborg.meta.nabl2.scopegraph.INameResolution;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPath;
import org.metaborg.meta.nabl2.scopegraph.terms.path.Paths;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.generic.TB;

public final class NameResolutionTerms {

    private final INameResolution<Scope, Label, Occurrence> nameResolution;

    private NameResolutionTerms(INameResolution<Scope, Label, Occurrence> nameResolution) {
        this.nameResolution = nameResolution;
    }

    private ITerm build() {
        final List<ITerm> resolutions =
                nameResolution.getAllRefs().stream().map(this::buildRef).collect(Collectors.toList());
        return TB.newAppl("NameResolution", (ITerm) TB.newList(resolutions));
    }

    private ITerm buildRef(Occurrence ref) {
        final List<ITerm> paths =
                nameResolution.resolve(ref).stream().map(this::buildPath).collect(Collectors.toList());
        final ITerm result;
        if(paths.isEmpty()) {
            result = TB.newAppl("NoResolution");
        } else {
            result = TB.newAppl("Resolution", (ITerm) TB.newList(paths));
        }
        return TB.newTuple(ref, result);
    }

    private ITerm buildPath(IResolutionPath<Scope, Label, Occurrence> path) {
        return TB.newTuple(path.getDeclaration(), Paths.toTerm(path));
    }

    public static ITerm build(INameResolution<Scope, Label, Occurrence> nameResolution) {
        return new NameResolutionTerms(nameResolution).build();
    }

}