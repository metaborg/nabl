package mb.nabl2.terms.matching;

import java.util.Arrays;
import java.util.List;

import org.metaborg.util.functions.CheckedFunction1;
import org.metaborg.util.functions.Function1;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.ITermVar;

public abstract class MaybeNotInstantiatedBool {

    private MaybeNotInstantiatedBool() {
    }

    public abstract <R> R match(ResultFunction<R> onResult, Function1<List<ITermVar>, R> onException);

    public abstract <R, E extends Throwable> R matchOrThrow(CheckedResultFunction<R, E> onResult,
            CheckedFunction1<List<ITermVar>, R, E> onException) throws E;

    public abstract MaybeNotInstantiatedBool map(ResultMapper f);

    public abstract void onResult(ResultAction f);

    public abstract MaybeNotInstantiatedBool flatMap(ResultFlatMapper f);

    public abstract boolean orElse(boolean defaultResult);

    public abstract <E extends Throwable> boolean orElseThrow(Function1<List<ITermVar>, E> provider) throws E;

    public static MaybeNotInstantiatedBool ofResult(boolean result) {
        return new OnResult(result);
    }

    public static MaybeNotInstantiatedBool ofNotInstantiated(ITermVar... vars) {
        return ofNotInstantiated(Arrays.asList(vars));
    }

    public static MaybeNotInstantiatedBool ofNotInstantiated(Iterable<? extends ITermVar> vars) {
        return new NotInstantiated(vars);
    }

    private static class OnResult extends MaybeNotInstantiatedBool {

        private final boolean result;

        private OnResult(boolean result) {
            this.result = result;
        }

        @Override public <R> R match(ResultFunction<R> onResult, Function1<List<ITermVar>, R> onException) {
            return onResult.apply(result);
        }

        @Override public <R, E extends Throwable> R matchOrThrow(CheckedResultFunction<R, E> onResult,
                CheckedFunction1<List<ITermVar>, R, E> onException) throws E {
            return onResult.apply(result);
        }

        @Override public MaybeNotInstantiatedBool map(ResultMapper f) {
            return new OnResult(f.apply(result));
        }

        @Override public void onResult(ResultAction f) {
            f.apply(result);
        }

        @Override public MaybeNotInstantiatedBool flatMap(ResultFlatMapper f) {
            return f.apply(result);
        }

        @Override public boolean orElse(boolean defaultResult) {
            return result;
        }

        @Override public <E extends Throwable> boolean orElseThrow(Function1<List<ITermVar>, E> provider) throws E {
            return result;
        }

        @Override public String toString() {
            return "result " + Boolean.toString(result);
        }

    }

    private static class NotInstantiated extends MaybeNotInstantiatedBool {

        private final List<ITermVar> vars;

        private NotInstantiated(Iterable<? extends ITermVar> vars) {
            this.vars = ImmutableList.copyOf(vars);
        }

        @Override public <R> R match(ResultFunction<R> onResult, Function1<List<ITermVar>, R> onException) {
            return onException.apply(vars);
        }

        @Override public <R, E extends Throwable> R matchOrThrow(CheckedResultFunction<R, E> onResult,
                CheckedFunction1<List<ITermVar>, R, E> onException) throws E {
            return onException.apply(vars);
        }

        @Override public MaybeNotInstantiatedBool map(ResultMapper f) {
            return new NotInstantiated(vars);
        }

        @Override public void onResult(ResultAction f) {
        }

        @Override public MaybeNotInstantiatedBool flatMap(ResultFlatMapper f) {
            return new NotInstantiated(vars);
        }

        @Override public boolean orElse(boolean defaultResult) {
            return defaultResult;
        }

        @Override public <E extends Throwable> boolean orElseThrow(Function1<List<ITermVar>, E> provider) throws E {
            throw provider.apply(vars);
        }

        @Override public String toString() {
            return "uninstantiated " + vars.toString();
        }

    }

    @FunctionalInterface
    public interface ResultFunction<R> {
        R apply(boolean t);
    }

    @FunctionalInterface
    public interface CheckedResultFunction<R, E extends Throwable> {
        R apply(boolean t) throws E;
    }

    @FunctionalInterface
    public interface ResultMapper {
        boolean apply(boolean t);
    }

    @FunctionalInterface
    public interface ResultFlatMapper {
        MaybeNotInstantiatedBool apply(boolean t);
    }

    @FunctionalInterface
    public interface ResultAction {
        void apply(boolean t);
    }
}