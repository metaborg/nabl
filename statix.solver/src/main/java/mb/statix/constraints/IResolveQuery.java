package mb.statix.constraints;

import org.metaborg.util.functions.Function1;

import mb.nabl2.terms.ITerm;
import mb.scopegraph.oopsla20.reference.ResolutionException;
import mb.statix.solver.IConstraint;
import mb.statix.solver.query.QueryFilter;
import mb.statix.solver.query.QueryMin;

public interface IResolveQuery extends IConstraint {

    QueryFilter filter();

    QueryMin min();

    ITerm scopeTerm();

    ITerm resultTerm();

    <R, E extends Throwable> R matchInResolution(ResolutionFunction1<CResolveQuery, R> onResolveQuery,
            ResolutionFunction1<CCompiledQuery, R> onCompiledQuery) throws ResolutionException, InterruptedException;

    interface ResolutionFunction1<T, R> {
        R apply(T t) throws ResolutionException, InterruptedException;
    }

    @Override default IConstraint.Tag constraintTag() {
        return IConstraint.Tag.IResolveQuery;
    }

    enum Tag {
        CResolveQuery,
        CCompiledQuery
    }

    Tag resolveQueryTag();
}
