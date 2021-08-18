package mb.p_raffrayi;

import java.util.List;
import java.util.Set;
import java.util.Map;

import javax.annotation.Nullable;

import mb.p_raffrayi.impl.StateCapture;
import mb.scopegraph.oopsla20.IScopeGraph;
import mb.scopegraph.oopsla20.diff.ScopeGraphDiff;

public interface IUnitResult<S, L, D, R extends IResult<S, L, D>, T> {

    String id();

    IScopeGraph.Immutable<S, L, D> scopeGraph();

    // Contains edges/data created by unit type checker only (i.e. not sub-unit edges/data)
    // TODO solve data duplication
    IScopeGraph.Immutable<S, L, D> localScopeGraph();

    Set<IRecordedQuery<S, L, D>> queries();

    List<S> rootScopes();

    @Nullable R analysis();

    @Nullable ScopeGraphDiff<S, L, D> diff();

    @Nullable StateCapture<S, L, D, T> localState();

    List<Throwable> failures();

    Map<String, IUnitResult<S, L, D, ?, ?>> subUnitResults();

    IUnitStats stats();

    TransitionTrace stateTransitionTrace();

    List<Throwable> allFailures();

    public enum TransitionTrace {
        OTHER, INITIALLY_STARTED, RESTARTED, RELEASED
    }

}