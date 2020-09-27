package mb.nabl2.scopegraph;

import java.util.Collection;
import java.util.Map;

import mb.nabl2.scopegraph.esop.CriticalEdge;
import mb.nabl2.scopegraph.path.IResolutionPath;


public interface INameResolution<S extends IScope, L extends ILabel, O extends IOccurrence> {

    java.util.Set<O> getResolvedRefs();

    Collection<IResolutionPath<S, L, O>> resolve(O ref)
            throws CriticalEdgeException, StuckException, InterruptedException;

    Collection<O> decls(S scope) throws CriticalEdgeException, StuckException;

    Collection<O> refs(S scope) throws CriticalEdgeException, StuckException;

    Collection<O> visible(S scope) throws CriticalEdgeException, StuckException, InterruptedException;

    Collection<O> reachable(S scope) throws CriticalEdgeException, StuckException, InterruptedException;

    Collection<? extends Map.Entry<O, ? extends Collection<IResolutionPath<S, L, O>>>> resolutionEntries();

    default void update(Iterable<CriticalEdge> criticalEdges) throws InterruptedException {
    }

}