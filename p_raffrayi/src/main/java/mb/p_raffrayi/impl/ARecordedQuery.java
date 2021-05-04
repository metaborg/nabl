package mb.p_raffrayi.impl;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.p_raffrayi.IRecordedQuery;
import mb.p_raffrayi.nameresolution.DataLeq;
import mb.p_raffrayi.nameresolution.DataWf;
import mb.scopegraph.ecoop21.LabelOrder;
import mb.scopegraph.ecoop21.LabelWf;

@Value.Immutable
@Serial.Version(42L)
public abstract class ARecordedQuery<S, L, D> implements IRecordedQuery<S, L, D> {

    @Override @Value.Parameter public abstract S scope();

    @Override @Value.Parameter public abstract LabelWf<L> labelWf();

    @Override @Value.Parameter public abstract DataWf<S, L, D> dataWf();

    @Override @Value.Parameter public abstract LabelOrder<L> labelOrder();

    @Override @Value.Parameter public abstract DataLeq<S, L, D> dataLeq();
}
