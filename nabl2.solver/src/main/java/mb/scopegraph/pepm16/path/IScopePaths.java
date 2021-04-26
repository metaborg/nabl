package mb.scopegraph.pepm16.path;

import java.util.Set;

import mb.scopegraph.pepm16.ILabel;
import mb.scopegraph.pepm16.IOccurrence;
import mb.scopegraph.pepm16.IScope;


public interface IScopePaths<S extends IScope, L extends ILabel, O extends IOccurrence> {

    IScopePaths<S, L, O> inverse();

    Set<IScopePath<S, L, O>> get(S scope);

}