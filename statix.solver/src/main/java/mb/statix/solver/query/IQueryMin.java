package mb.statix.solver.query;

import mb.nabl2.relations.IRelation;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitutable;
import mb.nabl2.util.TermFormatter;
import mb.statix.spec.Rule;

public interface IQueryMin extends ISubstitutable<IQueryMin> {

    IRelation<ITerm> getLabelOrder();

    Rule getDataEquiv();

    String toString(TermFormatter termToString);

}