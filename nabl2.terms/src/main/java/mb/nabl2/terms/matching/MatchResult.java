package mb.nabl2.terms.matching;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.substitution.ISubstitution.Immutable;
import mb.nabl2.util.Tuple2;

public class MatchResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private final ISubstitution.Immutable substitution;
    private final Set<ITermVar> constrainedVars;
    private final List<Tuple2<ITerm, ITerm>> equalities;

    MatchResult(Immutable substitution, Set<ITermVar> constrainedVars, List<Tuple2<ITerm, ITerm>> equalities) {
        this.substitution = substitution;
        this.constrainedVars = constrainedVars;
        this.equalities = equalities;
    }

    public ISubstitution.Immutable substitution() {
        return substitution;
    }

    public Set<ITermVar> constrainedVars() {
        return constrainedVars;
    }

    public List<Tuple2<ITerm, ITerm>> equalities() {
        return equalities;
    }

}