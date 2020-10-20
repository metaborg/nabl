package mb.statix.concurrent.p_raffrayi.nameresolution;

import org.metaborg.util.task.ICancel;

import mb.statix.concurrent.actors.futures.IFuture;

public interface DataLeqInternal<D> {

    IFuture<Boolean> leq(D d1, D d2, ICancel cancel) throws InterruptedException;

}