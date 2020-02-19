package mb.statix.solver.query;

import mb.nabl2.regexp.IRegExpMatcher;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitutable;
import mb.nabl2.util.TermFormatter;
import mb.statix.spec.Rule;

public interface IQueryFilter extends ISubstitutable<IQueryFilter> {

    IRegExpMatcher<ITerm> getLabelWF();

    Rule getDataWF();

    String toString(TermFormatter termToString);

}