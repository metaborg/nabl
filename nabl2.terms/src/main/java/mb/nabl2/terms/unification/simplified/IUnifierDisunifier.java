package mb.nabl2.terms.unification.simplified;

import java.util.Map.Entry;
import java.util.Optional;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.OccursException;

public interface IUnifierDisunifier extends IUnifier, IDisunifier {

    @Override Optional<? extends Result<? extends IUnifierDisunifier>>
            unifyAll(Iterable<? extends Entry<? extends ITerm, ? extends ITerm>> equalities) throws OccursException;

    @Override Result<ISubstitution.Immutable> removeAll(Iterable<ITermVar> vars);

    @Override Optional<? extends IUnifierDisunifier>
            disunifyAny(Iterable<? extends Entry<? extends ITerm, ? extends ITerm>> disequalities);

    interface Result<T> extends IUnifier.Result<T> {

        @Override T result();

        @Override IUnifierDisunifier unifier();

    }

}