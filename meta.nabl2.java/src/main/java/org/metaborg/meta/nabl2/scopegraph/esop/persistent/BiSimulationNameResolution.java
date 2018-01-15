package org.metaborg.meta.nabl2.scopegraph.esop.persistent;

import static org.metaborg.meta.nabl2.scopegraph.esop.persistent.IBiSimulation.biSimulate;

import java.util.Map.Entry;
import java.util.Optional;

import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IResolutionParameters;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopNameResolution;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.path.IDeclPath;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPath;
import org.metaborg.meta.nabl2.util.tuples.Tuple2;
import org.metaborg.util.functions.Predicate2;

import com.google.common.annotations.Beta;

import io.usethesource.capsule.Set;

public class BiSimulationNameResolution<S extends IScope, L extends ILabel, O extends IOccurrence>
        implements IEsopNameResolution.Immutable<S, L, O>, IBiSimulation, java.io.Serializable {

    private static final long serialVersionUID = 42L;

    private final IEsopNameResolution.Immutable<S, L, O> one;
    private final IEsopNameResolution.Immutable<S, L, O> two;

    public BiSimulationNameResolution(final IEsopNameResolution.Immutable<S, L, O> one, final IEsopNameResolution.Immutable<S, L, O> two) {
        this.one = one;
        this.two = two;
    }

    @Override
    public IResolutionParameters<L> getResolutionParameters() {
        return biSimulate(one::getResolutionParameters, two::getResolutionParameters);
    }    
    
    @Beta
    public IEsopScopeGraph<S, L, O, ?> getScopeGraph() {
        return biSimulate(one::getScopeGraph, two::getScopeGraph);
    }
    
    @Beta
    public boolean isEdgeClosed(S scope, L label) {
        return biSimulate(() -> one.isEdgeClosed(scope, label), () -> two.isEdgeClosed(scope, label));
    }
    
//    @Override
//    public Set.Immutable<S> getAllScopes() {
//        return biSimulate(one::getAllScopes, two::getAllScopes);
//    }
//
//    @Override
//    public Set.Immutable<O> getAllRefs() {
//        return biSimulate(one::getAllRefs, two::getAllRefs);
//    }

    @Override
    public Optional<io.usethesource.capsule.Set.Immutable<IResolutionPath<S, L, O>>> resolve(O reference) {
        return biSimulate(() -> one.resolve(reference), () -> two.resolve(reference));
    }

    @Override
    public Optional<io.usethesource.capsule.Set.Immutable<O>> visible(S scope) {
        return biSimulate(() -> one.visible(scope), () -> two.visible(scope));
    }

    @Override
    public Optional<io.usethesource.capsule.Set.Immutable<O>> reachable(S scope) {
        return biSimulate(() -> one.reachable(scope), () -> two.reachable(scope));
    }
        
//    @Override
//    public Optional<Tuple2<Set.Immutable<IResolutionPath<S, L, O>>, Set.Immutable<String>>> tryResolve(O reference) {
//        return biSimulate(() -> one.tryResolve(reference), () -> two.tryResolve(reference));
//    }
//
//    @Override
//    public Optional<Tuple2<Set.Immutable<IDeclPath<S, L, O>>, Set.Immutable<String>>> tryVisible(S scope) {
//        return biSimulate(() -> one.tryVisible(scope), () -> two.tryVisible(scope));
//    }
//
//    @Override
//    public Optional<Tuple2<Set.Immutable<IDeclPath<S, L, O>>, Set.Immutable<String>>> tryReachable(S scope) {
//        return biSimulate(() -> one.tryReachable(scope), () -> two.tryReachable(scope));
//    }

    @Override
    public java.util.Set<O> getResolvedRefs() {
        return biSimulate(one::getResolvedRefs, two::getResolvedRefs);
    }

    @Override
    public java.util.Set<Entry<O, io.usethesource.capsule.Set.Immutable<IResolutionPath<S, L, O>>>> resolutionEntries() {
        return biSimulate(one::resolutionEntries, two::resolutionEntries);
    }

    @Override
    public org.metaborg.meta.nabl2.scopegraph.esop.IEsopNameResolution.Transient<S, L, O> melt(
            IEsopScopeGraph<S, L, O, ?> scopeGraph, Predicate2<S, L> isEdgeClosed) {
        return new TransientBiSimulationNameResolution<>(
                one.melt(scopeGraph, isEdgeClosed), 
                two.melt(scopeGraph, isEdgeClosed)); 
    }

}

class TransientBiSimulationNameResolution<S extends IScope, L extends ILabel, O extends IOccurrence>
        implements IEsopNameResolution.Transient<S, L, O>, IBiSimulation {

    private final IEsopNameResolution.Transient<S, L, O> one;
    private final IEsopNameResolution.Transient<S, L, O> two;

    public TransientBiSimulationNameResolution(final IEsopNameResolution.Transient<S, L, O> one, final IEsopNameResolution.Transient<S, L, O> two) {
        this.one = one;
        this.two = two;
    }
    
    @Override
    public IResolutionParameters<L> getResolutionParameters() {
        return biSimulate(one::getResolutionParameters, two::getResolutionParameters);
    }

    @Override
    public IEsopScopeGraph<S, L, O, ?> getScopeGraph() {
        return biSimulate(one::getScopeGraph, two::getScopeGraph);
    }
    
    @Beta
    public boolean isEdgeClosed(S scope, L label) {
        return biSimulate(() -> one.isEdgeClosed(scope, label), () -> two.isEdgeClosed(scope, label));
    }    

    @Override
    public java.util.Set<O> getResolvedRefs() {
        return biSimulate(one::getResolvedRefs, two::getResolvedRefs);
    }

    @Override
    public Optional<io.usethesource.capsule.Set.Immutable<IResolutionPath<S, L, O>>> resolve(O reference) {
        return biSimulate(() -> one.resolve(reference), () -> two.resolve(reference));
    }

    @Override
    public Optional<io.usethesource.capsule.Set.Immutable<O>> visible(S scope) {
        return biSimulate(() -> one.visible(scope), () -> two.visible(scope));
    }

    @Override
    public Optional<io.usethesource.capsule.Set.Immutable<O>> reachable(S scope) {
        return biSimulate(() -> one.reachable(scope), () -> two.reachable(scope));
    }

    @Override
    public java.util.Set<Entry<O, io.usethesource.capsule.Set.Immutable<IResolutionPath<S, L, O>>>> resolutionEntries() {
        return biSimulate(one::resolutionEntries, two::resolutionEntries);
    }

    @Override
    public boolean addAll(IEsopNameResolution<S, L, O> other) {
        return biSimulate(() -> one.addAll(other), () -> two.addAll(other));

    }

    @Override
    public org.metaborg.meta.nabl2.scopegraph.esop.IEsopNameResolution.Immutable<S, L, O> freeze() {
        return new BiSimulationNameResolution<>(one.freeze(), two.freeze());
    }

}
