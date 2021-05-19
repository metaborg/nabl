package mb.p_raffrayi;

import org.metaborg.util.functions.Function1;
import org.metaborg.util.functions.Function2;
import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.IFuture;

import mb.scopegraph.oopsla20.diff.BiMap;

public interface IIncrementalTypeCheckerContext<S, L, D, R> extends ITypeCheckerContext<S, L, D> {

    /**
     * Runs a given local type checker incrementally. The boolean argument indicates whether the type checker was
     * restarted (true) or started because there was no earlier result (false). If the result could be reused, the
     * callback is not invoked at all.
     */
    <Q> IFuture<R> runIncremental(Function1<Boolean, IFuture<Q>> runLocalTypeChecker, Function1<R, Q> extractLocal,
            Function2<Q, BiMap.Immutable<S>, Q> patch, Function2<Q, Throwable, IFuture<R>> combine);

    default <Q> IFuture<R> runIncremental(Function1<Boolean, IFuture<Q>> runLocalTypeChecker, Function1<R, Q> extractLocal,
            Function2<Q, Throwable, IFuture<R>> combine) {
        return this.runIncremental(runLocalTypeChecker, extractLocal, (x, p) -> x, combine);
    }

    default IFuture<R> runIncremental(Function1<Boolean, IFuture<R>> runLocalTypeChecker) {
        return this.runIncremental(runLocalTypeChecker, x -> x, CompletableFuture::completed);
    }


}
