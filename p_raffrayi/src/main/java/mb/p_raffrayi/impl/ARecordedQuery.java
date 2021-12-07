package mb.p_raffrayi.impl;

import java.util.Set;
import java.util.stream.Collectors;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import com.google.common.collect.ImmutableSet;

import mb.p_raffrayi.IRecordedQuery;
import mb.p_raffrayi.nameresolution.DataWf;
import mb.scopegraph.ecoop21.LabelWf;
import mb.scopegraph.oopsla20.path.IStep;
import mb.scopegraph.oopsla20.reference.Env;
import mb.scopegraph.oopsla20.terms.newPath.ScopePath;
import mb.scopegraph.patching.IPatchCollection;

@Value.Immutable
@Serial.Version(42L)
public abstract class ARecordedQuery<S, L, D> implements IRecordedQuery<S, L, D> {

    @Override @Value.Parameter public abstract ScopePath<S, L> scopePath();

    @Override @Value.Parameter public abstract LabelWf<L> labelWf();

    @Override @Value.Parameter public abstract DataWf<S, L, D> dataWf();

    @Override @Value.Parameter public abstract boolean empty();

    @Override @Value.Parameter public abstract Set<IRecordedQuery<S, L, D>> transitiveQueries();

    @Override @Value.Parameter public abstract Set<IRecordedQuery<S, L, D>> predicateQueries();

    public static <S, L, D> RecordedQuery<S, L, D> of(ScopePath<S, L> scopePath, LabelWf<L> labelWf,
            DataWf<S, L, D> dataWf, Env<S, L, D> result, Set<IRecordedQuery<S, L, D>> transitiveQueries,
            Set<IRecordedQuery<S, L, D>> predicateQueries) {
        return RecordedQuery.of(scopePath, labelWf, dataWf, result.isEmpty(), transitiveQueries, predicateQueries);
    }

    public static <S, L, D> RecordedQuery<S, L, D> of(ScopePath<S, L> path, LabelWf<L> labelWf, DataWf<S, L, D> dataWf,
            Env<S, L, D> result) {
        return of(path, labelWf, dataWf, result, ImmutableSet.of(), ImmutableSet.of());
    }

    public static <S, L, D> RecordedQuery<S, L, D> of(S scope, LabelWf<L> labelWf, DataWf<S, L, D> dataWf,
            Env<S, L, D> result) {
        return of(new ScopePath<S, L>(scope), labelWf, dataWf, result);
    }

    public static <S, L, D> RecordedQuery<S, L, D> of(ScopePath<S, L> path, LabelWf<L> labelWf,
            DataWf<S, L, D> dataWf) {
        return RecordedQuery.of(path, labelWf, dataWf, false, ImmutableSet.of(), ImmutableSet.of());
    }

    @Override public IRecordedQuery<S, L, D> patch(IPatchCollection.Immutable<S> patches) {
        if(patches.isEmpty()) {
            return this;
        }
        final RecordedQuery<S, L, D> self = (RecordedQuery<S, L, D>) this;
        ScopePath<S, L> newPath = scopePath();
        final S previousSource = newPath.getSource();
        if(scopePath().size() != 0) {
            newPath = new ScopePath<>(patches.patch(previousSource));
            for(IStep<S, L> step : scopePath()) {
                final S previousTarget = step.getTarget();
                newPath = newPath.step(step.getLabel(), patches.patch(previousTarget)).get();
            }
        } else if(!patches.isIdentity(previousSource)) {
            newPath = new ScopePath<>(patches.patch(previousSource));
        }

        final Set<IRecordedQuery<S, L, D>> transitiveQueries =
                transitiveQueries().stream().map(q -> q.patch(patches)).collect(Collectors.toSet());
        final Set<IRecordedQuery<S, L, D>> predicateQueries =
                predicateQueries().stream().map(q -> q.patch(patches)).collect(Collectors.toSet());

        return self.withScopePath(newPath).withTransitiveQueries(transitiveQueries)
                .withPredicateQueries(predicateQueries);
    }

}
