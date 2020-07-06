package mb.statix.concurrent._attic;

import java.util.Set;

import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.scopegraph.path.IResolutionPath;
import mb.statix.scopegraph.reference.Access;
import mb.statix.scopegraph.reference.DataLeq;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.scopegraph.reference.LabelWF;

public interface IScopeGraphFacade<S, L, D> {

    boolean hasPending();

    void openRootEdges(S root, Iterable<L> labels);

    IFuture<S> freshScope(String name, Iterable<L> labels, Iterable<Access> data);

    void setDatum(S scope, D datum, Access access);

    void addEdge(S source, L label, S target);

    void closeEdge(S source, L label);

    IFuture<Set<IResolutionPath<S, L, D>>> query(S scope, LabelWF<L> labelWF, DataWF<D> dataWF,
            LabelOrder<L> labelOrder, DataLeq<D> dataEquiv);

    default IScopeGraphFacade<S, L, D> getSubScopeGraph() {
        IScopeGraphFacade<S, L, D> outer = this;
        return new IScopeGraphFacade<S, L, D>() {

            private int pending = 0;

            @Override public boolean hasPending() {
                return pending > 0;
            }

            @Override public void openRootEdges(S root, Iterable<L> labels) {
                throw new UnsupportedOperationException("Not supported in sub scope graphs.");
            }

            @Override public IFuture<S> freshScope(String name, Iterable<L> labels, Iterable<Access> data) {
                throw new UnsupportedOperationException("Not supported in sub scope graphs.");
            }

            @Override public void setDatum(S scope, D datum, Access access) {
                throw new UnsupportedOperationException("Not supported in sub scope graphs.");
            }

            @Override public void addEdge(S source, L label, S target) {
                throw new UnsupportedOperationException("Not supported in sub scope graphs.");
            }

            @Override public void closeEdge(S source, L label) {
                throw new UnsupportedOperationException("Not supported in sub scope graphs.");
            }

            @Override public IFuture<Set<IResolutionPath<S, L, D>>> query(S scope, LabelWF<L> labelWF, DataWF<D> dataWF,
                    LabelOrder<L> labelOrder, DataLeq<D> dataEquiv) {
                pending += 1;
                return outer.query(scope, labelWF, dataWF, labelOrder, dataEquiv).whenComplete((paths, ex) -> {
                    pending -= 1;
                });
            }

        };
    }

}