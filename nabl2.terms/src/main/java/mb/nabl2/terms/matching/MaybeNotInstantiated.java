package mb.nabl2.terms.matching;

import java.util.Arrays;
import java.util.List;

import org.metaborg.util.functions.Action1;
import org.metaborg.util.functions.CheckedFunction1;
import org.metaborg.util.functions.Function1;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.ITermVar;

public abstract class MaybeNotInstantiated<T> {

    private MaybeNotInstantiated() {
    }

    public abstract <R> R match(Function1<T, R> onResult, Function1<List<ITermVar>, R> onException);

    public abstract <R, E extends Throwable> R matchOrThrow(CheckedFunction1<T, R, E> onResult,
            CheckedFunction1<List<ITermVar>, R, E> onException) throws E;

    public abstract <R> MaybeNotInstantiated<R> map(Function1<T, R> f);

    public abstract void onResult(Action1<T> f);

    public abstract <R> MaybeNotInstantiated<R> flatMap(Function1<T, MaybeNotInstantiated<R>> f);

    public abstract T orElse(T defaultResult);

    public abstract <E extends Throwable> T orElseThrow(Function1<List<ITermVar>, E> provider) throws E;

    public static <T> MaybeNotInstantiated<T> ofResult(T result) {
        return new OnResult<>(result);
    }

    public static <T> MaybeNotInstantiated<T> ofNotInstantiated(ITermVar... vars) {
        return ofNotInstantiated(Arrays.asList(vars));
    }

    public static <T> MaybeNotInstantiated<T> ofNotInstantiated(Iterable<? extends ITermVar> vars) {
        return new NotInstantiated<>(vars);
    }

    private static class OnResult<T> extends MaybeNotInstantiated<T> {

        private final T result;

        private OnResult(T result) {
            this.result = result;
        }

        @Override public <R> R match(Function1<T, R> onResult, Function1<List<ITermVar>, R> onException) {
            return onResult.apply(result);
        }

        @Override public <R, E extends Throwable> R matchOrThrow(CheckedFunction1<T, R, E> onResult,
                CheckedFunction1<List<ITermVar>, R, E> onException) throws E {
            return onResult.apply(result);
        }

        @Override public <R> MaybeNotInstantiated<R> map(Function1<T, R> f) {
            return new OnResult<>(f.apply(result));
        }

        @Override public void onResult(Action1<T> f) {
            f.apply(result);
        }

        @Override public <R> MaybeNotInstantiated<R> flatMap(Function1<T, MaybeNotInstantiated<R>> f) {
            return f.apply(result);
        }

        @Override public T orElse(T defaultResult) {
            return result;
        }

        @Override public <E extends Throwable> T orElseThrow(Function1<List<ITermVar>, E> provider) throws E {
            return result;
        }

        @Override public String toString() {
            return "result " + result.toString();
        }

    }

    private static class NotInstantiated<T> extends MaybeNotInstantiated<T> {

        private final List<ITermVar> vars;

        private NotInstantiated(Iterable<? extends ITermVar> vars) {
            this.vars = ImmutableList.copyOf(vars);
        }

        @Override public <R> R match(Function1<T, R> onResult, Function1<List<ITermVar>, R> onException) {
            return onException.apply(vars);
        }

        @Override public <R, E extends Throwable> R matchOrThrow(CheckedFunction1<T, R, E> onResult,
                CheckedFunction1<List<ITermVar>, R, E> onException) throws E {
            return onException.apply(vars);
        }

        @Override public <R> MaybeNotInstantiated<R> map(Function1<T, R> f) {
            return new NotInstantiated<>(vars);
        }

        @Override public void onResult(Action1<T> f) {
        }

        @Override public <R> MaybeNotInstantiated<R> flatMap(Function1<T, MaybeNotInstantiated<R>> f) {
            return new NotInstantiated<>(vars);
        }

        @Override public T orElse(T defaultResult) {
            return defaultResult;
        }

        @Override public <E extends Throwable> T orElseThrow(Function1<List<ITermVar>, E> provider) throws E {
            throw provider.apply(vars);
        }

        @Override public String toString() {
            return "uninstantiated " + vars.toString();
        }

    }

}