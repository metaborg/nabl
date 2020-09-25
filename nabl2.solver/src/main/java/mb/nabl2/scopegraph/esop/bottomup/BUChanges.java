package mb.nabl2.scopegraph.esop.bottomup;

import java.util.stream.Stream;

import org.metaborg.util.functions.Function1;

import io.usethesource.capsule.Set;
import io.usethesource.capsule.util.stream.CapsuleCollectors;
import mb.nabl2.scopegraph.ILabel;
import mb.nabl2.scopegraph.IOccurrence;
import mb.nabl2.scopegraph.IScope;
import mb.nabl2.scopegraph.path.IDeclPath;
import mb.nabl2.scopegraph.path.IOpenPath;

public class BUChanges<S extends IScope, L extends ILabel, O extends IOccurrence, P extends IDeclPath<S, L, O>> {

    private final Set.Immutable<P> addedPaths;
    private final Set.Immutable<P> removedPaths;

    BUChanges(Set.Immutable<P> addedPaths, Set.Immutable<P> removedPaths) {
        this.addedPaths = addedPaths;
        this.removedPaths = removedPaths;
    }

    public boolean isEmpty() {
        return addedPaths.isEmpty();
    }

    public Set.Immutable<P> addedPaths() {
        return addedPaths;
    }

    public Set.Immutable<P> removedPaths() {
        return removedPaths;
    }

    public <Q extends IDeclPath<S, L, O>> BUChanges<S, L, O, Q> flatMap(Function1<P, Stream<Q>> pathMapper,
            Function1<IOpenPath<S, L, O>, Stream<IOpenPath<S, L, O>>> openMapper) {
        final Set.Immutable<Q> mappedAddedPaths =
                addedPaths.stream().flatMap(pathMapper::apply).collect(CapsuleCollectors.toSet());
        final Set.Immutable<Q> mappedRemovedPaths =
                removedPaths.stream().flatMap(pathMapper::apply).collect(CapsuleCollectors.toSet());
        return new BUChanges<>(mappedAddedPaths, mappedRemovedPaths);
    }

}