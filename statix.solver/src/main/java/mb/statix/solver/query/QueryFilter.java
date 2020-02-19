package mb.statix.solver.query;

import java.io.Serializable;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import mb.nabl2.regexp.IRegExp;
import mb.nabl2.regexp.IRegExpMatcher;
import mb.nabl2.regexp.RegExpMatcher;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
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

    @Override public IRegExpMatcher<ITerm> getLabelWF() {
        return pathWf;
    }

    @Override public Rule getDataWF() {
        return dataWf;
    }

    @Override public Set<ITermVar> boundVars() {
        return ImmutableSet.of();
    }

    @Override public Set<ITermVar> freeVars() {
        final ImmutableSet.Builder<ITermVar> freeVars = ImmutableSet.builder();
        freeVars.addAll(dataWf.freeVars());
        return freeVars.build();
    }

    @Override public IQueryFilter doSubstitute(IRenaming.Immutable localRenaming, ISubstitution.Immutable totalSubst) {
        return new QueryFilter(pathWf, dataWf.recSubstitute(totalSubst));
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
