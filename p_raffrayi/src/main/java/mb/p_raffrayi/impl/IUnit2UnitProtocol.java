package mb.p_raffrayi.impl;

import java.util.Set;

import org.metaborg.util.future.IFuture;

import mb.p_raffrayi.actors.IActorRef;
import mb.p_raffrayi.nameresolution.DataLeq;
import mb.p_raffrayi.nameresolution.DataWf;
import mb.scopegraph.ecoop21.LabelOrder;
import mb.scopegraph.ecoop21.LabelWf;
import mb.scopegraph.oopsla20.reference.Env;
import mb.scopegraph.oopsla20.terms.newPath.ScopePath;

/**
 * Protocol accepted by clients, from other clients
 */
public interface IUnit2UnitProtocol<S, L, D, R> {

    void _initShare(S scope, S childRep);

    IFuture<Env<S, L, D>> _query(S scope, ScopePath<S, L> path, LabelWf<L> labelWF, DataWf<S, L, D> dataWF,
            LabelOrder<L> labelOrder, DataLeq<S, L, D> dataEquiv);


    void _deadlockQuery(IActorRef<? extends IUnit<S, L, D, ?>> i, int m);

    void _deadlockReply(IActorRef<? extends IUnit<S, L, D, ?>> i, int m, Set<IActorRef<? extends IUnit<S, L, D, ?>>> r);

    void _deadlocked(Set<IActorRef<? extends IUnit<S, L, D, ?>>> nodes);

}