package mb.statix.taico.scopegraph;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;

import mb.nabl2.terms.ITerm;
import mb.nabl2.util.collections.IRelation3;
import mb.statix.scopegraph.terms.AScope;
import mb.statix.taico.module.IModule;
import mb.statix.taico.util.Relations;

public class DelegatingModuleScopeGraph extends ModuleScopeGraph {
    private final ModuleScopeGraph original;
    private final boolean clearScopes;
    
    public DelegatingModuleScopeGraph(ModuleScopeGraph original, boolean clearScopes) {
        super(original.id, original.getOwner(), original.getEdgeLabels(), original.getDataLabels(), original.getNoDataLabel(), original.getParentScopes());
        
        this.original = original;
        this.clearScopes = clearScopes;
        this.scopeCounter = original.scopeCounter;
    }

    @Override
    public ModuleScopeGraph createChild(IModule module, List<AScope> canExtend) {
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
    public IRelation3<AScope, ITerm, AScope> getOwnEdges() {
        return Relations.union(original.getOwnEdges(), super.getOwnEdges());
    }

    @Override
    public IRelation3<AScope, ITerm, ITerm> getOwnData() {
        return Relations.union(original.getOwnData(), super.getOwnData());
    }

    @Override
    public Set<AScope> getScopes() {
        return clearScopes ? super.getScopes() : Sets.union(original.getScopes(), super.getScopes());
    }
}
