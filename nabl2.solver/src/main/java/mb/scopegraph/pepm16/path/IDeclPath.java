package mb.scopegraph.pepm16.path;

import mb.scopegraph.pepm16.ILabel;
import mb.scopegraph.pepm16.IOccurrence;
import mb.scopegraph.pepm16.IScope;

public interface IDeclPath<S extends IScope, L extends ILabel, O extends IOccurrence> extends IPath<S, L, O> {

    IScopePath<S, L, O> getPath();

    O getDeclaration();

}