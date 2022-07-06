package mb.p_raffrayi.nameresolution.tracing;

import java.util.Collection;
import java.util.Set;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.p_raffrayi.IRecordedQuery;

@Value.Immutable
@Serial.Version(42)
public abstract class AExtQuerySet<S, L, D> {

    @Value.Parameter public abstract Set<IRecordedQuery<S, L, D>> transitiveQueries();

    @Value.Parameter public abstract Set<IRecordedQuery<S, L, D>> predicateQueries();

    public ExtQuerySet<S, L, D> addTransitiveQueries(Collection<IRecordedQuery<S, L, D>> transitiveQueries) {
        final ExtQuerySet<S, L, D> self = (ExtQuerySet<S, L, D>) this;
        if(transitiveQueries.isEmpty()) {
            return self;
        }

        final ExtQuerySet.Builder<S, L, D> builder = ExtQuerySet.<S, L, D>builder().from(self);
        builder.addAllTransitiveQueries(transitiveQueries);
        return builder.build();
    }

    public ExtQuerySet<S, L, D> addPredicateQueries(Collection<IRecordedQuery<S, L, D>> predicateQueries) {
        final ExtQuerySet<S, L, D> self = (ExtQuerySet<S, L, D>) this;
        if(predicateQueries.isEmpty()) {
            return self;
        }

        final ExtQuerySet.Builder<S, L, D> builder = ExtQuerySet.<S, L, D>builder().from(self);
        builder.addAllPredicateQueries(predicateQueries);
        return builder.build();
    }

    public ExtQuerySet<S, L, D> addQueries(Collection<IRecordedQuery<S, L, D>> transitiveQueries,
            Collection<IRecordedQuery<S, L, D>> predicateQueries) {
        final ExtQuerySet<S, L, D> self = (ExtQuerySet<S, L, D>) this;
        if(transitiveQueries.isEmpty() && predicateQueries.isEmpty()) {
            return self;
        }

        final ExtQuerySet.Builder<S, L, D> builder = ExtQuerySet.<S, L, D>builder().from(self);
        builder.addAllTransitiveQueries(transitiveQueries);
        builder.addAllPredicateQueries(predicateQueries);
        return builder.build();
    }

    public ExtQuerySet<S, L, D> addQueries(ExtQuerySet<S, L, D> queries) {
        return addQueries(queries.transitiveQueries(), queries.predicateQueries());
    }

}
