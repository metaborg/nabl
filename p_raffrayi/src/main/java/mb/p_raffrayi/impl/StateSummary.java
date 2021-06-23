package mb.p_raffrayi.impl;

import org.metaborg.util.functions.Action0;
import org.metaborg.util.functions.Action1;
import org.metaborg.util.functions.Function0;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.unit.Unit;

import mb.scopegraph.oopsla20.diff.BiMap;

public abstract class StateSummary<S> {

    @SuppressWarnings("rawtypes") private static StateSummary RESTART = new Restart<>();
    @SuppressWarnings("rawtypes") private static StateSummary EMPTY_RELEASE = new Release<>(BiMap.Immutable.of());
    @SuppressWarnings("rawtypes") private static StateSummary EMPTY_RELEASED = new Released<>(BiMap.Immutable.of());

    private StateSummary() {
    }

    public abstract <T> T match(Function0<T> onRestart, Function1<BiMap.Immutable<S>, T> onRelease,
            Function1<BiMap.Immutable<S>, T> onReleased);

    public void accept(Action0 onRestart, Action1<BiMap.Immutable<S>> onRelease,
            Action1<BiMap.Immutable<S>> onReleased) {
        this.match(() -> {
            onRestart.apply();
            return Unit.unit;
        }, ptcs -> {
            onRelease.apply(ptcs);
            return Unit.unit;
        }, ptcs -> {
            onReleased.apply(ptcs);
            return Unit.unit;
        });
    }

    public abstract StateSummary<S> combine(StateSummary<S> other);

    @SuppressWarnings("unchecked") public static <S> StateSummary<S> restart() {
        return RESTART;
    }

    @SuppressWarnings("unchecked") public static <S> StateSummary<S> release(BiMap.Immutable<S> patches) {
        if(patches.isEmpty()) {
            return EMPTY_RELEASE;
        }
        return new Release<>(patches);
    }

    @SuppressWarnings("unchecked") public static <S> StateSummary<S> released(BiMap.Immutable<S> patches) {
        if(patches.isEmpty()) {
            return EMPTY_RELEASED;
        }
        return new Released<>(patches);
    }

    @SuppressWarnings("unchecked") public static <S> StateSummary<S> release() {
        return EMPTY_RELEASE;
    }

    @SuppressWarnings("unchecked") public static <S> StateSummary<S> released() {
        return EMPTY_RELEASED;
    }

    private static class Restart<S> extends StateSummary<S> {

        @Override public <T> T match(Function0<T> onRestart, Function1<BiMap.Immutable<S>, T> onRelease,
                Function1<BiMap.Immutable<S>, T> onReleased) {
            return onRestart.apply();
        }

        @Override public StateSummary<S> combine(StateSummary<S> other) {
            return this;
        }

        @Override public String toString() {
            return "Restart{}";
        }

    }

    private static class Release<S> extends StateSummary<S> {

        private BiMap.Immutable<S> patches;

        Release(BiMap.Immutable<S> patches) {
            this.patches = patches;
        }

        @Override public <T> T match(Function0<T> onRestart, Function1<BiMap.Immutable<S>, T> onRelease,
                Function1<BiMap.Immutable<S>, T> onReleased) {
            return onRelease.apply(patches);
        }

        @Override public StateSummary<S> combine(StateSummary<S> other) {
            // @formatter:off
            return other.match(
                () -> restart(),
                ptcs -> release(patches.putAll(ptcs)),
                ptcs -> release(patches.putAll(ptcs))
            );
            // @formatter:on
        }

        @Override public String toString() {
            return "Release{" + patches + "}";
        }

    }

    private static class Released<S> extends StateSummary<S> {

        private BiMap.Immutable<S> patches;

        Released(BiMap.Immutable<S> patches) {
            this.patches = patches;
        }

        @Override public <T> T match(Function0<T> onRestart, Function1<BiMap.Immutable<S>, T> onRelease,
                Function1<BiMap.Immutable<S>, T> onReleased) {
            return onReleased.apply(patches);
        }

        @Override public StateSummary<S> combine(StateSummary<S> other) {
            // @formatter:off
            return other.match(
                () -> restart(),
                ptcs -> release(patches.putAll(ptcs)),
                ptcs -> released(patches.putAll(ptcs))
            );
            // @formatter:on
        }

        @Override public String toString() {
            return "Released{" + patches + "}";
        }

    }

}
