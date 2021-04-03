package mb.statix.solver.query;

import java.io.Serializable;
import java.util.Objects;

import io.usethesource.capsule.Set;
import mb.nabl2.regexp.IRegExp;
import mb.nabl2.regexp.IRegExpMatcher;
import mb.nabl2.regexp.RegExpMatcher;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.spec.Rule;
import mb.statix.spec.RuleUtil;

public class QueryFilter implements Serializable {
    private static final long serialVersionUID = 1L;

    private final IRegExpMatcher<ITerm> pathWf;
    private final Rule dataWf;

    public QueryFilter(IRegExp<ITerm> pathWf, Rule dataConstraint) {
        this(RegExpMatcher.create(pathWf), dataConstraint);
    }

    public QueryFilter(IRegExpMatcher<ITerm> pathWf, Rule dataConstraint) {
        this.pathWf = pathWf;
        this.dataWf = dataConstraint;
    }

    public IRegExpMatcher<ITerm> getLabelWF() {
        return pathWf;
    }

    public Rule getDataWF() {
        return dataWf;
    }

    public Set.Immutable<ITermVar> getVars() {
        return RuleUtil.vars(dataWf);
    }

    public QueryFilter apply(ISubstitution.Immutable subst) {
        return new QueryFilter(pathWf, dataWf.apply(subst));
    }

    public QueryFilter apply(IRenaming subst) {
        return new QueryFilter(pathWf, dataWf.apply(subst));
    }

    public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append("filter ");
        sb.append(pathWf);
        sb.append(" and ");
        sb.append(dataWf.toString(termToString));
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
        QueryFilter that = (QueryFilter) o;
        return Objects.equals(pathWf, that.pathWf) && Objects.equals(dataWf, that.dataWf);
    }

    @Override public int hashCode() {
        return Objects.hash(pathWf, dataWf);
    }
}
