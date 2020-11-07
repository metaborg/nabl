package mb.statix.scopegraph.bottomup;

import java.util.Collection;

import org.metaborg.util.functions.Function2;
import org.metaborg.util.functions.Predicate2;

import mb.nabl2.util.Tuple2;
import mb.statix.scopegraph.path.IResolutionPath;

public class BUChanges<S, L, D, P extends IResolutionPath<S, L, D>> {

    private final BUPathSet.Immutable<S, L, D, P> addedPaths;
    private final BUPathSet.Immutable<S, L, D, P> removedPaths;

    BUChanges(BUPathSet.Immutable<S, L, D, P> addedPaths, BUPathSet.Immutable<S, L, D, P> removedPaths) {
        this.addedPaths = addedPaths;
        this.removedPaths = removedPaths;
    }

    public boolean isEmpty() {
        return addedPaths.isEmpty() && removedPaths.isEmpty();
    }

    public BUPathSet.Immutable<S, L, D, P> addedPaths() {
        return addedPaths;
    }

    public BUPathSet.Immutable<S, L, D, P> removedPaths() {
        return removedPaths;
    }

    public <Q extends IResolutionPath<S, L, D>> BUChanges<S, L, D, Q>
            flatMap(Function2<BUPathKey<L>, Collection<P>, Tuple2<BUPathKey<L>, Collection<Q>>> pathMapper) {
        final BUPathSet.Transient<S, L, D, Q> mappedAddedPaths = BUPathSet.Transient.of();
        for(SpacedName an : addedPaths.names()) {
            for(BUPathKey<L> ak : addedPaths.keys(an)) {
                pathMapper.apply(ak, addedPaths.paths(ak)).apply(mappedAddedPaths::addAll);
            }
        }
        final BUPathSet.Transient<S, L, D, Q> mappedRemovedPaths = BUPathSet.Transient.of();
        for(SpacedName rn : removedPaths.names()) {
            for(BUPathKey<L> rk : removedPaths.keys(rn)) {
                pathMapper.apply(rk, removedPaths.paths(rk)).apply(mappedRemovedPaths::addAll);
            }
        }
        return new BUChanges<>(mappedAddedPaths.freeze(), mappedRemovedPaths.freeze());
    }

    public BUChanges<S, L, D, P> filter(Predicate2<BUPathKey<L>, P> pathFilter) {
        return new BUChanges<>(addedPaths.filter(pathFilter), removedPaths.filter(pathFilter));
    }


    public static <S, L, D, P extends IResolutionPath<S, L, D>> BUChanges<S, L, D, P> of(BUEnvKey<S, L, D> origin) {
        return new BUChanges<>(BUPathSet.Immutable.of(), BUPathSet.Immutable.of());
    }

    public static <S, L, D, P extends IResolutionPath<S, L, D>> BUChanges<S, L, D, P> ofPaths(BUEnvKey<S, L, D> origin,
            BUPathSet.Immutable<S, L, D, P> paths) {
        return new BUChanges<>(paths, BUPathSet.Immutable.of());
    }

}