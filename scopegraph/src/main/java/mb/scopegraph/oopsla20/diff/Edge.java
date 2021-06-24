package mb.scopegraph.oopsla20.diff;

import java.io.Serializable;
import java.util.Objects;

public class Edge<S, L> implements Serializable {

    private static final long serialVersionUID = 1L;

    public final S source;
    public final L label;
    public final S target;

    public Edge(S source, L label, S target) {
        this.source = source;
        this.label = label;
        this.target = target;
    }

    @Override public String toString() {
        return source + " -" + label + "-> " + target;
    }

    @Override public int hashCode() {
    	return Objects.hashCode(source);
    }

    @Override public boolean equals(Object obj) {
    	if (this == obj) {
    		return true;
    	} else if (obj == null || this.getClass() != obj.getClass()) {
    		return false;
    	}

    	@SuppressWarnings("unchecked")
		Edge<S, L> other = (Edge<S, L>) obj;

    	return Objects.equals(source, other.source)
    		&& Objects.equals(label, other.label)
    		&& Objects.equals(target, other.target);
    }

}