package mb.statix.solver.constraint;

import java.util.Optional;

import javax.annotation.Nullable;

import mb.nabl2.regexp.IRegExp;
import mb.nabl2.regexp.IRegExpMatcher;
import mb.nabl2.regexp.RegExpMatcher;
import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.solver.IConstraint;

public class CPathMatch implements IConstraint {

    private final IRegExpMatcher<ITerm> re;
    private final IListTerm labelsTerm;

    private final @Nullable IConstraint cause;

    public CPathMatch(IRegExp<ITerm> re, IListTerm labelsTerm) {
        this(RegExpMatcher.create(re), labelsTerm);
    }

    public CPathMatch(IRegExpMatcher<ITerm> re, IListTerm labelsTerm) {
        this(re, labelsTerm, null);
    }

    public CPathMatch(IRegExpMatcher<ITerm> re, IListTerm labelsTerm, @Nullable IConstraint cause) {
        this.re = re;
        this.labelsTerm = labelsTerm;
        this.cause = cause;
    }

    public IRegExpMatcher<ITerm> re() {
        return re;
    }

    public IListTerm labelsTerm() {
        return labelsTerm;
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CPathMatch withCause(@Nullable IConstraint cause) {
        return new CPathMatch(re, labelsTerm, cause);
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.casePathMatch(this);
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E {
        return cases.casePathMatch(this);
    }

    @Override public CPathMatch apply(ISubstitution.Immutable subst) {
        return new CPathMatch(re, (IListTerm) subst.apply(labelsTerm), cause);
    }

    @Override public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append("pathMatch[");
        sb.append(re);
        sb.append("](");
        sb.append(termToString.format(labelsTerm));
        sb.append(")");
        return sb.toString();
    }

    @Override public String toString() {
        return toString(ITerm::toString);
    }

}