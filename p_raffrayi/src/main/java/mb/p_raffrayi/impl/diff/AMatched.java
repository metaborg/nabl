package mb.p_raffrayi.impl.diff;

import java.util.Set;

import org.immutables.value.Value;
import org.metaborg.util.functions.Function0;
import org.metaborg.util.functions.Function1;

import mb.scopegraph.oopsla20.diff.Edge;

@Value.Immutable
public abstract class AMatched<S, L, D> implements IScopeDiff<S, L, D> {

    @Value.Parameter public abstract S currentScope();

    @Value.Parameter public abstract Set<Edge<S, L>> addedEdges();

    @Value.Parameter public abstract Set<Edge<S, L>> matchedEdges();

    @Value.Parameter public abstract Set<Edge<S, L>> removedEdges();

    @Override public <T> T match(Function1<Matched<S, L, D>, T> onMatched, Function0<T> onRemoved) {
        return onMatched.apply((Matched<S, L, D>) this);
    }

}
