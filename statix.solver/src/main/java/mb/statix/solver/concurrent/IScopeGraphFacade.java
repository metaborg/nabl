package mb.statix.solver.concurrent;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import mb.statix.scopegraph.path.IResolutionPath;
import mb.statix.scopegraph.reference.Access;
import mb.statix.scopegraph.reference.DataLeq;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.scopegraph.reference.LabelWF;

public interface IScopeGraphFacade<S, L, D> {

    void openRootEdges(S root, Iterable<L> labels);

    CompletableFuture<S> freshScope(String name, Iterable<L> labels, Iterable<Access> data);

    void setDatum(S scope, D datum, Access access);

    void addEdge(S source, L label, S target);

    void closeEdge(S source, L label);

    CompletableFuture<Set<IResolutionPath<S, L, D>>> query(S scope, LabelWF<L> labelWF, DataWF<D> dataWF,
            LabelOrder<L> labelOrder, DataLeq<D> dataEquiv);

}