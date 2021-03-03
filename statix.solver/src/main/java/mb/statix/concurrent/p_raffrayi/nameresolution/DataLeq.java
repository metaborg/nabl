package mb.statix.concurrent.p_raffrayi.nameresolution;

import org.metaborg.util.task.ICancel;

import mb.statix.concurrent.actors.futures.CompletableFuture;
import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.concurrent.p_raffrayi.ITypeCheckerContext;

public interface DataLeq<S, L, D> {

    IFuture<Boolean> leq(D d1, D d2, ITypeCheckerContext<S, L, D> context, ICancel cancel) throws InterruptedException;

    static <S, L, D> DataLeq<S, L, D> any() {
        return new DataLeq<S, L, D>() {
            @SuppressWarnings("unused") @Override public IFuture<Boolean> leq(D d1, D d2,
                    ITypeCheckerContext<S, L, D> context, ICancel cancel) throws InterruptedException {
                return CompletableFuture.completedFuture(true);
            }
        };
    }

    static <S, L, D> DataLeq<S, L, D> none() {
        return new DataLeq<S, L, D>() {
            @SuppressWarnings("unused") @Override public IFuture<Boolean> leq(D d1, D d2,
                    ITypeCheckerContext<S, L, D> context, ICancel cancel) throws InterruptedException {
                return CompletableFuture.completedFuture(false);
            }
        };
    }

}