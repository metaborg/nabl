package mb.statix.solver;

import mb.nabl2.terms.ITerm;

public interface ITermProperty {

    enum Multiplicity {
        SINGLETON, BAG
    }

    Multiplicity multiplicity();

    ITerm value();

    Iterable<ITerm> values();

    ITermProperty addValue(ITerm value);

}