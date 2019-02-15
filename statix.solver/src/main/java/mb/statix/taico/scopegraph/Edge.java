package mb.statix.taico.scopegraph;

import mb.statix.taico.module.IModule;
import mb.statix.taico.util.IOwnable;

public class Edge<S extends IOwnable<S, ?, ?>, L, T> implements IEdge<S, L, T> {
    private final IModule<S, ?, ?> owner;
    private final S source;
    private final T target;
    private final L label;
    
    public Edge(IModule<S, ?, ?> owner, S source, L label, T target) {
        this.owner = owner;
        this.source = source;
        this.label = label;
        this.target = target;
    }
    
    public IModule<S, ?, ?> getOwner() {
        return owner;
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
        result = prime * result + owner.hashCode();
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
        if (!owner.equals(other.owner))   return false;
        return true;
    }
}
