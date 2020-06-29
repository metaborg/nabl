package mb.statix.solver.concurrent2;

import java.util.Set;

import mb.statix.actors.IFuture;
import mb.statix.scopegraph.path.IResolutionPath;
import mb.statix.scopegraph.reference.Access;
import mb.statix.scopegraph.reference.DataLeq;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.scopegraph.reference.LabelWF;

/**
 * The interface from the system to the type checkers.
 */
public interface IClientProtocol<S, L, D> {

    /**
     * Initialize root scope.
     */
    void initRoot(S root, Iterable<L> labels);

    /**
     * Create fresh scope, declaring open edges and data.
     */
    S freshScope(String baseName, Iterable<L> labels, Iterable<Access> data);

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
     * Execute scope graph query in the given scope.
     */
    IFuture<Set<IResolutionPath<S, L, D>>> query(S scope, LabelWF<L> labelWF, DataWF<D> dataWF,
            LabelOrder<L> labelOrder, DataLeq<D> dataEquiv);

    // FIXME done()
    
    // FIXME fail()
    
}
