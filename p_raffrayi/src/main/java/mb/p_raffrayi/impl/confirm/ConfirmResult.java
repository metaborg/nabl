package mb.p_raffrayi.impl.confirm;

import org.metaborg.util.functions.Action0;
import org.metaborg.util.functions.Action1;
import org.metaborg.util.functions.Function0;
import org.metaborg.util.functions.Function1;

import mb.scopegraph.oopsla20.diff.BiMap;
import mb.scopegraph.oopsla20.diff.BiMap.Immutable;

public abstract class ConfirmResult<S> {

    @SuppressWarnings("rawtypes") private static final ConfirmResult.Deny DENY = new ConfirmResult.Deny<>();

    @SuppressWarnings({ "rawtypes", "unchecked" }) private static final ConfirmResult.Confirm EMPTY_CONFIRM =
            new ConfirmResult.Confirm(BiMap.Immutable.of());

    public abstract <T> T match(Function0<T> onDeny, Function1<BiMap.Immutable<S>, T> onConfirm);

    public abstract void visit(Action0 onDeny, Action1<BiMap.Immutable<S>> onConfirm);

    @SuppressWarnings("unchecked") public static <S> ConfirmResult<S> deny() {
        return DENY;
    }

    public static <S> ConfirmResult<S> confirm(BiMap.Immutable<S> patches) {
        return new ConfirmResult.Confirm<>(patches);
    }

    @SuppressWarnings("unchecked") public static <S> ConfirmResult<S> confirm() {
        return EMPTY_CONFIRM;
    }

    private static class Deny<S> extends ConfirmResult<S> {

        @Override public <T> T match(Function0<T> onDeny, Function1<BiMap.Immutable<S>, T> onConfirm) {
            return onDeny.apply();
        }

        @Override public void visit(Action0 onDeny, Action1<Immutable<S>> onConfirm) {
            onDeny.apply();
        }

    }

    private static class Confirm<S> extends ConfirmResult<S> {

        private final BiMap.Immutable<S> patches;

        private Confirm(BiMap.Immutable<S> patches) {
            this.patches = patches;
        }

        @Override public <T> T match(Function0<T> onDeny, Function1<BiMap.Immutable<S>, T> onConfirm) {
            return onConfirm.apply(patches);
        }

        @Override public void visit(Action0 onDeny, Action1<Immutable<S>> onConfirm) {
            onConfirm.apply(patches);
        }
    }
}