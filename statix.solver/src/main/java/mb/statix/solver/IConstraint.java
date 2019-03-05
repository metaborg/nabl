package mb.statix.solver;

import java.util.Optional;

import org.metaborg.util.functions.CheckedFunction1;
import org.metaborg.util.functions.Function1;

import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.solver.constraint.CEqual;
import mb.statix.solver.constraint.CFalse;
import mb.statix.solver.constraint.CInequal;
import mb.statix.solver.constraint.CNew;
import mb.statix.solver.constraint.CPathDst;
import mb.statix.solver.constraint.CPathLabels;
import mb.statix.solver.constraint.CPathLt;
import mb.statix.solver.constraint.CPathMatch;
import mb.statix.solver.constraint.CPathScopes;
import mb.statix.solver.constraint.CPathSrc;
import mb.statix.solver.constraint.CResolveQuery;
import mb.statix.solver.constraint.CTellEdge;
import mb.statix.solver.constraint.CTellRel;
import mb.statix.solver.constraint.CTermId;
import mb.statix.solver.constraint.CTrue;
import mb.statix.solver.constraint.CUser;

public interface IConstraint {

    Optional<IConstraint> cause();

    IConstraint withCause(IConstraint cause);

    <R> R match(Cases<R> cases);

    <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E;

    IConstraint apply(ISubstitution.Immutable subst);

    String toString(TermFormatter termToString);

    static String toString(Iterable<? extends IConstraint> constraints, TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for(IConstraint constraint : constraints) {
            if(!first) {
                sb.append(", ");
            }
            first = false;
            sb.append(constraint.toString(termToString));
        }
        return sb.toString();
    }

    interface Cases<R> extends Function1<IConstraint, R> {

        R caseEqual(CEqual c);

        R caseFalse(CFalse c);

        R caseInequal(CInequal c);

        R caseNew(CNew c);

        R casePathDst(CPathDst c);

        R casePathLabels(CPathLabels c);

        R casePathLt(CPathLt c);

        R casePathMatch(CPathMatch c);

        R casePathScopes(CPathScopes c);

        R casePathSrc(CPathSrc c);

        R caseResolveQuery(CResolveQuery c);

        R caseTellEdge(CTellEdge c);

        R caseTellRel(CTellRel c);

        R caseTermId(CTermId c);

        R caseTrue(CTrue c);

        R caseUser(CUser c);

        @Override default R apply(IConstraint c) {
            return c.match(this);
        }

    }

    interface CheckedCases<R, E extends Throwable> extends CheckedFunction1<IConstraint, R, E> {

        R caseEqual(CEqual c) throws E;

        R caseFalse(CFalse c) throws E;

        R caseInequal(CInequal c) throws E;

        R caseNew(CNew c) throws E;

        R casePathDst(CPathDst c) throws E;

        R casePathLabels(CPathLabels c) throws E;

        R casePathLt(CPathLt c) throws E;

        R casePathMatch(CPathMatch c) throws E;

        R casePathScopes(CPathScopes c) throws E;

        R casePathSrc(CPathSrc c) throws E;

        R caseResolveQuery(CResolveQuery c) throws E;

        R caseTellEdge(CTellEdge c) throws E;

        R caseTellRel(CTellRel c) throws E;

        R caseTermId(CTermId c) throws E;

        R caseTrue(CTrue c) throws E;

        R caseUser(CUser c) throws E;

        @Override default R apply(IConstraint c) throws E {
            return c.matchOrThrow(this);
        }

    }

}