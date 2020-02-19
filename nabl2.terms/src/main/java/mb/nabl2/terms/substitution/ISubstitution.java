package mb.nabl2.terms.substitution;

import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;

public interface ISubstitution {

    boolean isEmpty();

    boolean contains(ITermVar var);

    Set<ITermVar> varSet();

    Set<ITermVar> freeVarSet();

    Set<? extends Entry<ITermVar, ? extends ITerm>> entrySet();

    ITerm apply(ITerm term);

    ITerm apply(ITermVar term);

    default List<ITerm> applyTerms(List<ITerm> terms) {
        return terms.stream().map(this::apply).collect(ImmutableList.toImmutableList());
    }

    interface Immutable extends ISubstitution {

        Immutable put(ITermVar var, ITerm term);

        Immutable remove(ITermVar var);

        Immutable removeAll(Iterable<ITermVar> var);

        Immutable compose(ISubstitution.Immutable other);

        default Immutable compose(ITermVar var, ITerm term) {
            return compose(PersistentSubstitution.Immutable.of(var, term));
        }

        ISubstitution.Transient melt();

    }

    interface Transient extends ISubstitution {

        void put(ITermVar var, ITerm term);

        void remove(ITermVar var);

        void removeAll(Iterable<ITermVar> var);

        void compose(ISubstitution.Immutable other);

        default void compose(ITermVar var, ITerm term) {
            compose(PersistentSubstitution.Immutable.of(var, term));
        }

        ISubstitution.Immutable freeze();

    }

}