package mb.p_raffrayi.impl;

import java.util.Set;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

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

    @Override @Value.Parameter public abstract Set<S> datumScopes();

    @Override @Value.Parameter public abstract LabelWf<L> labelWf();

    @Override @Value.Parameter public abstract DataWf<S, L, D> dataWf();

    @Override @Value.Parameter public abstract boolean empty();

    @Override @Value.Parameter public abstract boolean includePatches();

    public static <S, L, D> RecordedQuery<S, L, D> of(ScopePath<S, L> scopePath, Set<S> datumScopes, LabelWf<L> labelWf,
            DataWf<S, L, D> dataWf, Env<S, L, D> result, boolean predicate) {
        return RecordedQuery.of(scopePath, datumScopes, labelWf, dataWf, result.isEmpty(), predicate);
    }

    public static <S, L, D> RecordedQuery<S, L, D> of(ScopePath<S, L> path, Set<S> datumScopes, LabelWf<L> labelWf, DataWf<S, L, D> dataWf,
            Env<S, L, D> result) {
        return of(path, datumScopes, labelWf, dataWf, result, true);
    }

    public static <S, L, D> RecordedQuery<S, L, D> of(S scope, Set<S> datumScopes, LabelWf<L> labelWf, DataWf<S, L, D> dataWf,
            Env<S, L, D> result) {
        return of(new ScopePath<S, L>(scope), datumScopes, labelWf, dataWf, result);
    }

    public static <S, L, D> RecordedQuery<S, L, D> of(ScopePath<S, L> path, Set<S> datumScopes, LabelWf<L> labelWf,
            DataWf<S, L, D> dataWf) {
        return RecordedQuery.of(path, datumScopes, labelWf, dataWf, false, true);
    }

    @Override public IRecordedQuery<S, L, D> patch(IPatchCollection.Immutable<S> patches) {
        if(patches.isIdentity()) {
            return this;
        }

        // Patch path
        ScopePath<S, L> newPath;
        if(!Sets.intersection(scopePath().scopeSet(), patches.patchDomain()).isEmpty()) {
            final S previousSource = scopePath().getSource();
            newPath = new ScopePath<>(patches.patch(previousSource));
            if(scopePath().size() != 0) {
                for(IStep<S, L> step : scopePath()) {
                    final S previousTarget = step.getTarget();
                    newPath = newPath.step(step.getLabel(), patches.patch(previousTarget)).get();
                }
            }
        } else {
            newPath = scopePath();
        }

        // Patch datumScopes
        final Set<S> newDatumScopes;
        if(Sets.intersection(datumScopes(), patches.patchDomain()).isEmpty()) {
            newDatumScopes = datumScopes();
        } else {
            newDatumScopes = datumScopes().stream().map(patches::patch).collect(ImmutableSet.toImmutableSet());
        }

        // Patch dataWf
        final DataWf<S, L, D> newDataWf;
        if(Sets.intersection(dataWf().scopes(), patches.patchDomain()).isEmpty()) {
            newDataWf = dataWf();
        } else {
            newDataWf = dataWf().patch(patches);
        }

        // @formatter:off
        return RecordedQuery.<S, L, D>builder().from(this)
            .dataWf(newDataWf)
            .scopePath(newPath)
            .datumScopes(newDatumScopes)
            .build();
        // @formatter:on
    }

}
