package mb.p_raffrayi;

import java.util.List;
import java.util.Set;
import java.util.Map;

import javax.annotation.Nullable;

import mb.scopegraph.oopsla20.IScopeGraph;
import mb.scopegraph.oopsla20.diff.ScopeGraphDiff;

public interface IUnitResult<S, L, D, R> {

    String id();

    IScopeGraph.Immutable<S, L, D> scopeGraph();

    // contains edges/data created by unit type checker only (i.e. not sub-unit edges/data)
    // TODO solve data duplication
    IScopeGraph.Immutable<S, L, D> localScopeGraph();

    Set<IRecordedQuery<S, L, D>> queries();

    List<S> rootScopes();

    @Nullable R analysis();

    @Nullable ScopeGraphDiff<S, L, D> diff();

    List<Throwable> failures();

    Map<String, IUnitResult<S, L, D, ?>> subUnitResults();

    IUnitStats stats();

    Transitions transitions();

    List<Throwable> allFailures();

    public enum Transitions {
        OTHER, INITIALLY_STARTED, RESTARTED, RELEASED
    }

}