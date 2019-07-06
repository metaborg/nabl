package mb.statix.taico.scopegraph;

import java.io.NotSerializableException;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;

import mb.nabl2.terms.ITerm;
import mb.nabl2.util.collections.IRelation3;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.taico.module.IModule;
import mb.statix.taico.util.Relations;

public class DelegatingModuleScopeGraph extends ModuleScopeGraph {
    private static final long serialVersionUID = 1L;
    
    private final ModuleScopeGraph original;
    private final boolean clearScopes;
    
    public DelegatingModuleScopeGraph(ModuleScopeGraph original, boolean clearScopes) {
        super(original.id, original.getOwner(), original.getEdgeLabels(), original.getDataLabels(), original.getNoDataLabel(), original.getParentScopes());
        
        this.original = original;
        this.clearScopes = clearScopes;
        this.scopeCounter = original.scopeCounter;
    }

    @Override
    public ModuleScopeGraph createChild(IModule module, List<Scope> canExtend) {
        throw new UnsupportedOperationException("Creating children in delegate scope graphs (during entails) is currently not supported.");
    }

    @Override
    public ModuleScopeGraph addChild(IModule child) {
        throw new UnsupportedOperationException("Adding children in delegate scope graphs (during entails) is currently not supported.");
    }

    @Override
    public boolean removeChild(IModule child) {
        throw new UnsupportedOperationException("Removing children in delegate scope graphs (during entails) is currently not supported.");
    }

    @Override
    public void purgeChildren() {
        throw new UnsupportedOperationException("Purging children on delegate scope graphs is not supported.");
    }

    @Override
    public IRelation3<Scope, ITerm, Scope> getOwnEdges() {
        return Relations.union(original.getOwnEdges(), super.getOwnEdges());
    }

    @Override
    public IRelation3<Scope, ITerm, ITerm> getOwnData() {
        return Relations.union(original.getOwnData(), super.getOwnData());
    }

    @Override
    public Set<Scope> getScopes() {
        return clearScopes ? super.getScopes() : Sets.union(original.getScopes(), super.getScopes());
    }
    
    //---------------------------------------------------------------------------------------------
    
    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
        throw new NotSerializableException("It is not possible to deserialize delegates of scope graphs.");
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
        throw new NotSerializableException("It is not possible to deserialize delegates of scope graphs.");
    }
}
