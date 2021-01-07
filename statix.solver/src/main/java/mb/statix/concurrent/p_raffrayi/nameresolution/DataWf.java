package mb.statix.concurrent.p_raffrayi.nameresolution;

import org.metaborg.util.task.ICancel;

public interface DataWf<D> {

    boolean wf(D d, ICancel cancel) throws InterruptedException;

    static <D> DataWf<D> any() {
        return new DataWf<D>() {
            @Override public boolean wf(D d, ICancel cancel) throws InterruptedException {
                return true;
            }
        };
    }

}