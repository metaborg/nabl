package mb.statix.modular.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import mb.nabl2.terms.ITerm;

public class LabelCache {
    public Map<ITerm, ITerm> map = new ConcurrentHashMap<>();
    
    @SuppressWarnings("unchecked")
    public <T extends ITerm> T get(T term) {
        return (T) map.computeIfAbsent(term, t -> t);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getUnsafe(T term) {
        return (T) map.computeIfAbsent((ITerm) term, t -> t);
    }
    
    public void clear() {
        map.clear();
    }
    
    public static LabelCache fake() {
        return new FakeLabelCache();
    }
    
    private static class FakeLabelCache extends LabelCache {
        @Override
        public <T extends ITerm> T get(T term) {
            return term;
        }
        
        @Override
        public <T> T getUnsafe(T term) {
            return term;
        }
        
        @Override
        public void clear() {}
    }
}
