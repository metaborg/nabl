package mb.statix.concurrent.p_raffrayi.nameresolution;

import org.metaborg.util.task.ICancel;

public interface DataLeq<D> {

    boolean leq(D d1, D d2, ICancel cancel) throws InterruptedException;

    static <D> DataLeq<D> any() {
        return new DataLeq<D>() {
            @Override public boolean leq(D d1, D d2, ICancel cancel) throws InterruptedException {
                return true;
            }
        };
    }

    static <D> DataLeq<D> none() {
        return new DataLeq<D>() {
            @Override public boolean leq(D d1, D d2, ICancel cancel) throws InterruptedException {
                return false;
            }
        };
    }

}