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

    Set<ITermVar> domainSet();

    Set<ITermVar> rangeSet();

    Set<Entry<ITermVar, ITerm>> entrySet();

    ITerm apply(ITerm term);

    default List<ITerm> apply(List<ITerm> terms) {
        final ImmutableList.Builder<ITerm> newTerms = ImmutableList.builderWithExpectedSize(terms.size());
        for(ITerm term : terms) {
            newTerms.add(apply(term));
        }
        return newTerms.build();
    }

    interface Immutable extends ISubstitution {

        Immutable put(ITermVar var, ITerm term);

        Immutable remove(ITermVar var);

        Immutable removeAll(Iterable<ITermVar> var);

        /**
         * Compose this substitution with another, resulting in the substitution that has the same effect as applying
         * this substitution first, and then the other.
         */
        Immutable compose(ISubstitution.Immutable other);

        /**
         * Compose this substitution with another, resulting in the substitution that has the same effect as applying
         * this substitution first, and then the other.
         */
        Immutable compose(ITermVar var, ITerm term);

        ISubstitution.Transient melt();

    }

    interface Transient extends ISubstitution {

        void put(ITermVar var, ITerm term);

        void remove(ITermVar var);

        void removeAll(Iterable<ITermVar> var);

        /**
         * Compose this substitution with another, resulting in the substitution that has the same effect as applying
         * this substitution first, and then the other.
         */
        void compose(ISubstitution.Immutable other);

        /**
         * Compose this substitution with another, resulting in the substitution that has the same effect as applying
         * this substitution first, and then the other.
         */
        void compose(ITermVar var, ITerm term);

        ISubstitution.Immutable freeze();

    }

}