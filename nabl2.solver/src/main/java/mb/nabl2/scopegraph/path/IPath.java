package mb.nabl2.scopegraph.path;

import io.usethesource.capsule.Set;
import mb.nabl2.scopegraph.ILabel;
import mb.nabl2.scopegraph.IOccurrence;
import mb.nabl2.scopegraph.IScope;
import mb.nabl2.util.collections.ConsList;

public interface IPath<S extends IScope, L extends ILabel, O extends IOccurrence> {

    Set.Immutable<O> getImports();

    Set.Immutable<S> getScopes();

    ConsList<L> getLabels();

    Iterable<IResolutionPath<S, L, O>> getImportPaths();

}