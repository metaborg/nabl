package mb.nabl2.scopegraph.esop.lazy;

import java.io.Serializable;
import java.util.Collection;
import java.util.Optional;

import org.metaborg.util.task.ICancel;

import mb.nabl2.scopegraph.CriticalEdgeException;
import mb.nabl2.scopegraph.ILabel;
import mb.nabl2.scopegraph.IOccurrence;
import mb.nabl2.scopegraph.IScope;
import mb.nabl2.scopegraph.path.IDeclPath;
import mb.nabl2.scopegraph.path.IPath;

public interface IEsopEnv<S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>>
        extends Serializable {

    Collection<P> get(ICancel cancel) throws CriticalEdgeException, InterruptedException;

    interface Filter<S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>>
            extends Serializable {

        Optional<P> test(IDeclPath<S, L, O> path);

        Object matchToken(P p);

        boolean shortCircuit();

    }

}