package org.metaborg.meta.nabl2.scopegraph.esop.persistent;

import static org.metaborg.meta.nabl2.scopegraph.esop.persistent.IBiSimulation.biSimulate;

import java.util.Optional;

import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopNameResolution;
import org.metaborg.meta.nabl2.scopegraph.path.IDeclPath;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPath;
import org.metaborg.meta.nabl2.util.tuples.Tuple2;

import io.usethesource.capsule.Set;

public class BiSimulationNameResolution<S extends IScope, L extends ILabel, O extends IOccurrence>
        implements IEsopNameResolution<S, L, O>, IBiSimulation, java.io.Serializable {

    private static final long serialVersionUID = 42L;

    private final IEsopNameResolution<S, L, O> one;
    private final IEsopNameResolution<S, L, O> two;

    public BiSimulationNameResolution(final IEsopNameResolution<S, L, O> one, final IEsopNameResolution<S, L, O> two) {
        this.one = one;
        this.two = two;
    }
    
    @Override
    public Set.Immutable<S> getAllScopes() {
        return biSimulate(one::getAllScopes, two::getAllScopes);
    }

    @Override
    public Set.Immutable<O> getAllRefs() {
        return biSimulate(one::getAllRefs, two::getAllRefs);
    }

    @Override
    public Set.Immutable<IResolutionPath<S, L, O>> resolve(O reference) {
        return biSimulate(() -> one.resolve(reference), () -> two.resolve(reference));
    }

    @Override
    public Set.Immutable<IDeclPath<S, L, O>> visible(S scope) {
        return biSimulate(() -> one.visible(scope), () -> two.visible(scope));
    }

    @Override
    public Set.Immutable<IDeclPath<S, L, O>> reachable(S scope) {
        return biSimulate(() -> one.reachable(scope), () -> two.reachable(scope));
    }

    @Override
    public Optional<Tuple2<Set.Immutable<IResolutionPath<S, L, O>>, Set.Immutable<String>>> tryResolve(O reference) {
        return biSimulate(() -> one.tryResolve(reference), () -> two.tryResolve(reference));
    }

    @Override
    public Optional<Tuple2<Set.Immutable<IDeclPath<S, L, O>>, Set.Immutable<String>>> tryVisible(S scope) {
        return biSimulate(() -> one.tryVisible(scope), () -> two.tryVisible(scope));
    }

    @Override
    public Optional<Tuple2<Set.Immutable<IDeclPath<S, L, O>>, Set.Immutable<String>>> tryReachable(S scope) {
        return biSimulate(() -> one.tryReachable(scope), () -> two.tryReachable(scope));
    }

}
