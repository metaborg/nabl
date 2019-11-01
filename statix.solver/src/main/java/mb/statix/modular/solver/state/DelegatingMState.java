package mb.statix.modular.solver.state;

import java.io.NotSerializableException;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.Sets;

import mb.nabl2.terms.ITermVar;

public class DelegatingMState extends MState {
    private static final long serialVersionUID = 1L;
    
    private final Set<ITermVar> originalVars;
    private final Set<ITermVar> varsUnion;
    
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
        this.varsUnion = Sets.union(originalVars, super.vars());
    }

    @Override
    public Set<ITermVar> vars() {
        return varsUnion;
    }
    
    //TODO Are the copy semantics correct?
    
    //---------------------------------------------------------------------------------------------
    
    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
        throw new NotSerializableException("It is not possible to deserialize delegates of states.");
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
        throw new NotSerializableException("It is not possible to deserialize delegates of states.");
    }
}
