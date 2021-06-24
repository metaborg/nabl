package mb.statix.solver.persistent;

import java.io.Serializable;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.IReplacement;
import mb.statix.solver.ITermProperty;

public class SingletonTermProperty implements ITermProperty, Serializable {

    private static final long serialVersionUID = 1L;

    private final ITerm value;

    private SingletonTermProperty(ITerm value) {
        this.value = value;
    }

    @Override public Multiplicity multiplicity() {
        return Multiplicity.SINGLETON;
    }

    @Override public ITerm value() {
        return value;
    };

    @Override public Iterable<ITerm> values() {
        throw new UnsupportedOperationException("Singleton property does not support multiple values.");
    }

    @Override public ITermProperty addValue(@SuppressWarnings("unused") ITerm value) {
        throw new UnsupportedOperationException("Cannot add value to singleton property.");
    }

    @Override public ITermProperty replace(IReplacement replacement) {
        return SingletonTermProperty.of(replacement.apply(value));
    }

    public static ITermProperty of(ITerm value) {
        return new SingletonTermProperty(value);
    }

}