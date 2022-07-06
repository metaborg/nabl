package mb.statix.constraints;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.functions.Action1;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.solver.IConstraint;

public class CAstId implements IConstraint, Serializable {
    private static final long serialVersionUID = 1L;

    private final ITerm term;
    private final ITerm idTerm;

    private final @Nullable IConstraint cause;

    public CAstId(ITerm term, ITerm idTerm) {
        this(term, idTerm, null);
    }

    public CAstId(ITerm term, ITerm idTerm, @Nullable IConstraint cause) {
        this.term = term;
        this.idTerm = idTerm;
        this.cause = cause;
    }

    public ITerm astTerm() {
        return term;
    }

    public ITerm idTerm() {
        return idTerm;
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CAstId withCause(@Nullable IConstraint cause) {
        return new CAstId(term, idTerm, cause);
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseTermId(this);
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E {
        return cases.caseTermId(this);
    }

    @Override public Set.Immutable<ITermVar> getVars() {
        return Set.Immutable.union(
            term.getVars(),
            idTerm.getVars()
        );
    }

    @Override public Set.Immutable<ITermVar> freeVars() {
        Set.Transient<ITermVar> freeVars = CapsuleUtil.transientSet();
        doVisitFreeVars(freeVars::__insert);
        return freeVars.freeze();
    }

    @Override public void visitFreeVars(Action1<ITermVar> onFreeVar) {
        doVisitFreeVars(onFreeVar);
    }

    private void doVisitFreeVars(Action1<ITermVar> onFreeVar) {
        term.getVars().forEach(onFreeVar::apply);
        idTerm.getVars().forEach(onFreeVar::apply);
    }

    @Override public CAstId apply(ISubstitution.Immutable subst) {
        return new CAstId(subst.apply(term), subst.apply(idTerm), cause);
    }

    @Override public CAstId unsafeApply(ISubstitution.Immutable subst) {
        return new CAstId(subst.apply(term), subst.apply(idTerm), cause);
    }

    @Override public CAstId apply(IRenaming subst) {
        return new CAstId(subst.apply(term), subst.apply(idTerm), cause);
    }

    @Override public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append("termId(");
        sb.append(termToString.format(term));
        sb.append(", ");
        sb.append(termToString.format(idTerm));
        sb.append(")");
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
        CAstId cAstId = (CAstId) o;
        return Objects.equals(term, cAstId.term) && Objects.equals(idTerm, cAstId.idTerm)
                && Objects.equals(cause, cAstId.cause);
    }

    private volatile int hashCode;

    @Override public int hashCode() {
        int result = hashCode;
        if(result == 0) {
            result = Objects.hash(term, idTerm, cause);
            hashCode = result;
        }
        return result;
    }

}
