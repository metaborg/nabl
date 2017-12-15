package org.metaborg.meta.nabl2.scopegraph.esop.persistent;

import static org.metaborg.meta.nabl2.scopegraph.esop.persistent.IBiSimulation.biSimulate;

import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopScopeGraph;
import org.metaborg.meta.nabl2.util.collections.IFunction;
import org.metaborg.meta.nabl2.util.collections.IRelation3;
import org.metaborg.util.functions.PartialFunction1;

import io.usethesource.capsule.Set;

public class BiSimulationScopeGraph<S extends IScope, L extends ILabel, O extends IOccurrence, V>
        implements IEsopScopeGraph.Immutable<S, L, O, V>, IBiSimulation, java.io.Serializable {
    
    private static final long serialVersionUID = 42L;

    private final IEsopScopeGraph.Immutable<S, L, O, V> one;
    private final IEsopScopeGraph.Immutable<S, L, O, V> two;

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
        return new TransientBiSimulationScopeGraph<>(one.melt(), two.melt());
    }
    
    @Override
    public String toString() {
        return two.toString();
    }
    
}

class TransientBiSimulationScopeGraph<S extends IScope, L extends ILabel, O extends IOccurrence, V>
        implements IEsopScopeGraph.Transient<S, L, O, V>, IBiSimulation {

    private final IEsopScopeGraph.Transient<S, L, O, V> one;
    private final IEsopScopeGraph.Transient<S, L, O, V> two;

    public TransientBiSimulationScopeGraph(final IEsopScopeGraph.Transient<S, L, O, V> one, final IEsopScopeGraph.Transient<S, L, O, V> two) {
        this.one = one;
        this.two = two;
    }
    
    @Override
    public boolean isOpen(S scope, L label) {
        return biSimulate(
                () -> one.isOpen(scope, label), 
                () -> two.isOpen(scope, label));
    }

    @Override
    public IRelation3<S, L, V> incompleteDirectEdges() {
        return biSimulate(one::incompleteDirectEdges, two::incompleteDirectEdges);
    }

    @Override
    public IRelation3<S, L, V> incompleteImportEdges() {
        return biSimulate(one::incompleteImportEdges, two::incompleteImportEdges);
    }

    @Override
    public boolean isComplete() {
        return biSimulate(one::isComplete, two::isComplete);
    }

    @Override
    public Set<S> getAllScopes() {
        return biSimulate(one::getAllScopes, two::getAllScopes);
    }

    @Override
    public Set<O> getAllDecls() {
        return biSimulate(one::getAllDecls, two::getAllDecls);
    }

    @Override
    public Set<O> getAllRefs() {
        return biSimulate(one::getAllRefs, two::getAllRefs);
    }

    @Override
    public IFunction<O, S> getDecls() {
        return biSimulate(one::getDecls, two::getDecls);
    }

    @Override
    public IFunction<O, S> getRefs() {
        return biSimulate(one::getRefs, two::getRefs);
    }

    @Override
    public IRelation3<S, L, S> getDirectEdges() {
        return biSimulate(one::getDirectEdges, two::getDirectEdges);
    }

    @Override
    public IRelation3<O, L, S> getExportEdges() {
        return biSimulate(one::getExportEdges, two::getExportEdges);
    }

    @Override
    public IRelation3<S, L, O> getImportEdges() {
        return biSimulate(one::getImportEdges, two::getImportEdges);
    }

    @Override
    public boolean addDecl(S scope, O decl) {
        return biSimulate(
                () -> one.addDecl(scope, decl), 
                () -> two.addDecl(scope, decl));
    }

    @Override
    public boolean addRef(O ref, S scope) {
        return biSimulate(
                () -> one.addRef(ref, scope), 
                () -> two.addRef(ref, scope));
    }

    @Override
    public boolean addDirectEdge(S sourceScope, L label, S targetScope) {
        return biSimulate(
                () -> one.addDirectEdge(sourceScope, label, targetScope), 
                () -> two.addDirectEdge(sourceScope, label, targetScope));
    }

    @Override
    public boolean addIncompleteDirectEdge(S scope, L label, V var) {
        return biSimulate(
                () -> one.addIncompleteDirectEdge(scope, label, var), 
                () -> two.addIncompleteDirectEdge(scope, label, var));
    }

    @Override
    public boolean addExportEdge(O decl, L label, S scope) {
        return biSimulate(
                () -> one.addExportEdge(decl, label, scope), 
                () -> two.addExportEdge(decl, label, scope));
    }

    @Override
    public boolean addImportEdge(S scope, L label, O ref) {
        return biSimulate(
                () -> one.addImportEdge(scope, label, ref), 
                () -> two.addImportEdge(scope, label, ref));
    }

    @Override
    public boolean addIncompleteImportEdge(S scope, L label, V var) {
        return biSimulate(
                () -> one.addIncompleteImportEdge(scope, label, var), 
                () -> two.addIncompleteImportEdge(scope, label, var));
    }

    @Override
    public boolean addAll(IEsopScopeGraph<S, L, O, V> other) {
        return biSimulate(
                () -> one.addAll(other), 
                () -> two.addAll(other));
    }

    @Override
    public boolean reduce(PartialFunction1<V, S> fs, PartialFunction1<V, O> fo) {
        return biSimulate(
                () -> one.reduce(fs, fo), 
                () -> two.reduce(fs, fo));
    }

    @Override
    public org.metaborg.meta.nabl2.scopegraph.esop.IEsopScopeGraph.Immutable<S, L, O, V> freeze() {
        return new BiSimulationScopeGraph<>(one.freeze(), two.freeze());
    }
    
    @Override
    public String toString() {
        return two.toString();
    }    
}
