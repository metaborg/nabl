package mb.statix.concurrent.p_raffrayi.nameresolution;

import mb.statix.concurrent.actors.futures.IFuture;

public interface DataLeq<D> {

    IFuture<Boolean> leq(D d1, D d2) throws InterruptedException;

    boolean alwaysTrue() throws InterruptedException;

}