package mb.statix.solver.query;

import java.io.Serializable;
import java.util.Objects;

import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;
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

    @Override public Multiset<ITermVar> getVars() {
        final ImmutableMultiset.Builder<ITermVar> vars = ImmutableMultiset.builder();
        vars.addAll(dataOrd.varSet());
        return vars.build();
    }

    @Override public IQueryMin apply(ISubstitution.Immutable subst) {
        return new QueryMin(labelOrd, dataOrd.apply(subst));
    }

    @Override public IQueryMin apply(IRenaming subst) {
        return new QueryMin(labelOrd, dataOrd.apply(subst));
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QueryMin queryMin = (QueryMin) o;
        return Objects.equals(labelOrd, queryMin.labelOrd) &&
                Objects.equals(dataOrd, queryMin.dataOrd);
    }

    @Override
    public int hashCode() {
        return Objects.hash(labelOrd, dataOrd);
    }
}
