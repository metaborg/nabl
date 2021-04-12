package mb.nabl2.scopegraph.path;

import org.metaborg.util.collection.ConsList;

import io.usethesource.capsule.Set;
import mb.nabl2.scopegraph.ILabel;
import mb.nabl2.scopegraph.IOccurrence;
import mb.nabl2.scopegraph.IScope;

public interface IPath<S extends IScope, L extends ILabel, O extends IOccurrence> {

    Set.Immutable<O> getImports();

    Set.Immutable<S> getScopes();

    ConsList<L> getLabels();

    Iterable<IResolutionPath<S, L, O>> getImportPaths();

}