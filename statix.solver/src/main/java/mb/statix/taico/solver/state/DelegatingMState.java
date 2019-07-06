package mb.statix.taico.solver.state;

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.Sets;

import mb.nabl2.terms.ITermVar;

public class DelegatingMState extends MState {
    private final Set<ITermVar> originalVars;
    
    /**
     * @param original
     *      the original state
     * @param vars
     *      the variables to keep in the delegate (variables are intersected with the original)
     * @param clearScopes
     *      if scopes should be cleared from the delegate
     */
    public DelegatingMState(MState original, Set<ITermVar> vars, boolean clearScopes) {
        super(original, new HashSet<>(), original.scopeGraph().delegatingGraph(clearScopes));
        this.originalVars = Sets.intersection(original.vars(), vars);
    }

    @Override
    public Set<ITermVar> vars() {
        return Sets.union(originalVars, super.vars());
    }
    
    //TODO Are the copy semantics correct?
}
