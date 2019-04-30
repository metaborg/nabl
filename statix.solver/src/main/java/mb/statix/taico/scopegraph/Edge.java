package mb.statix.taico.scopegraph;

public class Edge<S, L, T> implements IEdge<S, L, T> {
    private final S source;
    private final T target;
    private final L label;
    
    public Edge(S source, L label, T target) {
        this.source = source;
        this.label = label;
        this.target = target;
    }
    
    public S getSource() {
        return source;
    }

    public T getTarget() {
        return target;
    }
    
    public L getLabel() {
        return label;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + label.hashCode();
        result = prime * result + source.hashCode();
        result = prime * result + target.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        
        @SuppressWarnings("rawtypes")
        Edge other = (Edge) obj;
        
        if (!label.equals(other.label))   return false;
        if (!source.equals(other.source)) return false;
        if (!target.equals(other.target)) return false;
        return true;
    }
    
    @Override
    public String toString() {
        return "Edge<" + source + " -" + label + "-> " + target + ">";
    }
}
