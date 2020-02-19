package mb.nabl2.terms.substitution;

import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;

public interface IRenaming extends ISubstitution {

    @Override boolean isEmpty();

    @Override boolean contains(ITermVar var);

    @Override Set<ITermVar> varSet();

    @Override Set<? extends Entry<ITermVar, ITermVar>> entrySet();

    @Override ITerm apply(ITerm term);

    @Override ITermVar apply(ITermVar term);

    default List<ITermVar> applyVars(Iterable<ITermVar> terms) {
        return Streams.stream(terms).map(this::apply).collect(ImmutableList.toImmutableList());
    }

    interface Immutable extends IRenaming, ISubstitution.Immutable {

        IRenaming.Immutable put(ITermVar var, ITermVar term);

        @Override IRenaming.Immutable remove(ITermVar var);

        @Override IRenaming.Immutable removeAll(Iterable<ITermVar> var);

        IRenaming.Immutable compose(IRenaming.Immutable other);

        default IRenaming.Immutable compose(ITermVar var, ITermVar term) {
            return compose(PersistentRenaming.Immutable.of(var, term));
        }

        @Override IRenaming.Transient melt();

    }

    interface Transient extends IRenaming, ISubstitution.Transient {

        void put(ITermVar var, ITermVar term);

        @Deprecated @Override default void put(ITermVar var, ITerm term) {
            throw new UnsupportedOperationException("Cannot add a term value to a renaming.");
        }

        @Override void remove(ITermVar var);

        @Override void removeAll(Iterable<ITermVar> var);

        void compose(IRenaming.Immutable other);

        @Deprecated @Override default void compose(ISubstitution.Immutable other) {
            throw new UnsupportedOperationException("Cannot compose a substitution with a renaming.");
        }

        default void compose(ITermVar var, ITermVar term) {
            compose(PersistentRenaming.Immutable.of(var, term));
        }

        @Override IRenaming.Immutable freeze();

    }

}