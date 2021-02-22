package mb.statix.concurrent.p_raffrayi;

import javax.annotation.Nullable;

import mb.statix.concurrent.p_raffrayi.nameresolution.DataLeq;
import mb.statix.concurrent.p_raffrayi.nameresolution.DataLeqInternal;
import mb.statix.concurrent.p_raffrayi.nameresolution.DataWf;
import mb.statix.concurrent.p_raffrayi.nameresolution.DataWfInternal;
import mb.statix.concurrent.p_raffrayi.nameresolution.LabelOrder;
import mb.statix.concurrent.p_raffrayi.nameresolution.LabelWf;

public interface IRecordedQuery<S, L, D> {
    
    S scope();
    
    LabelWf<L> labelWf();
    
    DataWf<D> dataWf();
    
    LabelOrder<L> labelOrder();
    
    DataLeq<D> dataLeq();
    
    @Nullable DataWfInternal<D> dataWfInternal();
    
    @Nullable DataLeqInternal<D> dataLeqInternal();

}
