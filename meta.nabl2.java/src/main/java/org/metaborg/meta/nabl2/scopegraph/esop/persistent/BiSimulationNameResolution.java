package org.metaborg.meta.nabl2.scopegraph.esop.persistent;

import java.io.Serializable;
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
        implements IEsopNameResolution<S, L, O>, Serializable {

    private static final long serialVersionUID = 42L;

    private final IEsopNameResolution<S, L, O> one;
    private final IEsopNameResolution<S, L, O> two;

    public BiSimulationNameResolution(final IEsopNameResolution<S, L, O> one, final IEsopNameResolution<S, L, O> two) {
        this.one = one;
        this.two = two;
    }
    
    private void signalError() {
        // throw new IllegalStateException();        
    }
    
    private static <T> T choose(T one, T two) {
        return two;
    }
    
    @Override
    public Set.Immutable<S> getAllScopes() {
        final Set.Immutable<S> resultOne = one.getAllScopes();
        final Set.Immutable<S> resultTwo = two.getAllScopes();
        boolean equal = resultOne.equals(resultTwo);

        if (!equal)
            signalError();
        
        return choose(resultOne, resultTwo);
    }

    @Override
    public Set.Immutable<O> getAllRefs() {
        final Set.Immutable<O> resultOne = one.getAllRefs();
        final Set.Immutable<O> resultTwo = two.getAllRefs();
        boolean equal = resultOne.equals(resultTwo);       

        if (!equal)
            signalError();

        
        return choose(resultOne, resultTwo);
    }

    @Override
    public Set.Immutable<IResolutionPath<S, L, O>> resolve(O reference) {
        final Set.Immutable<IResolutionPath<S, L, O>> resultOne = one.resolve(reference);
        final Set.Immutable<IResolutionPath<S, L, O>> resultTwo = two.resolve(reference);
        boolean equal = resultOne.equals(resultTwo);       

        if (!equal)
            signalError();
        
        return choose(resultOne, resultTwo);
    }

    @Override
    public Set.Immutable<IDeclPath<S, L, O>> visible(S scope) {
        final Set.Immutable<IDeclPath<S, L, O>> resultOne = one.visible(scope);
        final Set.Immutable<IDeclPath<S, L, O>> resultTwo = two.visible(scope);
        boolean equal = resultOne.equals(resultTwo);       

        if (!equal)
            signalError();
        
        return choose(resultOne, resultTwo);
    }

    @Override
    public Set.Immutable<IDeclPath<S, L, O>> reachable(S scope) {
        final Set.Immutable<IDeclPath<S, L, O>> resultOne = one.reachable(scope);
        final Set.Immutable<IDeclPath<S, L, O>> resultTwo = two.reachable(scope);
        boolean equal = resultOne.equals(resultTwo);

        if (!equal)
            signalError();
        
        return choose(resultOne, resultTwo);
    }

    @Override
    public Optional<Tuple2<Set.Immutable<IResolutionPath<S, L, O>>, Set.Immutable<String>>> tryResolve(O reference) {
        final Optional<Tuple2<Set.Immutable<IResolutionPath<S, L, O>>, Set.Immutable<String>>> resultOne = one
                .tryResolve(reference);
        final Optional<Tuple2<Set.Immutable<IResolutionPath<S, L, O>>, Set.Immutable<String>>> resultTwo = two
                .tryResolve(reference);
        boolean equal = resultOne.equals(resultTwo);       

        if (!equal)
            signalError();
        
        return choose(resultOne, resultTwo);
    }

    @Override
    public Optional<Tuple2<Set.Immutable<IDeclPath<S, L, O>>, Set.Immutable<String>>> tryVisible(S scope) {
        final Optional<Tuple2<Set.Immutable<IDeclPath<S, L, O>>, Set.Immutable<String>>> resultOne = one
                .tryVisible(scope);
        final Optional<Tuple2<Set.Immutable<IDeclPath<S, L, O>>, Set.Immutable<String>>> resultTwo = two
                .tryVisible(scope);
        boolean equal = resultOne.equals(resultTwo);       

        if (!equal)
            signalError();
        
        return choose(resultOne, resultTwo);
    }

    @Override
    public Optional<Tuple2<Set.Immutable<IDeclPath<S, L, O>>, Set.Immutable<String>>> tryReachable(S scope) {
        final Optional<Tuple2<Set.Immutable<IDeclPath<S, L, O>>, Set.Immutable<String>>> resultOne = one
                .tryReachable(scope);
        final Optional<Tuple2<Set.Immutable<IDeclPath<S, L, O>>, Set.Immutable<String>>> resultTwo = two
                .tryReachable(scope);
        boolean equal = resultOne.equals(resultTwo);

        if (!equal)
            signalError();

        return choose(resultOne, resultTwo);
    }

}
