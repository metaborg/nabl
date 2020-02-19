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

public class CTellRel implements IConstraint, Serializable {
    private static final long serialVersionUID = 1L;

    private final ITerm scopeTerm;
    private final ITerm relation;
    private final ITerm datumTerm;

    private final @Nullable IConstraint cause;

    public CTellRel(ITerm scopeTerm, ITerm relation, ITerm datumTerm) {
        this(scopeTerm, relation, datumTerm, null);
    }

    public CTellRel(ITerm scopeTerm, ITerm relation, ITerm datumTerm, @Nullable IConstraint cause) {
        this.scopeTerm = scopeTerm;
        this.relation = relation;
        this.datumTerm = datumTerm;
        this.cause = cause;
    }

    public ITerm scopeTerm() {
        return scopeTerm;
    }

    public ITerm relation() {
        return relation;
    }

    public ITerm datumTerm() {
        return datumTerm;
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CTellRel withCause(@Nullable IConstraint cause) {
        return new CTellRel(scopeTerm, relation, datumTerm, cause);
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseTellRel(this);
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E {
        return cases.caseTellRel(this);
    }

    @Override public Set<ITermVar> boundVars() {
        return ImmutableSet.of();
    }

    @Override public Set<ITermVar> freeVars() {
        final ImmutableSet.Builder<ITermVar> freeVars = ImmutableSet.builder();
        freeVars.addAll(scopeTerm.getVars());
        freeVars.addAll(datumTerm.getVars());
        return freeVars.build();
    }

    @Override public CTellRel doSubstitute(IRenaming.Immutable localRenaming, ISubstitution.Immutable totalSubst) {
        return new CTellRel(totalSubst.apply(scopeTerm), relation, totalSubst.apply(datumTerm));
    }

    @Override public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append(termToString.format(scopeTerm));
        sb.append(" -");
        sb.append(termToString.format(relation));
        sb.append("-[] ");
        sb.append(termToString.format(datumTerm));
        return sb.toString();
    }

    @Override public String toString() {
        return toString(ITerm::toString);
    }

}