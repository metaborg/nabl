package mb.p_raffrayi.impl;

import java.util.Optional;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.util.collection.CapsuleUtil;

import io.usethesource.capsule.Set;
import mb.p_raffrayi.IRecordedQuery;
import mb.p_raffrayi.nameresolution.DataWf;
import mb.scopegraph.ecoop21.LabelWf;
import mb.scopegraph.oopsla20.reference.Env;
import mb.scopegraph.oopsla20.terms.newPath.ScopePath;
import mb.scopegraph.patching.IPatchCollection;

@Value.Immutable
@Serial.Version(42L)
public abstract class ARecordedQuery<S, L, D> implements IRecordedQuery<S, L, D> {

    @Override @Value.Parameter public abstract S source();

    @Override @Value.Parameter public abstract java.util.Set<S> datumScopes();

    @Override @Value.Parameter public abstract LabelWf<L> labelWf();

    @Override @Value.Parameter public abstract DataWf<S, L, D> dataWf();

    @Override @Value.Parameter public abstract boolean empty();

    @Override @Value.Parameter public abstract boolean includePatches();

    public static <S, L, D> RecordedQuery<S, L, D> of(ScopePath<S, L> scopePath, java.util.Set<S> datumScopes, LabelWf<L> labelWf,
            DataWf<S, L, D> dataWf, Env<S, L, D> result, boolean predicate) {
        return RecordedQuery.of(scopePath.getTarget(), datumScopes, labelWf, dataWf, result.isEmpty(), predicate);
    }

    public static <S, L, D> RecordedQuery<S, L, D> of(ScopePath<S, L> path, java.util.Set<S> datumScopes, LabelWf<L> labelWf,
            DataWf<S, L, D> dataWf, Env<S, L, D> result) {
        return of(path, datumScopes, labelWf, dataWf, result, true);
    }

    public static <S, L, D> RecordedQuery<S, L, D> of(S scope, java.util.Set<S> datumScopes, LabelWf<L> labelWf,
            DataWf<S, L, D> dataWf, Env<S, L, D> result) {
        return RecordedQuery.of(scope, datumScopes, labelWf, dataWf, result);
    }

    public static <S, L, D> RecordedQuery<S, L, D> of(ScopePath<S, L> path, java.util.Set<S> datumScopes, LabelWf<L> labelWf,
            DataWf<S, L, D> dataWf) {
        return RecordedQuery.of(path.getTarget(), datumScopes, labelWf, dataWf, false, true);
    }

    @Override public IRecordedQuery<S, L, D> patch(IPatchCollection.Immutable<S> patches) {
        if(patches.isIdentity()) {
            return this;
        }

        Optional<RecordedQuery.Builder<S, L, D>> builder = patchSource(patches);
        builder = patchDatumScopes(patches, builder);
        builder = patchDataWf(patches, builder);

        if(builder.isPresent()) {
            return builder.get().build();
        }

        return this;
    }

    private Optional<RecordedQuery.Builder<S, L, D>> patchDataWf(IPatchCollection.Immutable<S> patches,
            final Optional<RecordedQuery.Builder<S, L, D>> builder) {
        // dataWf().scopes() seems to be expensive, and does the same traversal as dataWf().patch()
        // Therefore, skip `hasOverlap` 'optimization' here.
        final DataWf<S, L, D> oldDataWf = dataWf();
        final DataWf<S, L, D> newDataWf = oldDataWf.patch(patches);

        return oldDataWf == newDataWf ? builder : Optional.of(
                builder.orElseGet(() -> RecordedQuery.<S, L, D>builder().from(this)).dataWf(newDataWf));
    }

    private Optional<RecordedQuery.Builder<S, L, D>> patchDatumScopes(IPatchCollection.Immutable<S> patches,
            final Optional<RecordedQuery.Builder<S, L, D>> builder) {
        if(hasOverlap(datumScopes(), patches.patchDomain())) {
            final Set.Transient<S> newScopes = CapsuleUtil.transientSet();
            for(S scope : datumScopes()) {
                newScopes.__insert(patches.patch(scope));
            }
            return Optional.of(builder.orElseGet(() -> RecordedQuery.<S, L, D>builder().from(this))
                    .datumScopes(newScopes.freeze()));
        }
        return builder;
    }

    private Optional<RecordedQuery.Builder<S, L, D>> patchSource(IPatchCollection.Immutable<S> patches) {
        final S newSource = patches.patch(source());
        if(newSource != source()) {
            return Optional.of(RecordedQuery.<S, L, D>builder().from(this).source(newSource));
        }
        return Optional.empty();
    }

    private boolean hasOverlap(final java.util.Set<S> set1, final java.util.Set<S> set2) {
        final java.util.Set<S> smaller;
        final java.util.Set<S> larger;

        if(set2.size() < set1.size()) {
            smaller = set2;
            larger = set1;
        } else {
            smaller = set1;
            larger = set2;
        }

        for(S scope : smaller) {
            if(larger.contains(scope)) {
                return true;
            }
        }
        return false;
    }

}
