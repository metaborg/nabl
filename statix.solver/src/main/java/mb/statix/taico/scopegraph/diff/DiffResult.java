package mb.statix.taico.scopegraph.diff;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DiffResult<S extends D, L, D> {
    private Map<String, ScopeGraphDiff<S, L, D>> diffs = Collections.synchronizedMap(new HashMap<>());
    
    public Map<String, ScopeGraphDiff<S, L, D>> getDiffs() {
        return diffs;
    }

    public void addDiff(String module, ScopeGraphDiff<S, L, D> diff) {
        diffs.put(module, diff);
    }
}
