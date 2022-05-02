package mb.statix.solver;

import java.util.Optional;

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

    Set.Immutable<ITermVar> getVars();

    Set.Immutable<ITermVar> freeVars();

    void visitFreeVars(Action1<ITermVar> onFreeVar);

    /**
     * Apply capture avoiding substitution.
     */
    IConstraint apply(ISubstitution.Immutable subst);

    /**
     * Apply unguarded substitution, which may result in capture.
     */
    IConstraint unsafeApply(ISubstitution.Immutable subst);

    /**
     * Apply variable renaming.
     */
    IConstraint apply(IRenaming subst);

    String toString(TermFormatter termToString);

    Tag constraintTag();

    enum Tag {
        CArith,
        CConj,
        CEqual,
        CExists,
        CFalse,
        CInequal,
        CNew,
        IResolveQuery,
        CTellEdge,
        CAstId,
        CAstProperty,
        CTrue,
        CTry,
        CUser
    }

}
