package mb.statix.concurrent.p_raffrayi.nameresolution;

import org.metaborg.util.task.ICancel;

import mb.statix.concurrent.actors.futures.CompletableFuture;
import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.concurrent.p_raffrayi.ITypeCheckerContext;

public interface DataWf<S, L, D> {

    IFuture<Boolean> wf(D d, ITypeCheckerContext<S, L, D> context, ICancel cancel) throws InterruptedException;

    static <S, L, D> DataWf<S, L, D> any() {
        return new DataWf<S, L, D>() {
            @SuppressWarnings("unused") @Override public IFuture<Boolean> wf(D d, ITypeCheckerContext<S, L, D> context,
                    ICancel cancel) throws InterruptedException {
                return CompletableFuture.completedFuture(true);
            }
        };
    }

}