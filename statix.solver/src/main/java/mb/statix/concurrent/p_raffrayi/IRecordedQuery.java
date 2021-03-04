package mb.statix.concurrent.p_raffrayi;

import mb.statix.concurrent.p_raffrayi.nameresolution.DataLeq;
import mb.statix.concurrent.p_raffrayi.nameresolution.DataWf;
import mb.statix.concurrent.p_raffrayi.nameresolution.LabelOrder;
import mb.statix.concurrent.p_raffrayi.nameresolution.LabelWf;

public interface IRecordedQuery<S, L, D> {
    
    S scope();
    
    LabelWf<L> labelWf();
    
    DataWf<S, L, D> dataWf();
    
    LabelOrder<L> labelOrder();
    
    DataLeq<S, L, D> dataLeq();

}
