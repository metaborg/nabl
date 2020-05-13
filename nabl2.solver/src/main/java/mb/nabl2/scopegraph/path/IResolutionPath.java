package mb.nabl2.scopegraph.path;

import mb.nabl2.scopegraph.ILabel;
import mb.nabl2.scopegraph.IOccurrence;
import mb.nabl2.scopegraph.IScope;

public interface IResolutionPath<S extends IScope, L extends ILabel, O extends IOccurrence> extends IDeclPath<S, L, O> {

    O getReference();

}