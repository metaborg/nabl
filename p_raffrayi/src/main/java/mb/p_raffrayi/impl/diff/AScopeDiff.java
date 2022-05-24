package mb.p_raffrayi.impl.diff;

import java.util.Set;

import org.immutables.value.Value;

import mb.scopegraph.oopsla20.diff.Edge;

@Value.Immutable
public abstract class AScopeDiff<S, L, D> {

    @Value.Parameter public abstract Set<Edge<S, L>> addedEdges();

    @Value.Parameter public abstract Set<Edge<S, L>> matchedEdges();

    @Value.Parameter public abstract Set<Edge<S, L>> removedEdges();

}
