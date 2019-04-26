package mb.statix.solver.query;

import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.spec.Rule;

public interface IQueryFilter {

    Rule getLabelWF();

    Rule getDataWF();

    IQueryFilter apply(ISubstitution.Immutable subst);

    String toString(TermFormatter termToString);

}