package mb.statix.solver.persistent;

import java.io.Serializable;

import org.metaborg.util.collection.ConsList;

import mb.nabl2.terms.ITerm;
import mb.statix.solver.ITermProperty;

public class BagTermProperty implements ITermProperty, Serializable {

    private static final long serialVersionUID = 1L;

    private final ConsList<ITerm> values;

    private BagTermProperty(ConsList<ITerm> values) {
        this.values = values;
    }

    @Override public Multiplicity multiplicity() {
        return Multiplicity.BAG;
    }

    @Override public ITerm value() {
        throw new UnsupportedOperationException("Bag property does not have a single value.");
    };

    @Override public Iterable<ITerm> values() {
        return values;
    }

    @Override public ITermProperty addValue(ITerm value) {
        return new BagTermProperty(values.prepend(value));
    }

    public static ITermProperty of() {
        return new BagTermProperty(ConsList.nil());
    }

    public static ITermProperty of(ITerm value) {
        return new BagTermProperty(ConsList.of(value));
    }

}