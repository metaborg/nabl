package org.metaborg.meta.nabl2.scopegraph.esop.persistent;

import static org.metaborg.meta.nabl2.scopegraph.esop.persistent.IBiSimulation.biSimulate;

import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopScopeGraph;
import org.metaborg.meta.nabl2.util.collections.IFunction;
import org.metaborg.meta.nabl2.util.collections.IRelation3;

import io.usethesource.capsule.Set;

public class BiSimulationScopeGraph<S extends IScope, L extends ILabel, O extends IOccurrence, V>
        implements IEsopScopeGraph.Immutable<S, L, O, V>, IBiSimulation, java.io.Serializable {
    
    private static final long serialVersionUID = 42L;

    private final IEsopScopeGraph<S, L, O, V> one;
    private final IEsopScopeGraph<S, L, O, V> two;

    public BiSimulationScopeGraph(final IEsopScopeGraph.Immutable<S, L, O, V> one, final IEsopScopeGraph.Immutable<S, L, O, V> two) {
        this.one = one;
        this.two = two;
    }   
    
    @Override
    public Set.Immutable<S> getAllScopes() {
        return (Set.Immutable<S>) biSimulate(one::getAllScopes, two::getAllScopes);
    }

    @Override
    public Set.Immutable<O> getAllDecls() {
        return (Set.Immutable<O>) biSimulate(one::getAllDecls, two::getAllDecls);
    }

    @Override
    public Set.Immutable<O> getAllRefs() {
        return (Set.Immutable<O>) biSimulate(one::getAllRefs, two::getAllRefs);
    }

    @Override
    public IFunction.Immutable<O, S> getDecls() {
        return (IFunction.Immutable<O, S>) biSimulate(one::getDecls, two::getDecls);
    }

    @Override
    public IFunction.Immutable<O, S> getRefs() {
        return (IFunction.Immutable<O, S>) biSimulate(one::getRefs, two::getRefs);
    }

    @Override
    public IRelation3.Immutable<S, L, S> getDirectEdges() {
        return (IRelation3.Immutable<S, L, S>) biSimulate(one::getDirectEdges, two::getDirectEdges);
    }

    @Override
    public IRelation3.Immutable<O, L, S> getExportEdges() {
        return (IRelation3.Immutable<O, L, S>) biSimulate(one::getExportEdges, two::getExportEdges);
    }

    @Override
    public IRelation3.Immutable<S, L, O> getImportEdges() {
        return (IRelation3.Immutable<S, L, O>) biSimulate(one::getImportEdges, two::getImportEdges);
    }

    @Override
    public boolean isOpen(S scope, L label) {
        return biSimulate(() -> one.isOpen(scope, label), () -> two.isOpen(scope, label));
    }

    @Override
    public IRelation3.Immutable<S, L, V> incompleteDirectEdges() {
        return (IRelation3.Immutable<S, L, V>) biSimulate(one::incompleteDirectEdges, two::incompleteDirectEdges);
    }

    @Override
    public IRelation3.Immutable<S, L, V> incompleteImportEdges() {
        return (IRelation3.Immutable<S, L, V>) biSimulate(one::incompleteImportEdges, two::incompleteImportEdges);
    }

    @Override
    public boolean isComplete() {
        return biSimulate(one::isComplete, two::isComplete);
    }

    @Override
    public org.metaborg.meta.nabl2.scopegraph.esop.IEsopScopeGraph.Transient<S, L, O, V> melt() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Not yet implemented.");
    }
    
}
