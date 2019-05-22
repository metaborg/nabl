package mb.statix.solver.query;

import java.io.Serializable;

import mb.nabl2.regexp.IRegExp;
import mb.nabl2.regexp.IRegExpMatcher;
import mb.nabl2.regexp.RegExpMatcher;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.spec.IRule;

/**
 * Class to represent query filters.
 * 
 * <pre>filter &lt;path&gt; and &lt;data&gt;</pre>
 */
public class QueryFilter implements IQueryFilter, Serializable {
    private static final long serialVersionUID = 1L;

    private final IRegExpMatcher<ITerm> pathWf;
    private final IRule dataWf;

    public QueryFilter(IRegExp<ITerm> pathWf, IRule dataConstraint) {
        this(RegExpMatcher.create(pathWf), dataConstraint);
    }

    private QueryFilter(IRegExpMatcher<ITerm> pathWf, IRule dataConstraint) {
        this.pathWf = pathWf;
        this.dataWf = dataConstraint;
    }

    @Override public IQueryFilter apply(ISubstitution.Immutable subst) {
        return new QueryFilter(pathWf, dataWf.apply(subst));
    }

    @Override public IRegExpMatcher<ITerm> getLabelWF() {
        return pathWf;
    }

    @Override public IRule getDataWF() {
        return dataWf;
    }

    @Override public String toString(TermFormatter termToString) {
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
}
