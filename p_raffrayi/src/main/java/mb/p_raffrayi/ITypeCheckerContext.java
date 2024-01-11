package mb.p_raffrayi;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import jakarta.annotation.Nullable;

import org.metaborg.util.future.IFuture;
import org.metaborg.util.unit.Unit;

import mb.p_raffrayi.ITypeChecker.IOutput;
import mb.p_raffrayi.ITypeChecker.IState;
import mb.p_raffrayi.impl.Result;
import mb.p_raffrayi.nameresolution.DataLeq;
import mb.p_raffrayi.nameresolution.DataWf;
import mb.p_raffrayi.nameresolution.IQuery;
import mb.scopegraph.ecoop21.LabelOrder;
import mb.scopegraph.ecoop21.LabelWf;
import mb.scopegraph.library.IScopeGraphLibrary;
import mb.scopegraph.oopsla20.path.IResolutionPath;
import mb.scopegraph.resolution.StateMachine;

/**
 * The interface from the system to the type checkers.
 */
public interface ITypeCheckerContext<S, L, D> {

    /**
     * Return id of the current unit.
     */
    String id();

    /**
     * Start sub unit with the given type-checker, root scopes and changed marker.
     */
    <R extends IOutput<S, L, D>, T extends IState<S, L, D>> IFuture<IUnitResult<S, L, D, Result<S, L, D, R, T>>>
            add(String id, ITypeChecker<S, L, D, R, T> unitChecker, List<S> rootScopes, boolean changed);

    /**
     * Start sub unit with the given type-checker, root scopes, marked as changed.
     */
    default <R extends IOutput<S, L, D>, T extends IState<S, L, D>> IFuture<IUnitResult<S, L, D, Result<S, L, D, R, T>>>
            add(String id, ITypeChecker<S, L, D, R, T> unitChecker, List<S> rootScopes) {
        return add(id, unitChecker, rootScopes, true);
    }

    /**
     * Start sub unit with the given static scope graph and root scopes.
     */
    IFuture<IUnitResult<S, L, D, Unit>> add(String id, IScopeGraphLibrary<S, L, D> library, List<S> rootScopes);

    /**
     * Initialize root scope.
     */
    void initScope(S root, Collection<L> labels, boolean shared);

    /**
     * Create fresh scope, declaring open edges and data, and sharing with sub type checkers.
     */
    S freshScope(String baseName, Iterable<L> labels, boolean data, boolean shared);

    /**
     * Create fresh scope with stable identity, declaring open edges and data, and sharing with sub type checkers. Will
     * automatically be marked as shared.
     *
     * Will throw when identity is already used previously.
     */
    S stableFreshScope(String name, Iterable<L> labels, boolean data);

    /**
     * Set datum of a scope. Scope must be open for data at given access level. Datum is automatically closed by setting
     * it.
     */
    void setDatum(S scope, D datum);

    /**
     * Add edge. Source scope must be open for this label.
     */
    void addEdge(S source, L label, S target);

    /**
     * Indicate that the unit intends to initialize the scope again.
     */
    void shareLocal(S scope);

    /**
     * Close open label for the given scope.
     */
    void closeEdge(S source, L label);

    /**
     * Declare scope to be closed for sharing.
     */
    void closeScope(S scope);

    /**
     * Execute scope graph query in the given scope.
     *
     * It is important that the LabelWF, LabelOrder, DataWF, and DataLeq arguments are self-contained, static values
     * that do not leak references to the type checker, as this will break the actor abstraction.
     */
    default IFuture<? extends Set<IResolutionPath<S, L, D>>> query(S scope, LabelWf<L> labelWF,
            LabelOrder<L> labelOrder, DataWf<S, L, D> dataWF, DataLeq<S, L, D> dataEquiv) {
        return query(scope, labelWF, labelOrder, dataWF, dataEquiv, null, null);
    }

    /**
     * Execute scope graph query in the given scope.
     *
     * It is important that the LabelWF, LabelOrder, DataWF, and DataLeq arguments are self-contained, static values
     * that do not leak references to the type checker, as this will break the actor abstraction.
     *
     * The internal variants of these parameters are only executed on the local type checker, and may refer to the local
     * type checker state safely.
     */
    IFuture<? extends Set<IResolutionPath<S, L, D>>> query(S scope, IQuery<S, L, D> query, DataWf<S, L, D> dataWF,
            DataLeq<S, L, D> dataEquiv, DataWf<S, L, D> dataWfInternal, DataLeq<S, L, D> dataEquivInternal);

    /**
     * Execute interpreted scope graph query in the given scope.
     *
     * It is important that the LabelWF, LabelOrder, DataWF, and DataLeq arguments are self-contained, static values
     * that do not leak references to the type checker, as this will break the actor abstraction.
     *
     * The internal variants of these parameters are only executed on the local type checker, and may refer to the local
     * type checker state safely.
     */
    IFuture<? extends Set<IResolutionPath<S, L, D>>> query(S scope, LabelWf<L> labelWF, LabelOrder<L> labelOrder,
            DataWf<S, L, D> dataWF, DataLeq<S, L, D> dataEquiv, DataWf<S, L, D> dataWfInternal,
            DataLeq<S, L, D> dataEquivInternal);

    /**
     * Execute compiled scope graph query in the given scope.
     *
     * It is important that the LabelWF, LabelOrder, DataWF, and DataLeq arguments are self-contained, static values
     * that do not leak references to the type checker, as this will break the actor abstraction.
     *
     * The internal variants of these parameters are only executed on the local type checker, and may refer to the local
     * type checker state safely.
     */
    IFuture<? extends Set<IResolutionPath<S, L, D>>> query(S scope, StateMachine<L> stateMachine,
            DataWf<S, L, D> dataWF, DataLeq<S, L, D> dataEquiv, DataWf<S, L, D> dataWfInternal,
            DataLeq<S, L, D> dataEquivInternal);

    default ITypeCheckerContext<S, L, D> subContext(String subId) {
        final ITypeCheckerContext<S, L, D> outer = this;
        return new ITypeCheckerContext<S, L, D>() {

            private final String id = outer.id() + "#" + subId;

            @Override public String id() {
                return id;
            }

            @SuppressWarnings("unused") @Override public <R extends IOutput<S, L, D>, T extends IState<S, L, D>>
                    IFuture<IUnitResult<S, L, D, Result<S, L, D, R, T>>>
                    add(String id, ITypeChecker<S, L, D, R, T> unitChecker, List<S> rootScopes, boolean changed) {
                throw new UnsupportedOperationException("Unsupported in sub-contexts.");
            }

            @SuppressWarnings("unused") @Override public IFuture<IUnitResult<S, L, D, Unit>> add(String id,
                    IScopeGraphLibrary<S, L, D> library, List<S> rootScopes) {
                throw new UnsupportedOperationException("Unsupported in sub-contexts.");
            }

            @SuppressWarnings("unused") @Override public void initScope(S root, Collection<L> labels, boolean shared) {
                throw new UnsupportedOperationException("Unsupported in sub-contexts.");
            }

            @SuppressWarnings("unused") @Override public S freshScope(String baseName, Iterable<L> labels, boolean data,
                    boolean shared) {
                throw new UnsupportedOperationException("Unsupported in sub-contexts.");
            }

            @SuppressWarnings("unused") @Override public S stableFreshScope(String name, Iterable<L> labels,
                    boolean data) {
                throw new UnsupportedOperationException("Unsupported in sub-contexts.");
            }

            @SuppressWarnings("unused") @Override public void shareLocal(S scope) {
                throw new UnsupportedOperationException("Unsupported in sub-contexts.");
            }

            @SuppressWarnings("unused") @Override public void setDatum(S scope, D datum) {
                throw new UnsupportedOperationException("Unsupported in sub-contexts.");
            }

            @SuppressWarnings("unused") @Override public void addEdge(S source, L label, S target) {
                throw new UnsupportedOperationException("Unsupported in sub-contexts.");
            }

            @SuppressWarnings("unused") @Override public void closeEdge(S source, L label) {
                throw new UnsupportedOperationException("Unsupported in sub-contexts.");
            }

            @SuppressWarnings("unused") @Override public void closeScope(S scope) {
                throw new UnsupportedOperationException("Unsupported in sub-contexts.");
            }

            @Override public IFuture<? extends Set<IResolutionPath<S, L, D>>> query(S scope, IQuery<S, L, D> query,
                    DataWf<S, L, D> dataWF, DataLeq<S, L, D> dataEquiv, DataWf<S, L, D> dataWfInternal,
                    DataLeq<S, L, D> dataEquivInternal) {
                return outer.query(scope, query, dataWF, dataEquiv, null, null);
            }

            @Override public IFuture<? extends Set<IResolutionPath<S, L, D>>> query(S scope,
                    StateMachine<L> stateMachine, DataWf<S, L, D> dataWF, DataLeq<S, L, D> dataEquiv,
                    DataWf<S, L, D> dataWfInternal, DataLeq<S, L, D> dataEquivInternal) {
                return outer.query(scope, stateMachine, dataWF, dataEquiv, null, null);
            }

            @Override public IFuture<? extends Set<IResolutionPath<S, L, D>>> query(S scope, LabelWf<L> labelWF,
                    LabelOrder<L> labelOrder, DataWf<S, L, D> dataWF, DataLeq<S, L, D> dataEquiv,
                    @Nullable DataWf<S, L, D> dataWfInternal, @Nullable DataLeq<S, L, D> dataEquivInternal) {
                return outer.query(scope, labelWF, labelOrder, dataWF, dataEquiv, null, null);
            }

        };
    }

}
