package mb.statix.constraints;

import org.metaborg.util.functions.CheckedFunction1;
import org.metaborg.util.functions.Function1;

import mb.nabl2.terms.ITerm;
import mb.statix.solver.IConstraint;
import mb.statix.solver.query.QueryFilter;
import mb.statix.solver.query.QueryMin;

public interface IResolveQuery extends IConstraint {

    QueryFilter filter();

    QueryMin min();

    ITerm scopeTerm();

    ITerm resultTerm();

    <R> R match(Cases<R> cases);

    <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E;

    @Override default <R> R match(IConstraint.Cases<R> cases) {
        return cases.caseResolveQuery(this);
    }

    @Override default <R, E extends Throwable> R matchOrThrow(IConstraint.CheckedCases<R, E> cases) throws E {
        return cases.caseResolveQuery(this);
    }

    interface Cases<R> extends Function1<IResolveQuery, R> {

        R caseResolveQuery(CResolveQuery q);

        R caseCompiledQuery(CCompiledQuery q);

        @Override default R apply(IResolveQuery q) {
            return q.match(this);
        }

    }

    interface CheckedCases<R, E extends Throwable> extends CheckedFunction1<IResolveQuery, R, E> {

        R caseResolveQuery(CResolveQuery q) throws E;

        R caseCompiledQuery(CCompiledQuery q) throws E;

        @Override default R apply(IResolveQuery q) throws E {
            return q.matchOrThrow(this);
        }

    }

}
