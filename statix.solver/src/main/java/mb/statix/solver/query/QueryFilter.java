package mb.statix.solver.query;

import java.io.Serializable;

import mb.nabl2.regexp.IRegExp;
import mb.nabl2.regexp.IRegExpMatcher;
import mb.nabl2.regexp.RegExpMatcher;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.solver.State;
import mb.statix.solver.completeness.IsComplete;
import mb.statix.solver.log.IDebugContext;
import mb.statix.spec.Rule;

public class QueryFilter implements IQueryFilter, Serializable {
    private static final long serialVersionUID = 1L;

    private final IRegExpMatcher<ITerm> pathWf;
    private final Rule dataWf;

    public QueryFilter(IRegExp<ITerm> pathWf, Rule dataConstraint) {
        this(RegExpMatcher.create(pathWf), dataConstraint);
    }

    private QueryFilter(IRegExpMatcher<ITerm> pathWf, Rule dataConstraint) {
        this.pathWf = pathWf;
        this.dataWf = dataConstraint;
    }

    @Override public IQueryFilter apply(ISubstitution.Immutable subst) {
        return new QueryFilter(pathWf, dataWf.apply(subst));
    }

    @Override public LabelWF<ITerm> getLabelWF(State state, IsComplete isComplete, IDebugContext debug) {
        return RegExpLabelWF.of(pathWf);
    }

    @Override public DataWF<ITerm> getDataWF(State state, IsComplete isComplete, IDebugContext debug) {
        return new ConstraintDataWF(dataWf, state, isComplete, debug);
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