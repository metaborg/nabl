package mb.statix.concurrent.p_raffrayi;

import org.metaborg.util.functions.Function1;
import org.metaborg.util.functions.Function2;

import mb.statix.concurrent.actors.futures.IFuture;

public interface IIncrementalTypeCheckerContext<S, L, D, R> extends ITypeCheckerContext<S, L, D> {

    /**
     * Runs a given local type checker incrementally. The boolean argument indicates whether the type checker was
     * restarted (true) or started because there was no earlier result (false). If the result could be reused, the
     * callback is not invoked at all.
     */
    <Q> IFuture<R> runIncremental(Function1<Boolean, IFuture<Q>> runLocalTypeChecker, Function1<R, Q> extractLocal,
            Function2<Q, Throwable, IFuture<R>> combine);
    
}
