package mb.statix.solver.persistent;

import java.io.Serializable;
import java.util.Collection;

import org.metaborg.util.collection.ConsList;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.IReplacement;
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

    @Override public Collection<ITerm> values() {
        return values;
    }

    @Override public ITermProperty addValue(ITerm value) {
        return new BagTermProperty(values.prepend(value));
    }

    @Override public ITermProperty replace(IReplacement replacement) {
        final ITermProperty newProp = BagTermProperty.of();
        values.forEach(val -> newProp.addValue(replacement.apply(val)));
        return newProp;
    }

    public static ITermProperty of() {
        return new BagTermProperty(ConsList.nil());
    }

    public static ITermProperty of(ITerm value) {
        return new BagTermProperty(ConsList.of(value));
    }

}