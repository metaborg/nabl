package mb.scopegraph.pepm16.path;

import org.metaborg.util.collection.ConsList;

import io.usethesource.capsule.Set;
import mb.scopegraph.pepm16.ILabel;
import mb.scopegraph.pepm16.IOccurrence;
import mb.scopegraph.pepm16.IScope;

public interface IPath<S extends IScope, L extends ILabel, O extends IOccurrence> {

    Set.Immutable<O> getImports();

    Set.Immutable<S> getScopes();

    ConsList<L> getLabels();

    Iterable<IResolutionPath<S, L, O>> getImportPaths();

}