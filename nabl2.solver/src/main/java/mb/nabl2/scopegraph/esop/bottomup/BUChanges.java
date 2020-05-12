package mb.nabl2.scopegraph.esop.bottomup;

import java.util.stream.Stream;

import org.metaborg.util.functions.Function1;

import io.usethesource.capsule.Set;
import io.usethesource.capsule.Set.Immutable;
import io.usethesource.capsule.util.stream.CapsuleCollectors;
import mb.nabl2.scopegraph.ILabel;
import mb.nabl2.scopegraph.IOccurrence;
import mb.nabl2.scopegraph.IScope;
import mb.nabl2.scopegraph.path.IDeclPath;

public class BUChanges<S extends IScope, L extends ILabel, O extends IOccurrence, P extends IDeclPath<S, L, O>> {

    private final Set.Immutable<P> added;
    private final Set.Immutable<P> removed;

    BUChanges(Immutable<P> added, Immutable<P> removed) {
        this.added = added;
        this.removed = removed;
    }

    public boolean isEmpty() {
        return added.isEmpty() && removed.isEmpty();
    }

    public Set.Immutable<P> added() {
        return added;
    }

    public Set.Immutable<P> removed() {
        return removed;
    }

    public <Q extends IDeclPath<S, L, O>> BUChanges<S, L, O, Q> flatMap(Function1<P, Stream<Q>> mapper) {
        Set.Immutable<Q> mappedAdded = added.stream().flatMap(mapper::apply).collect(CapsuleCollectors.toSet());
        Set.Immutable<Q> mappedRemoved = removed.stream().flatMap(mapper::apply).collect(CapsuleCollectors.toSet());
        return new BUChanges<>(mappedAdded, mappedRemoved);
    }

}