package mb.statix.concurrent.p_raffrayi.impl;

import javax.annotation.Nullable;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.statix.concurrent.p_raffrayi.IRecordedQuery;
import mb.statix.concurrent.p_raffrayi.nameresolution.DataLeq;
import mb.statix.concurrent.p_raffrayi.nameresolution.DataLeqInternal;
import mb.statix.concurrent.p_raffrayi.nameresolution.DataWf;
import mb.statix.concurrent.p_raffrayi.nameresolution.DataWfInternal;
import mb.statix.concurrent.p_raffrayi.nameresolution.LabelOrder;
import mb.statix.concurrent.p_raffrayi.nameresolution.LabelWf;

@Value.Immutable
@Serial.Version(42L)
public abstract class ARecordedQuery<S, L, D> implements IRecordedQuery<S, L, D> {

    @Override @Value.Parameter public abstract S scope();

    @Override @Value.Parameter public abstract LabelWf<L> labelWf();

    @Override @Value.Parameter public abstract DataWf<D> dataWf();

    @Override @Value.Parameter public abstract LabelOrder<L> labelOrder();

    @Override @Value.Parameter public abstract DataLeq<D> dataLeq();

    @Override @Value.Parameter public abstract @Nullable DataWfInternal<D> dataWfInternal();

    @Override @Value.Parameter public abstract @Nullable DataLeqInternal<D> dataLeqInternal();
}
