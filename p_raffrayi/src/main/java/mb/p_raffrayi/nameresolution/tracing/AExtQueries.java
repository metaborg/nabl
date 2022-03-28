package mb.p_raffrayi.nameresolution.tracing;

import java.util.Collection;
import java.util.Set;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.p_raffrayi.IRecordedQuery;

@Value.Immutable
@Serial.Version(42)
public abstract class AExtQueries<S, L, D> {

    @SuppressWarnings("rawtypes") private static final ExtQueries empty = ExtQueries.builder().build();

    @Value.Parameter public abstract Set<IRecordedQuery<S, L, D>> transitiveQueries();

    @Value.Parameter public abstract Set<IRecordedQuery<S, L, D>> predicateQueries();

    public ExtQueries<S, L, D> addTransitiveQueries(Collection<IRecordedQuery<S, L, D>> transitiveQueries) {
        final ExtQueries<S, L, D> self = (ExtQueries<S, L, D>) this;
        if(transitiveQueries.isEmpty()) {
            return self;
        }

        final ExtQueries.Builder<S, L, D> builder = ExtQueries.<S, L, D>builder().from(self);
        builder.addAllTransitiveQueries(transitiveQueries);
        return builder.build();
    }

    public ExtQueries<S, L, D> addPredicateQueries(Collection<IRecordedQuery<S, L, D>> predicateQueries) {
        final ExtQueries<S, L, D> self = (ExtQueries<S, L, D>) this;
        if(predicateQueries.isEmpty()) {
            return self;
        }

        final ExtQueries.Builder<S, L, D> builder = ExtQueries.<S, L, D>builder().from(self);
        builder.addAllPredicateQueries(predicateQueries);
        return builder.build();
    }

    public ExtQueries<S, L, D> addQueries(Collection<IRecordedQuery<S, L, D>> transitiveQueries,
            Collection<IRecordedQuery<S, L, D>> predicateQueries) {
        final ExtQueries<S, L, D> self = (ExtQueries<S, L, D>) this;
        if(transitiveQueries.isEmpty() && predicateQueries.isEmpty()) {
            return self;
        }

        final ExtQueries.Builder<S, L, D> builder = ExtQueries.<S, L, D>builder().from(self);
        builder.addAllTransitiveQueries(transitiveQueries);
        builder.addAllPredicateQueries(predicateQueries);
        return builder.build();
    }

    public ExtQueries<S, L, D> addQueries(ExtQueries<S, L, D> queries) {
        return addQueries(queries.transitiveQueries(), queries.predicateQueries());
    }

    @SuppressWarnings("unchecked") public static <S, L, D> ExtQueries<S, L, D> empty() {
        return empty;
    }

}
