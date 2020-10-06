package mb.nabl2.scopegraph.esop.bottomup;

import java.util.stream.Stream;

import org.metaborg.util.functions.Function1;

import mb.nabl2.scopegraph.ILabel;
import mb.nabl2.scopegraph.IOccurrence;
import mb.nabl2.scopegraph.IScope;
import mb.nabl2.scopegraph.path.IDeclPath;
import mb.nabl2.scopegraph.terms.SpacedName;
import mb.nabl2.util.Tuple3;

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
            flatMap(Function1<P, Stream<Tuple3<SpacedName, L, Q>>> pathMapper, BUPathKeyFactory<L> keyFactory) {
        final BUPathSet.Transient<S, L, O, Q> mappedAddedPaths = BUPathSet.Transient.of(keyFactory);
        for(P ap : addedPaths.paths()) {
            pathMapper.apply(ap).forEach(e -> mappedAddedPaths.add(e._1(), e._2(), e._3()));
        }
        final BUPathSet.Transient<S, L, O, Q> mappedRemovedPaths = BUPathSet.Transient.of(keyFactory);
        for(P rp : removedPaths.paths()) {
            pathMapper.apply(rp).forEach(e -> mappedRemovedPaths.add(e._1(), e._2(), e._3()));
        }
        return new BUChanges<>(mappedAddedPaths.freeze(), mappedRemovedPaths.freeze());
    }

    public static <S extends IScope, L extends ILabel, O extends IOccurrence, P extends IDeclPath<S, L, O>>
            BUChanges<S, L, O, IDeclPath<S, L, O>> of() {
        return new BUChanges<>(BUPathSet.Immutable.of(), BUPathSet.Immutable.of());
    }

}