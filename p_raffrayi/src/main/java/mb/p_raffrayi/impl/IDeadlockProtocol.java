package mb.p_raffrayi.impl;

import java.util.Set;

import org.metaborg.util.future.IFuture;

import mb.scopegraph.patching.IPatchCollection;

public interface IDeadlockProtocol<S, L, D> {

    // Deadlock detection

    void _deadlockQuery(IProcess<S, L, D> i, int m, IProcess<S, L, D> k);

    void _deadlockReply(IProcess<S, L, D> i, int m, Set<IProcess<S, L, D>> r);

    void _deadlocked(Set<IProcess<S, L, D>> nodes);

    // Deadlock analysis

    IFuture<StateSummary<S, L, D>> _state();

    // Deadlock resolution

    void _release(IPatchCollection.Immutable<S> patches);

    void _restart();

}
