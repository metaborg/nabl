package mb.p_raffrayi.impl;

import java.util.Set;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import com.google.common.collect.ImmutableSet;

import mb.p_raffrayi.IRecordedQuery;
import mb.p_raffrayi.nameresolution.DataLeq;
import mb.p_raffrayi.nameresolution.DataWf;
import mb.scopegraph.ecoop21.LabelOrder;
import mb.scopegraph.ecoop21.LabelWf;
import mb.scopegraph.oopsla20.reference.Env;
import mb.scopegraph.oopsla20.terms.newPath.ScopePath;

@Value.Immutable
@Serial.Version(42L)
public abstract class ARecordedQuery<S, L, D> implements IRecordedQuery<S, L, D> {

    @Override @Value.Parameter public abstract ScopePath<S, L> scopePath();

    @Override @Value.Parameter public abstract LabelWf<L> labelWf();

    @Override @Value.Parameter public abstract DataWf<S, L, D> dataWf();

    @Override @Value.Parameter public abstract LabelOrder<L> labelOrder();

    @Override @Value.Parameter public abstract DataLeq<S, L, D> dataLeq();

    @Override @Value.Parameter public abstract Env<S, L, D> result();

    @Override @Value.Parameter public abstract Set<IRecordedQuery<S, L, D>> transitiveQueries();

    @Override @Value.Parameter public abstract Set<IRecordedQuery<S, L, D>> predicateQueries();

    public static <S, L, D> RecordedQuery<S, L, D> of(ScopePath<S, L> path, LabelWf<L> labelWf, DataWf<S, L, D> dataWf,
            LabelOrder<L> labelOrder, DataLeq<S, L, D> dataLeq, Env<S, L, D> result) {
        return RecordedQuery.of(path, labelWf, dataWf, labelOrder, dataLeq, result, ImmutableSet.of(),
                ImmutableSet.of());
    }

    public static <S, L, D> RecordedQuery<S, L, D> of(S scope, LabelWf<L> labelWf, DataWf<S, L, D> dataWf,
            LabelOrder<L> labelOrder, DataLeq<S, L, D> dataLeq, Env<S, L, D> result) {
        return of(new ScopePath<S, L>(scope), labelWf, dataWf, labelOrder, dataLeq, result);
    }

}
