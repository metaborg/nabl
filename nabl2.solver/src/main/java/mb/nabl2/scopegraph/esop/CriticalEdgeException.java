package mb.nabl2.scopegraph.esop;

import java.util.List;

import com.google.common.collect.ImmutableList;

import mb.nabl2.scopegraph.ILabel;
import mb.nabl2.scopegraph.IScope;

public class CriticalEdgeException extends Throwable {

    private static final long serialVersionUID = 1L;

    private final List<CriticalEdge> incompletes;

    public CriticalEdgeException(Iterable<CriticalEdge> incompletes) {
        super("incomplete", null, false, false);
        this.incompletes = ImmutableList.copyOf(incompletes);
    }

    public CriticalEdgeException(IScope scope, ILabel label) {
        this(ImmutableList.of(ImmutableCriticalEdge.of(scope, label)));
    }

    public List<CriticalEdge> incompletes() {
        return incompletes;
    }

    public static CriticalEdgeException of(Iterable<CriticalEdgeException> exceptions) {
        ImmutableList.Builder<CriticalEdge> incompletes = ImmutableList.builder();
        exceptions.forEach(e -> incompletes.addAll(e.incompletes()));
        return new CriticalEdgeException(incompletes.build());
    }

}