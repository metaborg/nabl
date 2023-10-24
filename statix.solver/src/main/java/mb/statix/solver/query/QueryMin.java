package mb.statix.solver.query;

import java.io.Serializable;
import java.util.Objects;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.scopegraph.oopsla20.reference.EdgeOrData;
import mb.scopegraph.relations.IRelation;
import mb.statix.spec.Rule;
import mb.statix.spec.RuleUtil;

public class QueryMin implements Serializable {
    private static final long serialVersionUID = 1L;

    private final IRelation.Immutable<EdgeOrData<ITerm>> labelOrd;
    private final Rule dataOrd;

    public QueryMin(IRelation.Immutable<EdgeOrData<ITerm>> labelOrd, Rule dataConstraint) {
        this.labelOrd = labelOrd;
        this.dataOrd = dataConstraint;
    }

    public IRelation.Immutable<EdgeOrData<ITerm>> getLabelOrder() {
        return labelOrd;
    }

    public Rule getDataEquiv() {
        return dataOrd;
    }

    public Set.Immutable<ITermVar> getVars() {
        return RuleUtil.vars(dataOrd);
    }

    /**
     * Apply capture avoiding substitution.
     *
     * @param subst the substitution to apply
     */
    public QueryMin apply(ISubstitution.Immutable subst) {
        return apply(subst, false);
    }

    /**
     * Apply unguarded substitution, which may result in capture.
     *
     * @param subst the substitution to apply
     */
    public QueryMin unsafeApply(ISubstitution.Immutable subst) {
        return unsafeApply(subst, false);
    }

    /**
     * Apply variable renaming.
     *
     * @param subst the substitution to apply
     */
    public QueryMin apply(IRenaming subst) {
        return apply(subst, false);
    }

    /**
     * Apply capture avoiding substitution.
     *
     * @param subst the substitution to apply
     * @param trackOrigins whether to track the syntactic origin of the constraints, if not already tracked
     */
    public QueryMin apply(ISubstitution.Immutable subst, boolean trackOrigins) {
        return new QueryMin(labelOrd, dataOrd.apply(subst, trackOrigins));
    }

    /**
     * Apply unguarded substitution, which may result in capture.
     *
     * @param subst the substitution to apply
     * @param trackOrigins whether to track the syntactic origin of the constraints, if not already tracked
     */
    public QueryMin unsafeApply(ISubstitution.Immutable subst, boolean trackOrigins) {
        return new QueryMin(labelOrd, dataOrd.unsafeApply(subst, trackOrigins));
    }

    /**
     * Apply variable renaming.
     *
     * @param subst the substitution to apply
     * @param trackOrigins whether to track the syntactic origin of the constraints, if not already tracked
     */
    public QueryMin apply(IRenaming subst, boolean trackOrigins) {
        return new QueryMin(labelOrd, dataOrd.apply(subst, trackOrigins));
    }

    public String toString(TermFormatter termToString) {
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

    @Override public boolean equals(Object o) {
        if(this == o)
            return true;
        if(o == null || getClass() != o.getClass())
            return false;
        QueryMin queryMin = (QueryMin) o;
        return Objects.equals(labelOrd, queryMin.labelOrd) && Objects.equals(dataOrd, queryMin.dataOrd);
    }

    @Override public int hashCode() {
        return Objects.hash(labelOrd, dataOrd);
    }
}
