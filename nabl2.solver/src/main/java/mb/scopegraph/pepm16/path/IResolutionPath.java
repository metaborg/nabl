package mb.scopegraph.pepm16.path;

import mb.scopegraph.pepm16.ILabel;
import mb.scopegraph.pepm16.IOccurrence;
import mb.scopegraph.pepm16.IScope;

public interface IResolutionPath<S extends IScope, L extends ILabel, O extends IOccurrence> extends IDeclPath<S, L, O> {

    O getReference();

}