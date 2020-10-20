package mb.statix.concurrent.p_raffrayi.nameresolution;

import org.metaborg.util.task.ICancel;

import mb.statix.concurrent.actors.futures.IFuture;

public interface DataWfInternal<D> {

    IFuture<Boolean> wf(D d, ICancel cancel) throws InterruptedException;

}