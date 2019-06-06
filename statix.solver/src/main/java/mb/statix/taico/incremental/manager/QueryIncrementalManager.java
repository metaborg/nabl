package mb.statix.taico.incremental.manager;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class QueryIncrementalManager extends IncrementalManager {
    private Set<String> allowedAccess = ConcurrentHashMap.newKeySet();
    
    @Override
    public void setPhase(Object phase) {
        if (!(phase instanceof QueryPhase)) throw new IllegalArgumentException("Can only switch the phase to a query phase.");
        
        QueryPhase oldPhase = getPhase();
        super.setPhase(phase);
        updatePhase(oldPhase, (QueryPhase) phase);
    }
    
    protected void updatePhase(QueryPhase oldPhase, QueryPhase newPhase) {
        //TODO IMPORTANT
    }
    
    public boolean isAllowedAccess(String module) {
        return allowedAccess.contains(module);
    }
    
    public void allowAccess(String module) {
        allowedAccess.add(module);
    }
    
    public static enum QueryPhase {
        Dirty,
        
        
        /** The final phase where we just do normal solving. */
        Final
        
    }
}
