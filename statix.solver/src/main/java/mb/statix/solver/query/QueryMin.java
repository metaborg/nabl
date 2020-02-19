package mb.statix.solver.query;

import java.io.Serializable;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import mb.nabl2.relations.IRelation;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.spec.Rule;

public class QueryMin implements IQueryMin, Serializable {
    private static final long serialVersionUID = 1L;

    private final IRelation.Immutable<ITerm> labelOrd;
    private final Rule dataOrd;

    public QueryMin(IRelation.Immutable<ITerm> labelOrd, Rule dataConstraint) {
        this.labelOrd = labelOrd;
        this.dataOrd = dataConstraint;
    }

    @Override public IRelation<ITerm> getLabelOrder() {
        return labelOrd;
    }

    @Override public Rule getDataEquiv() {
        return dataOrd;
    }

    @Override public Set<ITermVar> boundVars() {
        return ImmutableSet.of();
    }

    @Override public Set<ITermVar> freeVars() {
        final ImmutableSet.Builder<ITermVar> freeVars = ImmutableSet.builder();
        freeVars.addAll(dataOrd.freeVars());
        return freeVars.build();
    }

    @Override public IQueryMin doSubstitute(IRenaming.Immutable localRenaming, ISubstitution.Immutable totalSubst) {
        return new QueryMin(labelOrd, dataOrd.recSubstitute(totalSubst));
    }

    @Override public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append("min ");
        sb.append(labelOrd);
        sb.append(" and ");
        sb.append(dataOrd.toString(termToString));
        return sb.toString();
    }

    @Override public String toString() {
        return toString(ITerm::toString);
    }

}
