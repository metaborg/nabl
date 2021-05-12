package mb.p_raffrayi.impl;

import org.metaborg.util.functions.Action0;
import org.metaborg.util.functions.Action1;
import org.metaborg.util.functions.Function0;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.unit.Unit;

import mb.scopegraph.oopsla20.diff.BiMap;

public abstract class ReleaseOrRestart<S> {

    @SuppressWarnings("rawtypes") private static ReleaseOrRestart RESTART = new Restart<>();

    private ReleaseOrRestart() {
    }

    public abstract <T> T match(Function0<T> onRestart, Function1<BiMap.Immutable<S>, T> onRelease);

    public void accept(Action0 onRestart, Action1<BiMap.Immutable<S>> onRelease) {
        this.match(() -> {
            onRestart.apply();
            return Unit.unit;
        }, ptcs -> {
            onRelease.apply(ptcs);
            return Unit.unit;
        });
    }

    public abstract ReleaseOrRestart<S> combine(ReleaseOrRestart<S> other);

    public boolean isRelease() {
        return this.match(() -> false, __ -> true);
    }

    public boolean isRestart() {
        return this.match(() -> true, __ -> false);
    }

    @SuppressWarnings("unchecked") public static <S> ReleaseOrRestart<S> restart() {
        return RESTART;
    }

    public static <S> ReleaseOrRestart<S> release(BiMap.Immutable<S> patches) {
        return new Release<>(patches);
    }

    private static class Restart<S> extends ReleaseOrRestart<S> {

        @Override public <T> T match(Function0<T> onRestart, Function1<BiMap.Immutable<S>, T> onRelease) {
            return onRestart.apply();
        }

        @Override public ReleaseOrRestart<S> combine(ReleaseOrRestart<S> other) {
            return this;
        }

        @Override public String toString() {
            return "Restart{}";
        }

    }

    private static class Release<S> extends ReleaseOrRestart<S> {

        private BiMap.Immutable<S> patches;

        Release(BiMap.Immutable<S> patches) {
            this.patches = patches;
        }

        @Override public <T> T match(Function0<T> onRestart, Function1<BiMap.Immutable<S>, T> onRelease) {
            return onRelease.apply(patches);
        }

        @Override public ReleaseOrRestart<S> combine(ReleaseOrRestart<S> other) {
            // @formatter:off
            return other.match(
                () -> restart(),
                ptcs -> release(patches.putAll(ptcs))
            );
            // @formatter:on
        }

        @Override public String toString() {
            return "Release{" + patches + "}";
        }

    }

}
