package mb.statix.concurrent.p_raffrayi.nameresolution;

import org.metaborg.util.task.ICancel;

public interface DataWf<D> {

    boolean wf(D d, ICancel cancel) throws InterruptedException;

}