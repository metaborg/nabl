package mb.p_raffrayi.impl;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Map;

import jakarta.annotation.Nullable;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.p_raffrayi.IRecordedQuery;
import mb.p_raffrayi.IUnitResult;
import mb.p_raffrayi.IUnitStats;
import mb.scopegraph.oopsla20.IScopeGraph;
import mb.scopegraph.oopsla20.diff.ScopeGraphDiff;

@Value.Immutable
@Serial.Version(42L)
public abstract class AUnitResult<S, L, D, R> implements IUnitResult<S, L, D, R> {

    @Value.Parameter @Override public abstract String id();

    @Value.Parameter @Override public abstract IScopeGraph.Immutable<S, L, D> scopeGraph();

    @Value.Parameter @Override public abstract Set<IRecordedQuery<S, L, D>> queries();

    @Value.Parameter @Override public abstract List<S> rootScopes();

    @Value.Parameter @Override public abstract Set<S> scopes();

    @Value.Parameter @Override public abstract @Nullable R result();

    @Value.Auxiliary @Override public abstract @Nullable ScopeGraphDiff<S, L, D> diff();

    @Value.Parameter @Override public abstract List<Throwable> failures();

    @Value.Parameter @Override public abstract Map<String, IUnitResult<S, L, D, ?>> subUnitResults();

    @Value.Parameter @Override public abstract @Nullable IUnitStats stats();

    @Value.Lazy @Override public List<Throwable> allFailures() {
        // @formatter:off
        return subUnitResults().values().stream()
            .map(IUnitResult::allFailures)
            .map(List::stream)
            .reduce(failures().stream(), Stream::concat)
            .collect(Collectors.toList());
        // @formatter:on
    }

    @Value.Default @Override public TransitionTrace stateTransitionTrace() {
        return TransitionTrace.OTHER;
    }

}
