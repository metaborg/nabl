package mb.scopegraph.pepm16.bottomup;

import java.util.Collection;

import org.metaborg.util.functions.Function2;
import org.metaborg.util.functions.Predicate2;
import org.metaborg.util.tuple.Tuple2;

import mb.scopegraph.pepm16.ILabel;
import mb.scopegraph.pepm16.IOccurrence;
import mb.scopegraph.pepm16.IScope;
import mb.scopegraph.pepm16.path.IDeclPath;
import mb.scopegraph.pepm16.terms.SpacedName;

public class BUChanges<S extends IScope, L extends ILabel, O extends IOccurrence, P extends IDeclPath<S, L, O>> {

    private final BUPathSet.Immutable<S, L, O, P> addedPaths;
    private final BUPathSet.Immutable<S, L, O, P> removedPaths;

    BUChanges(BUPathSet.Immutable<S, L, O, P> addedPaths, BUPathSet.Immutable<S, L, O, P> removedPaths) {
        this.addedPaths = addedPaths;
        this.removedPaths = removedPaths;
    }

    public boolean isEmpty() {
        return addedPaths.isEmpty() && removedPaths.isEmpty();
    }

    public BUPathSet.Immutable<S, L, O, P> addedPaths() {
        return addedPaths;
    }

    public BUPathSet.Immutable<S, L, O, P> removedPaths() {
        return removedPaths;
    }

    public <Q extends IDeclPath<S, L, O>> BUChanges<S, L, O, Q>
            flatMap(Function2<BUPathKey<L>, Collection<P>, Tuple2<BUPathKey<L>, Collection<Q>>> pathMapper) {
        final BUPathSet.Transient<S, L, O, Q> mappedAddedPaths = BUPathSet.Transient.of();
        for(SpacedName an : addedPaths.names()) {
            for(BUPathKey<L> ak : addedPaths.keys(an)) {
                pathMapper.apply(ak, addedPaths.paths(ak)).apply(mappedAddedPaths::add);
            }
        }
        final BUPathSet.Transient<S, L, O, Q> mappedRemovedPaths = BUPathSet.Transient.of();
        for(SpacedName rn : removedPaths.names()) {
            for(BUPathKey<L> rk : removedPaths.keys(rn)) {
                pathMapper.apply(rk, removedPaths.paths(rk)).apply(mappedRemovedPaths::add);
            }
        }
        return new BUChanges<>(mappedAddedPaths.freeze(), mappedRemovedPaths.freeze());
    }

    public BUChanges<S, L, O, P> filter(Predicate2<BUPathKey<L>, P> pathFilter) {
        return new BUChanges<>(addedPaths.filter(pathFilter), removedPaths.filter(pathFilter));
    }


    public static <S extends IScope, L extends ILabel, O extends IOccurrence, P extends IDeclPath<S, L, O>>
            BUChanges<S, L, O, P> of(BUEnvKey<S, L> origin) {
        return new BUChanges<>(BUPathSet.Immutable.of(), BUPathSet.Immutable.of());
    }

    public static <S extends IScope, L extends ILabel, O extends IOccurrence, P extends IDeclPath<S, L, O>>
            BUChanges<S, L, O, P> ofPaths(BUEnvKey<S, L> origin, BUPathSet.Immutable<S, L, O, P> paths) {
        return new BUChanges<>(paths, BUPathSet.Immutable.of());
    }

}