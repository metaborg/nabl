package mb.statix.solver.query;

import mb.nabl2.relations.IRelation;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.spec.IRule;

/**
 * Interface to represent query min.
 * 
 * <pre>min &lt;pathConstraint&gt; and &lt;dataConstraint&gt;</pre>
 */
public interface IQueryMin {

    IQueryMin apply(ISubstitution.Immutable subst);

    IRelation<ITerm> getLabelOrder();

    IRule getDataEquiv();

    String toString(TermFormatter termToString);
}