package mb.nabl2.scopegraph.path;

import mb.nabl2.scopegraph.ILabel;
import mb.nabl2.scopegraph.IOccurrence;
import mb.nabl2.scopegraph.IScope;

public interface IOpenPath<S extends IScope, L extends ILabel, O extends IOccurrence> extends IPath<S, L, O> {

    IScopePath<S, L, O> getPath();

    L getLabel();

}