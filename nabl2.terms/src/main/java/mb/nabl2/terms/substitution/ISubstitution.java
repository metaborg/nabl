package mb.nabl2.terms.substitution;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;

public interface ISubstitution {

    boolean isEmpty();

    boolean contains(ITermVar var);

    ITerm apply(ITerm term);

    boolean isRenaming();

    interface Immutable extends ISubstitution {

        Immutable match(ITerm pattern, ITerm term) throws MatchException;

        Immutable put(ITermVar var, ITerm term);

        Immutable remove(ITermVar var);

        Immutable removeAll(Iterable<ITermVar> var);

        ISubstitution.Transient melt();

    }

    interface Transient extends ISubstitution {

        void match(ITerm pattern, ITerm term) throws MatchException;

        void put(ITermVar var, ITerm term);

        void remove(ITermVar var);

        void removeAll(Iterable<ITermVar> var);

        ISubstitution.Immutable freeze();

    }

}