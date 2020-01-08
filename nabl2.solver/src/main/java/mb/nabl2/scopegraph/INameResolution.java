package mb.nabl2.scopegraph;

import java.util.Map;
import java.util.Set;

import mb.nabl2.scopegraph.esop.CriticalEdgeException;
import mb.nabl2.scopegraph.path.IResolutionPath;


public interface INameResolution<S extends IScope, L extends ILabel, O extends IOccurrence> {

    java.util.Set<O> getResolvedRefs();

    Set<IResolutionPath<S, L, O>> resolve(O ref) throws CriticalEdgeException;

    Set<O> decls(S scope) throws CriticalEdgeException;

    Set<O> refs(S scope) throws CriticalEdgeException;

    Set<O> visible(S scope) throws CriticalEdgeException;

    Set<O> reachable(S scope) throws CriticalEdgeException;

    Set<Map.Entry<O, Set<IResolutionPath<S, L, O>>>> resolutionEntries();

}