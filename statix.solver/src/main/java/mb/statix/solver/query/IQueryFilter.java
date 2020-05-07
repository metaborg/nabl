package mb.statix.solver.query;

import com.google.common.collect.Multiset;

import mb.nabl2.regexp.IRegExpMatcher;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.spec.Rule;

public interface IQueryFilter {

    IRegExpMatcher<ITerm> getLabelWF();

    Rule getDataWF();

    Multiset<ITermVar> getVars();

    IQueryFilter apply(ISubstitution.Immutable subst);

    IQueryFilter apply(IRenaming subst);

    String toString(TermFormatter termToString);

}
