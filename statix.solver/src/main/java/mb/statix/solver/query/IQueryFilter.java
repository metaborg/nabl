package mb.statix.solver.query;

import mb.nabl2.regexp.IRegExpMatcher;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.spec.Rule;

public interface IQueryFilter {

    IRegExpMatcher<ITerm> getLabelWF();

    Rule getDataWF();

    IQueryFilter substitute(ISubstitution.Immutable subst);

    String toString(TermFormatter termToString);

}