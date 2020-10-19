package mb.statix.concurrent.p_raffrayi.nameresolution;

import mb.statix.concurrent.actors.futures.IFuture;

public interface DataWF<D> {

    IFuture<Boolean> wf(D d) throws InterruptedException;

}