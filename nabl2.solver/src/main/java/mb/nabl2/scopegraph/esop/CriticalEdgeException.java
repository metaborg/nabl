package mb.nabl2.scopegraph.esop;

import java.util.List;

import com.google.common.collect.ImmutableList;

import mb.nabl2.scopegraph.ILabel;
import mb.nabl2.scopegraph.IScope;

public class CriticalEdgeException extends Throwable {

    private static final long serialVersionUID = 1L;

    private final List<CriticalEdge> criticalEdges;

    public CriticalEdgeException(Iterable<CriticalEdge> criticalEdges) {
        super("incomplete", null, false, false);
        this.criticalEdges = ImmutableList.copyOf(criticalEdges);
        if(this.criticalEdges.isEmpty()) {
            throw new IllegalArgumentException("Critical edges cannot be empty.");
        }
    }

    public CriticalEdgeException(IScope scope, ILabel label) {
        this(ImmutableList.of(ImmutableCriticalEdge.of(scope, label)));
    }

    public List<CriticalEdge> criticalEdges() {
        return criticalEdges;
    }

    public static CriticalEdgeException of(Iterable<CriticalEdgeException> exceptions) {
        ImmutableList.Builder<CriticalEdge> incompletes = ImmutableList.builder();
        exceptions.forEach(e -> incompletes.addAll(e.criticalEdges()));
        return new CriticalEdgeException(incompletes.build());
    }

}