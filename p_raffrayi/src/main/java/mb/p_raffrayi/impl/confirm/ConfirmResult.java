package mb.p_raffrayi.impl.confirm;

import org.metaborg.util.functions.Action0;
import org.metaborg.util.functions.Action1;
import org.metaborg.util.functions.Function0;
import org.metaborg.util.functions.Function1;

import mb.scopegraph.patching.IPatchCollection;
import mb.scopegraph.patching.PatchCollection;

public abstract class ConfirmResult<S> {

    @SuppressWarnings("rawtypes") private static final ConfirmResult.Deny DENY = new ConfirmResult.Deny<>();

    @SuppressWarnings({ "rawtypes" }) private static final ConfirmResult.Confirm EMPTY_CONFIRM =
            new ConfirmResult.Confirm<>(PatchCollection.Immutable.of());

    public abstract <T> T match(Function0<T> onDeny, Function1<IPatchCollection.Immutable<S>, T> onConfirm);

    public abstract void visit(Action0 onDeny, Action1<IPatchCollection.Immutable<S>> onConfirm);

    @SuppressWarnings("unchecked") public static <S> ConfirmResult<S> deny() {
        return DENY;
    }

    public static <S> ConfirmResult<S> confirm(IPatchCollection.Immutable<S> patches) {
        return new ConfirmResult.Confirm<>(patches);
    }

    @SuppressWarnings("unchecked") public static <S> ConfirmResult<S> confirm() {
        return EMPTY_CONFIRM;
    }

    private static class Deny<S> extends ConfirmResult<S> {

        @Override public <T> T match(Function0<T> onDeny, Function1<IPatchCollection.Immutable<S>, T> onConfirm) {
            return onDeny.apply();
        }

        @Override public void visit(Action0 onDeny, Action1<IPatchCollection.Immutable<S>> onConfirm) {
            onDeny.apply();
        }

        @Override public int hashCode() {
            return 42;
        }

        @Override public boolean equals(Object obj) {
            return obj == this;
        }

        @Override public String toString() {
            return "Deny{}";
        }

    }

    private static class Confirm<S> extends ConfirmResult<S> {

        private final IPatchCollection.Immutable<S> patches;

        private Confirm(IPatchCollection.Immutable<S> patches) {
            this.patches = patches;
        }

        @Override public <T> T match(Function0<T> onDeny, Function1<IPatchCollection.Immutable<S>, T> onConfirm) {
            return onConfirm.apply(patches);
        }

        @Override public void visit(Action0 onDeny, Action1<IPatchCollection.Immutable<S>> onConfirm) {
            onConfirm.apply(patches);
        }

        @Override public int hashCode() {
            return 17 + 31 * patches.hashCode();
        }

        @SuppressWarnings("unchecked")
        @Override public boolean equals(Object obj) {
            if(obj == this) {
                return true;
            }
            if(obj == null) {
                return false;
            }
            if(!obj.getClass().equals(this.getClass())) {
                return false;
            }

            return ((Confirm<S>) obj).patches.equals(patches);
        }

        @Override public String toString() {
            return "Confirm{" + patches + "}";
        }

    }
}
