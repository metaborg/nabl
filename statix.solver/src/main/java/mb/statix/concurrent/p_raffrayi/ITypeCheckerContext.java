package mb.statix.concurrent.p_raffrayi;

import java.util.List;
import java.util.Set;

import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.concurrent.p_raffrayi.impl.IInitialState;
import mb.statix.concurrent.p_raffrayi.nameresolution.DataLeq;
import mb.statix.concurrent.p_raffrayi.nameresolution.DataLeqInternal;
import mb.statix.concurrent.p_raffrayi.nameresolution.DataWf;
import mb.statix.concurrent.p_raffrayi.nameresolution.DataWfInternal;
import mb.statix.concurrent.p_raffrayi.nameresolution.LabelOrder;
import mb.statix.concurrent.p_raffrayi.nameresolution.LabelWf;
import mb.statix.scopegraph.path.IResolutionPath;

/**
 * The interface from the system to the type checkers.
 */
public interface ITypeCheckerContext<S, L, D> {

    /**
     * Return id of the current unit.
     */
    String id();

    /**
     * Start sub type-checker, with the given root scope and previous result.
     */
    <R> IFuture<IUnitResult<S, L, D, R>> add(String id, ITypeChecker<S, L, D, R> unitChecker,
    		List<S> rootScopes, IInitialState<S, L, D, R> initialState);

    /**
     * Start sub type-checker, with the given root scope and no previous result.
     */
    default <R> IFuture<IUnitResult<S, L, D, R>> add(String id, ITypeChecker<S, L, D, R> unitChecker, List<S> rootScopes) {
    	return add(id, unitChecker, rootScopes, null);
    }

    /**
     * Initialize root scope.
     */
    void initScope(S root, Iterable<L> labels, boolean shared);

    /**
     * Create fresh scope, declaring open edges and data, and sharing with sub type checkers.
     */
    S freshScope(String baseName, Iterable<L> labels, boolean data, boolean shared);

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
     */
    default IFuture<? extends Set<IResolutionPath<S, L, D>>> query(S scope, LabelWf<L> labelWF,
            LabelOrder<L> labelOrder, DataWf<D> dataWF, DataLeq<D> dataEquiv) {
        return query(scope, labelWF, labelOrder, dataWF, dataEquiv, null, null);
    }

    IFuture<? extends Set<IResolutionPath<S, L, D>>> query(S scope, LabelWf<L> labelWF, LabelOrder<L> labelOrder,
            DataWf<D> dataWF, DataLeq<D> dataEquiv, DataWfInternal<D> dataWfInternal,
            DataLeqInternal<D> dataEquivInternal);

    default ITypeCheckerContext<S, L, D> subContext(String subId) {
        final ITypeCheckerContext<S, L, D> outer = this;
        return new ITypeCheckerContext<S, L, D>() {

            private final String id = outer.id() + "#" + subId;

            @Override public String id() {
                return id;
            }

            @Override public <R> IFuture<IUnitResult<S, L, D, R>> add(String id, ITypeChecker<S, L, D, R> unitChecker,
                    List<S> rootScopes, IInitialState<S, L, D, R> initialState) {
                throw new UnsupportedOperationException("Unsupported in sub-contexts.");
            }

            @Override public void initScope(S root, Iterable<L> labels, boolean shared) {
                throw new UnsupportedOperationException("Unsupported in sub-contexts.");
            }

            @Override public S freshScope(String baseName, Iterable<L> labels, boolean data, boolean shared) {
                throw new UnsupportedOperationException("Unsupported in sub-contexts.");
            }

            @Override public void shareLocal(S scope) {
                throw new UnsupportedOperationException("Unsupported in sub-contexts.");
            }

            @Override public void setDatum(S scope, D datum) {
                throw new UnsupportedOperationException("Unsupported in sub-contexts.");
            }

            @Override public void addEdge(S source, L label, S target) {
                throw new UnsupportedOperationException("Unsupported in sub-contexts.");
            }

            @Override public void closeEdge(S source, L label) {
                throw new UnsupportedOperationException("Unsupported in sub-contexts.");
            }

            @Override public void closeScope(S scope) {
                throw new UnsupportedOperationException("Unsupported in sub-contexts.");
            }

            @Override public IFuture<? extends Set<IResolutionPath<S, L, D>>> query(S scope, LabelWf<L> labelWF,
                    LabelOrder<L> labelOrder, DataWf<D> dataWF, DataLeq<D> dataEquiv, DataWfInternal<D> dataWfInternal,
                    DataLeqInternal<D> dataEquivInternal) {
                return outer.query(scope, labelWF, labelOrder, dataWF, dataEquiv, dataWfInternal, dataEquivInternal);
            }

        };
    }

}
