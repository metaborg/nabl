package mb.scopegraph.pepm16.path;

import java.util.Set;

import mb.scopegraph.pepm16.ILabel;
import mb.scopegraph.pepm16.IOccurrence;
import mb.scopegraph.pepm16.IScope;


public interface IResolutionPaths<S extends IScope, L extends ILabel, O extends IOccurrence> {

    IResolutionPaths<S, L, O> inverse();

    Set<IResolutionPath<S, L, O>> get(O occurrence);

}