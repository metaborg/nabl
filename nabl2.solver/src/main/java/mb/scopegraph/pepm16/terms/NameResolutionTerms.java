package mb.scopegraph.pepm16.terms;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.List;

import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import mb.nabl2.terms.ITerm;
import mb.scopegraph.pepm16.CriticalEdgeException;
import mb.scopegraph.pepm16.INameResolution;
import mb.scopegraph.pepm16.IScopeGraph;
import mb.scopegraph.pepm16.StuckException;
import mb.scopegraph.pepm16.path.IResolutionPath;
import mb.scopegraph.pepm16.terms.path.Paths;

public final class NameResolutionTerms {

    private final IScopeGraph<Scope, Label, Occurrence> scopeGraph;
    private final INameResolution<Scope, Label, Occurrence> nameResolution;
    private final ICancel cancel;
    private final IProgress progress;

    private NameResolutionTerms(IScopeGraph<Scope, Label, Occurrence> scopeGraph,
            INameResolution<Scope, Label, Occurrence> nameResolution, ICancel cancel, IProgress progress) {
        this.scopeGraph = scopeGraph;
        this.nameResolution = nameResolution;
        this.cancel = cancel;
        this.progress = progress;
    }

    private ITerm build() throws InterruptedException {
        final List<ITerm> resolutions = Lists.newArrayList();
        for(Occurrence ref : scopeGraph.getAllRefs()) {
            resolutions.add(buildRef(ref));
        }
        return B.newAppl("NameResolution", (ITerm) B.newList(resolutions));
    }

    private ITerm buildRef(Occurrence ref) throws InterruptedException {
        List<ITerm> paths;
        try {
            paths = nameResolution.resolve(ref, cancel, progress).stream().map(this::buildPath)
                    .collect(ImmutableList.toImmutableList());
        } catch(CriticalEdgeException | StuckException e) {
            paths = ImmutableList.of();
        }
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
            INameResolution<Scope, Label, Occurrence> nameResolution, ICancel cancel, IProgress progress)
            throws InterruptedException {
        return new NameResolutionTerms(scopeGraph, nameResolution, cancel, progress).build();
    }

}