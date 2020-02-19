package mb.statix.solver.query;

import mb.nabl2.relations.IRelation;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.spec.Rule;

public interface IQueryMin {

    IRelation<ITerm> getLabelOrder();

    Rule getDataEquiv();

    IQueryMin substitute(ISubstitution.Immutable subst);

    String toString(TermFormatter termToString);

}