package mb.p_raffrayi.nameresolution;


import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.task.ICancel;

import io.usethesource.capsule.Set;
import io.usethesource.capsule.Set.Immutable;
import mb.p_raffrayi.ITypeCheckerContext;
import mb.scopegraph.patching.IPatchCollection;

public interface DataWf<S, L, D> {

    IFuture<Boolean> wf(D d, ITypeCheckerContext<S, L, D> context, ICancel cancel) throws InterruptedException;

    default Set.Immutable<S> scopes() {
        return CapsuleUtil.immutableSet();
    }

    default DataWf<S, L, D> patch(IPatchCollection.Immutable<S> patches) {
        return this;
    }

    @SuppressWarnings("unchecked") static <S, L, D> DataWf<S, L, D> any() {
        return ANY;
    }

    @SuppressWarnings("unchecked") static <S, L, D> DataWf<S, L, D> none() {
        return NONE;
    }

    @SuppressWarnings("rawtypes") static final DataWf ANY = new DataWf() {

        @SuppressWarnings("unused") @Override public IFuture<Boolean> wf(Object d, ITypeCheckerContext context,
                ICancel cancel) throws InterruptedException {
            return CompletableFuture.completedFuture(true);
        }

        @Override public String toString() {
            return "any";
        }

        @Override public Immutable scopes() {
            return CapsuleUtil.immutableSet();
        }

        @Override public DataWf patch(IPatchCollection.Immutable patches) {
            return this;
        }

    };

    @SuppressWarnings("rawtypes") static final DataWf NONE = new DataWf() {

        @SuppressWarnings("unused") @Override public IFuture<Boolean> wf(Object d, ITypeCheckerContext context,
                ICancel cancel) throws InterruptedException {
            return CompletableFuture.completedFuture(false);
        }

        @Override public String toString() {
            return "none";
        }

        @Override public Immutable scopes() {
            return CapsuleUtil.immutableSet();
        }

        @Override public DataWf patch(IPatchCollection.Immutable patches) {
            return this;
        }

    };

}
