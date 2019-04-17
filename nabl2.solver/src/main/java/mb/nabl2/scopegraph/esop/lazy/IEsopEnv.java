package mb.nabl2.scopegraph.esop.lazy;

import java.io.Serializable;
import java.util.Optional;

import io.usethesource.capsule.Set;
import mb.nabl2.scopegraph.ILabel;
import mb.nabl2.scopegraph.IOccurrence;
import mb.nabl2.scopegraph.IScope;
import mb.nabl2.scopegraph.path.IDeclPath;
import mb.nabl2.scopegraph.path.IPath;
import mb.nabl2.util.Tuple2;

public interface IEsopEnv<S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>>
        extends Serializable {

    Optional<Tuple2<Set.Immutable<P>, Set.Immutable<String>>> get();

    interface Filter<S extends IScope, L extends ILabel, O extends IOccurrence, P extends IPath<S, L, O>>
            extends Serializable {

        Optional<P> test(IDeclPath<S, L, O> path);

        Object matchToken(P p);

        boolean shortCircuit();

    }

}