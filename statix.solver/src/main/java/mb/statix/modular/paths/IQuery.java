package mb.statix.modular.paths;

import mb.statix.modular.util.IOwnable;
import mb.statix.scopegraph.reference.DataLeq;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.scopegraph.reference.LabelWF;

public interface IQuery<S extends IOwnable, V, L, R> extends IOwnable {
    LabelWF<L> getWFL();
    DataWF<V> getWFD();
    LabelOrder<L> getLabelOrder();
    DataLeq<V> getDataOrder();
    
    //TODO Queries need to be thrown away with the scope identities of the source scope
    S getSourceScope();
    
    
}
