package mb.statix.concurrent.p_raffrayi;

import java.util.Set;

import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.scopegraph.path.IResolutionPath;
import mb.statix.scopegraph.reference.Access;
import mb.statix.scopegraph.reference.DataLeq;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.scopegraph.reference.LabelWF;

/**
 * The interface from the system to the type checkers.
 */
public interface ITypeCheckerContext<S, L, D, R> {

    /**
     * Return id of the current unit.
     */
    String id();

    /**
     * Start sub type-checker, with the given root scope.
     */
    void add(String id, ITypeChecker<S, L, D, R> unitChecker, S root);

    /**
     * Initialize root scope.
     */
    void initRoot(S root, Iterable<L> labels, boolean shared);

    /**
     * Create fresh scope, declaring open edges and data, and sharing with sub type checkers.
     */
    S freshScope(String baseName, Iterable<L> labels, Iterable<Access> data, boolean shared);

    /**
     * Set datum of a scope. Scope must be open for data at given access level. Datum is automatically closed by setting
     * it.
     */
    void setDatum(S scope, D datum, Access access);

    /**
     * Add edge. Source scope must be open for this label.
     */
    void addEdge(S source, L label, S target);

    /**
     * Close open label for the given scope.
     */
    void closeEdge(S source, L label);

    /**
     * Declare scope to be closed for sharing.
     */
    void closeShare(S scope);

    /**
     * Execute scope graph query in the given scope.
     */
    IFuture<? extends Set<IResolutionPath<S, L, D>>> query(S scope, LabelWF<L> labelWF, DataWF<D> dataWF,
            LabelOrder<L> labelOrder, DataLeq<D> dataEquiv);

    default ITypeCheckerContext<S, L, D, R> subContext(String subId) {
        final ITypeCheckerContext<S, L, D, R> outer = this;
        return new ITypeCheckerContext<S, L, D, R>() {

            private final String id = outer.id() + "#" + subId;

            @Override public String id() {
                return id;
            }

            @Override public void add(String id, ITypeChecker<S, L, D, R> unitChecker, S root) {
                throw new UnsupportedOperationException("Unsupported in sub-contexts.");
            }

            @Override public void initRoot(S root, Iterable<L> labels, boolean shared) {
                throw new UnsupportedOperationException("Unsupported in sub-contexts.");
            }

            @Override public S freshScope(String baseName, Iterable<L> labels, Iterable<Access> data, boolean shared) {
                throw new UnsupportedOperationException("Unsupported in sub-contexts.");
            }

            @Override public void setDatum(S scope, D datum, Access access) {
                throw new UnsupportedOperationException("Unsupported in sub-contexts.");
            }

            @Override public void addEdge(S source, L label, S target) {
                throw new UnsupportedOperationException("Unsupported in sub-contexts.");
            }

            @Override public void closeEdge(S source, L label) {
                throw new UnsupportedOperationException("Unsupported in sub-contexts.");
            }

            @Override public void closeShare(S scope) {
                throw new UnsupportedOperationException("Unsupported in sub-contexts.");
            }

            @Override public IFuture<? extends Set<IResolutionPath<S, L, D>>> query(S scope, LabelWF<L> labelWF,
                    DataWF<D> dataWF, LabelOrder<L> labelOrder, DataLeq<D> dataEquiv) {
                return outer.query(scope, labelWF, dataWF, labelOrder, dataEquiv);
            }

        };
    }

}
