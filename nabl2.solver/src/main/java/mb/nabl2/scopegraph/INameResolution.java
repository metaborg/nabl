package mb.nabl2.scopegraph;

import java.util.Collection;
import java.util.Map;

import mb.nabl2.scopegraph.esop.CriticalEdgeException;
import mb.nabl2.scopegraph.path.IResolutionPath;


public interface INameResolution<S extends IScope, L extends ILabel, O extends IOccurrence> {

    java.util.Set<O> getResolvedRefs();

    Collection<IResolutionPath<S, L, O>> resolve(O ref) throws CriticalEdgeException;

    Collection<O> decls(S scope) throws CriticalEdgeException;

    Collection<O> refs(S scope) throws CriticalEdgeException;

    Collection<O> visible(S scope) throws CriticalEdgeException;

    Collection<O> reachable(S scope) throws CriticalEdgeException;

    Collection<Map.Entry<O, Collection<IResolutionPath<S, L, O>>>> resolutionEntries();

}