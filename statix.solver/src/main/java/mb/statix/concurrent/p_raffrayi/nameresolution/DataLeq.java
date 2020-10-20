package mb.statix.concurrent.p_raffrayi.nameresolution;

import org.metaborg.util.task.ICancel;

public interface DataLeq<D> {

    boolean leq(D d1, D d2, ICancel cancel) throws InterruptedException;

}