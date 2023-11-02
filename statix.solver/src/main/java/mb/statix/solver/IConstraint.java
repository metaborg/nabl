package mb.statix.solver;

import java.util.Optional;

import jakarta.annotation.Nullable;
import org.metaborg.util.functions.Action1;
import org.metaborg.util.functions.CheckedFunction1;
import org.metaborg.util.functions.Function1;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.constraints.CArith;
import mb.statix.constraints.CAstId;
import mb.statix.constraints.CAstProperty;
import mb.statix.constraints.CConj;
import mb.statix.constraints.CEqual;
import mb.statix.constraints.CExists;
import mb.statix.constraints.CFalse;
import mb.statix.constraints.CInequal;
import mb.statix.constraints.CNew;
import mb.statix.constraints.CTellEdge;
import mb.statix.constraints.CTrue;
import mb.statix.constraints.CTry;
import mb.statix.constraints.CUser;
import mb.statix.constraints.IResolveQuery;
import mb.statix.constraints.messages.IMessage;
import mb.statix.solver.completeness.Completeness;
import mb.statix.solver.completeness.ICompleteness;

public interface IConstraint {

    Optional<IConstraint> cause();

    IConstraint withCause(IConstraint cause);

    default Optional<IMessage> message() {
        return Optional.empty();
    }

    /**
     * The syntactic constraint from which this instance was derived.
     *
     * @return a constraint of the same type; or {@code null} if this information was not (yet) recorded
     */
    @Nullable IConstraint origin();

    default IConstraint withMessage(@SuppressWarnings("unused") IMessage msg) {
        throw new UnsupportedOperationException("Constraint does not support message.");
    }

    /**
     * Returns pre-computed critical edges that are introduced when this constraint is unfolded to its sub-constraints.
     */
    default Optional<ICompleteness.Immutable> ownCriticalEdges() {
        return Optional.of(Completeness.Immutable.of());
    }

    default IConstraint withOwnCriticalEdges(@SuppressWarnings("unused") ICompleteness.Immutable criticalEdges) {
        throw new UnsupportedOperationException("Constraint does not support own critical edges.");
    }

    /**
     * Returns pre-computed critical edges that are introduced when this constraint is unfolded to its sub-constraints.
     */
    default Optional<ICompleteness.Immutable> bodyCriticalEdges() {
        return Optional.of(Completeness.Immutable.of());
    }

    default IConstraint withBodyCriticalEdges(@SuppressWarnings("unused") ICompleteness.Immutable criticalEdges) {
        throw new UnsupportedOperationException("Constraint does not support body critical edges.");
    }

    <R> R match(Cases<R> cases);

    <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E;

    Set.Immutable<ITermVar> getVars();

    Set.Immutable<ITermVar> freeVars();

    void visitFreeVars(Action1<ITermVar> onFreeVar);

    /**
     * Apply capture avoiding substitution.
     *
     * @param subst the substitution to apply
     */
    IConstraint apply(ISubstitution.Immutable subst);

    /**
     * Apply unguarded substitution, which may result in capture.
     *
     * @param subst the substitution to apply
     */
    IConstraint unsafeApply(ISubstitution.Immutable subst);

    /**
     * Apply variable renaming.
     *
     * @param subst the substitution to apply
     */
    IConstraint apply(IRenaming subst);

    /**
     * Apply capture avoiding substitution.
     *
     * @param subst the substitution to apply
     * @param trackOrigin whether to use the current constraint as the syntactic {@link #origin()}
     *                    of the resulting constraint, if not already tracked
     */
    IConstraint apply(ISubstitution.Immutable subst, boolean trackOrigin);

    /**
     * Apply unguarded substitution, which may result in capture.
     *
     * @param subst the substitution to apply
     * @param trackOrigin whether to use the current constraint as the syntactic {@link #origin()}
     *                    of the resulting constraint, if not already tracked
     */
    IConstraint unsafeApply(ISubstitution.Immutable subst, boolean trackOrigin);

    /**
     * Apply variable renaming.
     *
     * @param subst the substitution to apply
     * @param trackOrigin whether to use the current constraint as the syntactic {@link #origin()}
     *                    of the resulting constraint, if not already tracked
     */
    IConstraint apply(IRenaming subst, boolean trackOrigin);

    String toString(TermFormatter termToString);

    interface Cases<R> extends Function1<IConstraint, R> {

        R caseArith(CArith c);

        R caseConj(CConj c);

        R caseEqual(CEqual c);

        R caseExists(CExists c);

        R caseFalse(CFalse c);

        R caseInequal(CInequal c);

        R caseNew(CNew c);

        R caseResolveQuery(IResolveQuery c);

        R caseTellEdge(CTellEdge c);

        R caseTermId(CAstId c);

        R caseTermProperty(CAstProperty c);

        R caseTrue(CTrue c);

        R caseTry(CTry c);

        R caseUser(CUser c);

        @Override default R apply(IConstraint c) {
            return c.match(this);
        }

    }

    interface CheckedCases<R, E extends Throwable> extends CheckedFunction1<IConstraint, R, E> {

        R caseArith(CArith c) throws E;

        R caseConj(CConj c) throws E;

        R caseEqual(CEqual c) throws E;

        R caseExists(CExists c) throws E;

        R caseFalse(CFalse c) throws E;

        R caseInequal(CInequal c) throws E;

        R caseNew(CNew c) throws E;

        R caseResolveQuery(IResolveQuery c) throws E;

        R caseTellEdge(CTellEdge c) throws E;

        R caseTermId(CAstId c) throws E;

        R caseTermProperty(CAstProperty c) throws E;

        R caseTrue(CTrue c) throws E;

        R caseTry(CTry c) throws E;

        R caseUser(CUser c) throws E;

        @Override default R apply(IConstraint c) throws E {
            return c.matchOrThrow(this);
        }

    }

}
