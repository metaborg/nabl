package mb.scopegraph.pepm16;

import java.util.List;

import org.metaborg.util.collection.ImList;

import mb.scopegraph.pepm16.esop15.CriticalEdge;

public class CriticalEdgeException extends Throwable {

    private static final long serialVersionUID = 1L;

    private final List<CriticalEdge> criticalEdges;

    public CriticalEdgeException(Iterable<CriticalEdge> criticalEdges) {
        super("incomplete", null, false, false);
        this.criticalEdges = ImList.Immutable.copyOf(criticalEdges);
        if(this.criticalEdges.isEmpty()) {
            throw new IllegalArgumentException("Critical edges cannot be empty.");
        }
    }

    public CriticalEdgeException(IScope scope, ILabel label) {
        this(ImList.Immutable.of(CriticalEdge.of(scope, label)));
    }

    public List<CriticalEdge> criticalEdges() {
        return criticalEdges;
    }

    public static CriticalEdgeException of(Iterable<CriticalEdgeException> exceptions) {
        ImList.Transient<CriticalEdge> incompletes = ImList.Transient.of();
        exceptions.forEach(e -> incompletes.addAll(e.criticalEdges()));
        return new CriticalEdgeException(incompletes.freeze());
    }

    @Override public String getMessage() {
        final StringBuilder sb = new StringBuilder();
        sb.append("incomplete:");
        for(CriticalEdge ce : criticalEdges) {
            sb.append(" * ").append(ce);
        }
        return sb.toString();
    }

}