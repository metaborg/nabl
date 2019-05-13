package mb.statix.solver;

import java.util.Optional;

import org.metaborg.util.functions.CheckedFunction1;
import org.metaborg.util.functions.Function1;

import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.solver.constraint.CConj;
import mb.statix.solver.constraint.CEqual;
import mb.statix.solver.constraint.CExists;
import mb.statix.solver.constraint.CFalse;
import mb.statix.solver.constraint.CInequal;
import mb.statix.solver.constraint.CNew;
import mb.statix.solver.constraint.CPathLt;
import mb.statix.solver.constraint.CPathMatch;
import mb.statix.solver.constraint.CResolveQuery;
import mb.statix.solver.constraint.CTellEdge;
import mb.statix.solver.constraint.CTellRel;
import mb.statix.solver.constraint.CTermId;
import mb.statix.solver.constraint.CTermProperty;
import mb.statix.solver.constraint.CTrue;
import mb.statix.solver.constraint.CUser;

public interface IConstraint {

    Optional<IConstraint> cause();

    IConstraint withCause(IConstraint cause);

    <R> R match(Cases<R> cases);

    <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E;

    IConstraint apply(ISubstitution.Immutable subst);

    String toString(TermFormatter termToString);

    interface Cases<R> extends Function1<IConstraint, R> {

        R caseConj(CConj c);

        R caseEqual(CEqual c);

        R caseExists(CExists c);

        R caseFalse(CFalse c);

        R caseInequal(CInequal c);

        R caseNew(CNew c);

        R casePathLt(CPathLt c);

        R casePathMatch(CPathMatch c);

        R caseResolveQuery(CResolveQuery c);

        R caseTellEdge(CTellEdge c);

        R caseTellRel(CTellRel c);

        R caseTermId(CTermId c);

        R caseTermProperty(CTermProperty c);

        R caseTrue(CTrue c);

        R caseUser(CUser c);

        @Override default R apply(IConstraint c) {
            return c.match(this);
        }

    }

    interface CheckedCases<R, E extends Throwable> extends CheckedFunction1<IConstraint, R, E> {

        R caseConj(CConj c) throws E;

        R caseEqual(CEqual c) throws E;

        R caseExists(CExists c) throws E;

        R caseFalse(CFalse c) throws E;

        R caseInequal(CInequal c) throws E;

        R caseNew(CNew c) throws E;

        R casePathLt(CPathLt c) throws E;

        R casePathMatch(CPathMatch c) throws E;

        R caseResolveQuery(CResolveQuery c) throws E;

        R caseTellEdge(CTellEdge c) throws E;

        R caseTellRel(CTellRel c) throws E;

        R caseTermId(CTermId c) throws E;

        R caseTermProperty(CTermProperty c) throws E;

        R caseTrue(CTrue c) throws E;

        R caseUser(CUser c) throws E;

        @Override default R apply(IConstraint c) throws E {
            return c.matchOrThrow(this);
        }

    }

}