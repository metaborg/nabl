package mb.scopegraph.pepm16.esop15.lazy;

import java.io.Serializable;
import java.util.Collection;
import java.util.Optional;

import org.metaborg.util.task.ICancel;

import mb.scopegraph.pepm16.CriticalEdgeException;
import mb.scopegraph.pepm16.ILabel;
import mb.scopegraph.pepm16.IOccurrence;
import mb.scopegraph.pepm16.IScope;
import mb.scopegraph.pepm16.path.IDeclPath;
import mb.scopegraph.pepm16.path.IPath;

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