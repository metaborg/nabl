package mb.statix.solver;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.IReplacement;

public interface ITermProperty {

    enum Multiplicity {
        SINGLETON, BAG
    }

    Multiplicity multiplicity();

    ITerm value();

    Iterable<ITerm> values();

    ITermProperty addValue(ITerm value);

    ITermProperty replace(IReplacement replacement);

}