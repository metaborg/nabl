package mb.statix.constraints;

import org.metaborg.util.functions.Function1;

import mb.nabl2.terms.ITerm;
import mb.scopegraph.oopsla20.reference.ResolutionException;
import mb.statix.solver.IConstraint;
import mb.statix.solver.query.QueryFilter;
import mb.statix.solver.query.QueryMin;
import mb.statix.solver.query.QueryProject;

public interface IResolveQuery extends IConstraint {

    QueryFilter filter();

    QueryMin min();

    QueryProject project();

    ITerm scopeTerm();

    ITerm resultTerm();

    <R> R match(Cases<R> cases);

    <R, E extends Throwable> R matchInResolution(
            ResolutionFunction1<CResolveQuery, R> onResolveQuery,
            ResolutionFunction1<CCompiledQuery, R> onCompiledQuery
    ) throws ResolutionException, InterruptedException;

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

    interface ResolutionFunction1<T, R> {
        R apply(T t) throws ResolutionException, InterruptedException;
    }

}
