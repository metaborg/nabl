package mb.nabl2.scopegraph.path;

import java.util.Set;

import mb.nabl2.scopegraph.ILabel;
import mb.nabl2.scopegraph.IOccurrence;
import mb.nabl2.scopegraph.IScope;


public interface IResolutionPaths<S extends IScope, L extends ILabel, O extends IOccurrence> {

    IResolutionPaths<S, L, O> inverse();

    Set<IResolutionPath<S, L, O>> get(O occurrence);

}