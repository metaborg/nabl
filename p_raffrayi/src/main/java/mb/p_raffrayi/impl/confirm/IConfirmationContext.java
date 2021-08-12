package mb.p_raffrayi.impl.confirm;

import java.util.Optional;

import org.metaborg.util.functions.Action0;
import org.metaborg.util.functions.Action1;
import org.metaborg.util.future.IFuture;

import mb.p_raffrayi.impl.IQueryAnswer;
import mb.p_raffrayi.impl.envdiff.IEnvDiff;
import mb.p_raffrayi.nameresolution.DataLeq;
import mb.p_raffrayi.nameresolution.DataWf;
import mb.scopegraph.ecoop21.LabelOrder;
import mb.scopegraph.ecoop21.LabelWf;
import mb.scopegraph.oopsla20.diff.BiMap;
import mb.scopegraph.oopsla20.diff.BiMap.Immutable;
import mb.scopegraph.oopsla20.reference.Env;
import mb.scopegraph.oopsla20.terms.newPath.ScopePath;

public interface IConfirmationContext<S, L, D> {

    IFuture<IQueryAnswer<S, L, D>> query(ScopePath<S, L> scopePath, LabelWf<L> labelWf, LabelOrder<L> labelOrder,
            DataWf<S, L, D> dataWf, DataLeq<S, L, D> dataEquiv);

    IFuture<Env<S, L, D>> queryPrevious(ScopePath<S, L> scopePath, LabelWf<L> labelWf, DataWf<S, L, D> dataWf,
            LabelOrder<L> labelOrder, DataLeq<S, L, D> dataEquiv);

    IFuture<ExternalConfirm<S>> externalConfirm(ScopePath<S, L> path, LabelWf<L> labelWf, DataWf<S, L, D> dataWf,
            boolean prevEnvEmpty);

    IFuture<IEnvDiff<S, L, D>> envDiff(ScopePath<S, L> path, LabelWf<L> labelWf, DataWf<S, L, D> dataWf);

    IFuture<Optional<S>> match(S scope);

    abstract class ExternalConfirm<S> {

        @SuppressWarnings("rawtypes") private static final Local LOCAL = new Local<>();
        @SuppressWarnings("rawtypes") private static final Deny DENY = new Deny<>();

        public abstract void visit(Action0 onLocal, Action0 onDeny, Action1<BiMap.Immutable<S>> onConfirm);


        @SuppressWarnings("unchecked") public static <S> ExternalConfirm<S> local() {
            return LOCAL;
        }

        @SuppressWarnings("unchecked") public static <S> ExternalConfirm<S> deny() {
            return DENY;
        }

        public static <S> ExternalConfirm<S> confirm(BiMap.Immutable<S> patches) {
            return new Confirm<>(patches);
        }

        private static class Local<S> extends ExternalConfirm<S> {

            @Override public void visit(Action0 onLocal, Action0 onDeny, Action1<Immutable<S>> onConfirm) {
                onLocal.apply();
            }

        }

        private static class Deny<S> extends ExternalConfirm<S> {

            @Override public void visit(Action0 onLocal, Action0 onDeny, Action1<Immutable<S>> onConfirm) {
                onDeny.apply();
            }

        }

        private static class Confirm<S> extends ExternalConfirm<S> {

            private final BiMap.Immutable<S> patches;

            private Confirm(BiMap.Immutable<S> patches) {
                this.patches = patches;
            }

            @Override public void visit(Action0 onLocal, Action0 onDeny, Action1<Immutable<S>> onConfirm) {
                onConfirm.apply(patches);
            }
        }
    }
}
