package mb.p_raffrayi.impl;

import java.util.Set;

import org.metaborg.util.future.IFuture;

import mb.scopegraph.oopsla20.diff.BiMap;

public interface IDeadlockProtocol<S, L, D> {

    void _deadlockQuery(IProcess<S, L, D> i, int m, IProcess<S, L, D> k);

    void _deadlockReply(IProcess<S, L, D> i, int m, Set<IProcess<S, L, D>> r);

    void _deadlocked(Set<IProcess<S, L, D>> nodes);

    IFuture<ReleaseOrRestart<S>> _requireRestart();

    void _release(BiMap.Immutable<S> patches);

    void _restart();

}
