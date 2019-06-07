package mb.statix.taico.incremental.manager;

import java.io.Serializable;

public class IncrementalManager implements Serializable {
    private static final long serialVersionUID = 1L;
    
    protected volatile Object phase;
    protected boolean initPhase = true;
    
    @SuppressWarnings("unchecked")
    public <T> T getPhase() {
        return (T) phase;
    }
    
    public void setPhase(Object phase) {
        this.phase = phase;
    }
    
    public boolean isInitPhase() {
        return initPhase;
    }
    
    public void finishInitPhase() {
        initPhase = true;
    }

    @Override
    public String toString() {
        return "IncrementalManager [phase=" + phase + ", initPhase=" + initPhase + "]";
    }
}
