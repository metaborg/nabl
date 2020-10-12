package mb.statix.solver.query;

import io.usethesource.capsule.Set;
import mb.nabl2.relations.IRelation;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.scopegraph.reference.EdgeOrData;
import mb.statix.spec.Rule;

public interface IQueryMin {

    IRelation<EdgeOrData<ITerm>> getLabelOrder();

    Rule getDataEquiv();

    Set.Immutable<ITermVar> getVars();

    IQueryMin apply(ISubstitution.Immutable subst);

    IQueryMin apply(IRenaming subst);

    String toString(TermFormatter termToString);

}
