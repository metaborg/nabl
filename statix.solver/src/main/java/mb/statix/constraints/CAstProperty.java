package mb.statix.constraints;

import java.io.Serializable;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.solver.IConstraint;

public class CAstProperty implements IConstraint, Serializable {
    private static final long serialVersionUID = 1L;

    public static enum Op {
        SET {
            @Override public String toString() {
                return ":=";
            }
        },
        ADD {
            @Override public String toString() {
                return "+=";
            }
        }
    }

    private final ITerm idTerm;
    private final ITerm property;
    private final Op op;
    private final ITerm value;

    private final @Nullable IConstraint cause;

    public CAstProperty(ITerm idTerm, ITerm property, Op op, ITerm value) {
        this(idTerm, property, op, value, null);
    }

    public CAstProperty(ITerm idTerm, ITerm property, Op op, ITerm value, @Nullable IConstraint cause) {
        this.idTerm = idTerm;
        this.property = property;
        this.op = op;
        this.value = value;
        this.cause = cause;
    }

    public ITerm idTerm() {
        return idTerm;
    }

    public ITerm property() {
        return property;
    }

    public Op op() {
        return op;
    }

    public ITerm value() {
        return value;
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CAstProperty withCause(@Nullable IConstraint cause) {
        return new CAstProperty(idTerm, property, op, value, cause);
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseTermProperty(this);
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E {
        return cases.caseTermProperty(this);
    }

    @Override public Set<ITermVar> boundVars() {
        return ImmutableSet.of();
    }

    @Override public Set<ITermVar> freeVars() {
        final ImmutableSet.Builder<ITermVar> freeVars = ImmutableSet.builder();
        freeVars.addAll(idTerm.getVars());
        freeVars.addAll(value.getVars());
        return freeVars.build();
    }

    @Override public CAstProperty doSubstitute(IRenaming.Immutable localRenaming, ISubstitution.Immutable totalSubst) {
        return new CAstProperty(totalSubst.apply(idTerm), property, op, totalSubst.apply(value), cause);
    }

    @Override public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append("@");
        sb.append(termToString.format(idTerm));
        sb.append(".");
        sb.append(property.toString());
        sb.append(" ").append(op).append(" ");
        sb.append(termToString.format(value));
        return sb.toString();
    }

    @Override public String toString() {
        return toString(ITerm::toString);
    }

}
