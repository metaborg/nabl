package mb.p_raffrayi.nameresolution.tracing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.util.collection.CapsuleUtil;

import io.usethesource.capsule.Set;
import mb.p_raffrayi.IRecordedQuery;
import mb.p_raffrayi.impl.RecordedQuery;

@Value.Immutable(builder = false)
@Serial.Version(42)
public abstract class AExtQuerySet<S, L, D> {

    @Value.Parameter public abstract Set.Immutable<IRecordedQuery<S, L, D>> transitiveQueries();

    @Value.Parameter public abstract Set.Immutable<IRecordedQuery<S, L, D>> predicateQueries();

    public ExtQuerySet<S, L, D> addTransitiveQueries(java.util.Set<IRecordedQuery<S, L, D>> transitiveQueries) {
        final ExtQuerySet<S, L, D> self = (ExtQuerySet<S, L, D>) this;
        if(transitiveQueries.isEmpty()) {
            return self;
        }

        final AExtQuerySet.Builder<S, L, D> builder = AExtQuerySet.<S, L, D>builder().from(self);
        builder.addAllTransitiveQueries(transitiveQueries);
        return builder.build();
    }

    public ExtQuerySet<S, L, D> addPredicateQueries(java.util.Set<IRecordedQuery<S, L, D>> predicateQueries) {
        final ExtQuerySet<S, L, D> self = (ExtQuerySet<S, L, D>) this;
        if(predicateQueries.isEmpty()) {
            return self;
        }

        final AExtQuerySet.Builder<S, L, D> builder = AExtQuerySet.<S, L, D>builder().from(self);
        builder.addAllPredicateQueries(predicateQueries);
        return builder.build();
    }

    public ExtQuerySet<S, L, D> addQueries(java.util.Set<IRecordedQuery<S, L, D>> transitiveQueries,
        java.util.Set<IRecordedQuery<S, L, D>> predicateQueries) {
        final ExtQuerySet<S, L, D> self = (ExtQuerySet<S, L, D>) this;
        if(transitiveQueries.isEmpty() && predicateQueries.isEmpty()) {
            return self;
        }

        final AExtQuerySet.Builder<S, L, D> builder = AExtQuerySet.<S, L, D>builder().from(self);
        builder.addAllTransitiveQueries(transitiveQueries);
        builder.addAllPredicateQueries(predicateQueries);
        return builder.build();
    }

    public static <L, S, D> Builder builder() {
        return new Builder();
    }

    public ExtQuerySet<S, L, D> addQueries(ExtQuerySet<S, L, D> queries) {
        return addQueries(queries.transitiveQueries(), queries.predicateQueries());
    }

    public static final class Builder<S, L, D> {
        private Set.Transient<IRecordedQuery<S, L, D>> transitiveQueries;
        private Set.Transient<IRecordedQuery<S, L, D>> predicateQueries;
        private AExtQuerySet<S, L, D> frozen = null;

        private Builder() {
            transitiveQueries = CapsuleUtil.transientSet();
            predicateQueries = CapsuleUtil.transientSet();
        }

        public final AExtQuerySet.Builder<S, L, D> from(AExtQuerySet<S, L, D> instance) {
            Objects.requireNonNull(instance, "instance");
            addAllTransitiveQueries(instance.transitiveQueries());
            addAllPredicateQueries(instance.predicateQueries());
            return this;
        }

        public AExtQuerySet.Builder<S, L, D> addAllPredicateQueries(java.util.Set<IRecordedQuery<S, L, D>> predicateQueries) {
            this.predicateQueries.__insertAll(predicateQueries);
            return this;
        }

        public AExtQuerySet.Builder<S, L, D> addAllTransitiveQueries(java.util.Set<IRecordedQuery<S, L, D>> transitiveQueries) {
            this.transitiveQueries.__insertAll(transitiveQueries);
            return this;
        }

        public final AExtQuerySet.Builder<S, L, D> transitiveQueries(Set.Transient<IRecordedQuery<S, L, D>> transitiveQueries) {
            this.transitiveQueries = Objects.requireNonNull(transitiveQueries, "transitiveQueries");
            return this;
        }

        public final AExtQuerySet.Builder<S, L, D> predicateQueries(Set.Transient<IRecordedQuery<S, L, D>> predicateQueries) {
            this.predicateQueries = Objects.requireNonNull(predicateQueries, "predicateQueries");
            return this;
        }

        public ExtQuerySet<S, L, D> build() {
            return ExtQuerySet.of(transitiveQueries.freeze(), predicateQueries.freeze());
        }

        public AExtQuerySet.Builder<S, L, D> addTransitiveQueries(RecordedQuery<S, L, D> transitiveQuery) {
            this.transitiveQueries.__insert(transitiveQuery);
            return this;
        }
    }
}
