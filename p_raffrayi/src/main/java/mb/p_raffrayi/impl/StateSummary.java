package mb.p_raffrayi.impl;

import java.util.Set;

import org.metaborg.util.functions.Action0;
import org.metaborg.util.functions.Action1;
import org.metaborg.util.functions.Function0;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.unit.Unit;

import com.google.common.collect.ImmutableSet;

import mb.scopegraph.patching.IPatchCollection;
import mb.scopegraph.patching.PatchCollection;

public abstract class StateSummary<S, L, D> {

    private final IProcess<S, L, D> self;
    private final ImmutableSet<IProcess<S, L, D>> dependencies;

    private StateSummary(IProcess<S, L, D> self, Set<IProcess<S, L, D>> dependencies) {
        this.self = self;
        this.dependencies = ImmutableSet.copyOf(dependencies);
    }

    public abstract <T> T match(Function0<T> onRestart, Function1<IPatchCollection.Immutable<S>, T> onRelease,
            Function1<IPatchCollection.Immutable<S>, T> onReleased);

    public IProcess<S, L, D> getSelf() {
        return self;
    }

    public ImmutableSet<IProcess<S, L, D>> getDependencies() {
        return dependencies;
    }

    public void accept(Action0 onRestart, Action1<IPatchCollection.Immutable<S>> onRelease,
            Action1<IPatchCollection.Immutable<S>> onReleased) {
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

    public static <S, L, D> StateSummary<S, L, D> restart(IProcess<S, L, D> self, Set<IProcess<S, L, D>> dependencies) {
        return new Restart<>(self, dependencies);
    }

    public static <S, L, D> StateSummary<S, L, D> release(IProcess<S, L, D> self, Set<IProcess<S, L, D>> dependencies,
            IPatchCollection.Immutable<S> patches) {
        return new Release<>(self, dependencies, patches);
    }

    public static <S, L, D> StateSummary<S, L, D> released(IProcess<S, L, D> self, Set<IProcess<S, L, D>> dependencies,
            IPatchCollection.Immutable<S> patches) {
        return new Released<>(self, dependencies, patches);
    }

    public static <S, L, D> StateSummary<S, L, D> release(IProcess<S, L, D> self, Set<IProcess<S, L, D>> dependencies) {
        return release(self, dependencies, PatchCollection.Immutable.of());
    }

    public static <S, L, D> StateSummary<S, L, D> released(IProcess<S, L, D> self,
            Set<IProcess<S, L, D>> dependencies) {
        return released(self, dependencies, PatchCollection.Immutable.of());
    }

    private static class Restart<S, L, D> extends StateSummary<S, L, D> {

        Restart(IProcess<S, L, D> self, Set<IProcess<S, L, D>> dependencies) {
            super(self, dependencies);
        }

        @Override public <T> T match(Function0<T> onRestart, Function1<IPatchCollection.Immutable<S>, T> onRelease,
                Function1<IPatchCollection.Immutable<S>, T> onReleased) {
            return onRestart.apply();
        }

        @Override public String toString() {
            return "Restart{self=" + getSelf() + ", dependencies=" + getDependencies() + "}";
        }

    }

    private static class Release<S, L, D> extends StateSummary<S, L, D> {

        private IPatchCollection.Immutable<S> patches;

        Release(IProcess<S, L, D> self, Set<IProcess<S, L, D>> dependencies, IPatchCollection.Immutable<S> patches) {
            super(self, dependencies);
            this.patches = patches;
        }

        @Override public <T> T match(Function0<T> onRestart, Function1<IPatchCollection.Immutable<S>, T> onRelease,
                Function1<IPatchCollection.Immutable<S>, T> onReleased) {
            return onRelease.apply(patches);
        }

        @Override public String toString() {
            return "Release{self=" + getSelf() + ", patches=" + patches + ", dependencies=" + getDependencies() + "}";
        }

    }

    private static class Released<S, L, D> extends StateSummary<S, L, D> {

        private IPatchCollection.Immutable<S> patches;

        Released(IProcess<S, L, D> self, Set<IProcess<S, L, D>> dependencies, IPatchCollection.Immutable<S> patches) {
            super(self, dependencies);
            this.patches = patches;
        }

        @Override public <T> T match(Function0<T> onRestart, Function1<IPatchCollection.Immutable<S>, T> onRelease,
                Function1<IPatchCollection.Immutable<S>, T> onReleased) {
            return onReleased.apply(patches);
        }

        @Override public String toString() {
            return "Released{self=" + getSelf() + ", patches=" + patches + ", dependencies=" + getDependencies() + "}";
        }

    }

}
