package mb.statix.constraints;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.solver.IConstraint;

public class CUser implements IConstraint, Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final List<ITerm> args;

    private final @Nullable IConstraint cause;

    public CUser(String name, Iterable<? extends ITerm> args) {
        this(name, args, null);
    }

    public CUser(String name, Iterable<? extends ITerm> args, @Nullable IConstraint cause) {
        this.name = name;
        this.args = ImmutableList.copyOf(args);
        this.cause = cause;
    }

    public String name() {
        return name;
    }

    public List<ITerm> args() {
        return args;
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CUser withCause(@Nullable IConstraint cause) {
        return new CUser(name, args, cause);
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseUser(this);
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E {
        return cases.caseUser(this);
    }

    @Override public CUser apply(ISubstitution.Immutable subst) {
        return new CUser(name, subst.apply(args), cause);
    }

    @Override public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append(name);
        sb.append("(");
        sb.append(termToString.format(args));
        sb.append(")");
        return sb.toString();
    }

    @Override public String toString() {
        return toString(ITerm::toString);
    }

}
