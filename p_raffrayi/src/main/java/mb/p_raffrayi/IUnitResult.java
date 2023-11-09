package mb.p_raffrayi;

import java.util.List;
import java.util.Set;
import java.util.Map;

import jakarta.annotation.Nullable;

import mb.scopegraph.oopsla20.IScopeGraph;
import mb.scopegraph.oopsla20.diff.ScopeGraphDiff;

public interface IUnitResult<S, L, D, R> {

    String id();

    IScopeGraph.Immutable<S, L, D> scopeGraph();

    @Nullable R result();

    Set<IRecordedQuery<S, L, D>> queries();

    List<S> rootScopes();

    Set<S> scopes();

    @Nullable ScopeGraphDiff<S, L, D> diff();

    List<Throwable> failures();

    Map<String, IUnitResult<S, L, D, ?>> subUnitResults();

    IUnitStats stats();

    TransitionTrace stateTransitionTrace();

    List<Throwable> allFailures();

    public enum TransitionTrace {
        OTHER, INITIALLY_STARTED, RESTARTED, RELEASED
    }

}
