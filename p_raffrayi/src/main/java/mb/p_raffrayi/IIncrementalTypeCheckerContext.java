package mb.p_raffrayi;

import java.util.Optional;

import org.metaborg.util.functions.Function1;
import org.metaborg.util.functions.Function2;
import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.IFuture;

import mb.scopegraph.patching.IPatchCollection;

public interface IIncrementalTypeCheckerContext<S, L, D, R, T> extends ITypeCheckerContext<S, L, D> {

    /**
     * Runs a given local type checker incrementally. When a type-checker uses incrementality, it should be implemented
     * in the following way: First, it should synchronously create all shared scopes, and start all its subunits. After
     * that, {@link runIncremental} should be called with the appropriate callbacks.
     *
     * The callbacks are designed in such a way that the initial result of a particular type checker can be reused
     * whenever the unit itself was not required to (re)start. This holds regardless of the state of its subunits. We
     * assume that the type-checker result {@link R} is an aggregation of the local result {@link Q} of the type-checker
     * and the results of its subunits. we allow to split these values with {@code extractLocal}, and later to be
     * recombined with either updated or fresh results using {@code combine}.
     *
     * @implSpec In summary, when the type-checker result can be reused with patches {@code p},
     *           {@code combine(patch(extractLocal(previousResult), p), null)} is used to obtain the updated result.
     *           Otherwise, {@code combine(runLocalTypeChecker(restarted), null)} is used to compute the new result.
     *
     * @param <Q>
     *            The type of the local type-checker result
     *
     * @param runLocalTypeChecker
     *            The callback that should start the type checker. If present, the argument provides the local snapshot
     *            of the state taken in a previous incremental run. If the result could be reused, the callback is not
     *            invoked at all. Instead, the {@code patch} callback will be invoked to update it.
     *
     * @param extractLocal
     *            Callback that should extract the local type-checker result {@link Q} from an aggregated result
     *            {@link R}
     *
     * @param patch
     *            Callback that should update a previous result with the supplied scope patches.
     *
     * @param combine
     *            Callback that should combine the local result (which is either fresh or updated from a previous run)
     *            with the results of its sub-units. The futures of this subunit should be captured in the callback
     *            implementation.
     */
    <Q> IFuture<R> runIncremental(Function1<Optional<T>, IFuture<Q>> runLocalTypeChecker, Function1<R, Q> extractLocal,
            Function2<Q, IPatchCollection.Immutable<S>, Q> patch, Function2<Q, Throwable, IFuture<R>> combine);

    /**
     * Default {@link runIncremental} implementation that applies no scope patching.
     */
    default <Q> IFuture<R> runIncremental(Function1<Optional<T>, IFuture<Q>> runLocalTypeChecker,
            Function1<R, Q> extractLocal, Function2<Q, Throwable, IFuture<R>> combine) {
        return this.runIncremental(runLocalTypeChecker, extractLocal, (x, p) -> x, combine);
    }

    /**
     * Default {@link runIncremental} implementation that applies no scope patching nor subunit result aggregation.
     */
    default IFuture<R> runIncremental(Function1<Optional<T>, IFuture<R>> runLocalTypeChecker) {
        return this.runIncremental(runLocalTypeChecker, x -> x, CompletableFuture::completed);
    }

}
