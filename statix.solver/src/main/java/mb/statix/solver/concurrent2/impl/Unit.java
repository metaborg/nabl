package mb.statix.solver.concurrent2.impl;

import java.util.Set;

import mb.statix.scopegraph.path.IResolutionPath;
import mb.statix.scopegraph.reference.Access;
import mb.statix.scopegraph.reference.DataLeq;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.solver.concurrent.util.IFuture;
import mb.statix.solver.concurrent2.IUnit;

public class Unit<S, L, D> implements IUnit<S, L, D> {

    private IBrokerProtocol broker;
    
    // scope graph
    // open edges
    // open data
    // open sub units

    ///////////////////////////////////////////////////////////////////////////
    // 
    ///////////////////////////////////////////////////////////////////////////
    
    ///////////////////////////////////////////////////////////////////////////
    // IUnit interface, called by ITypeChecker implementations
    ///////////////////////////////////////////////////////////////////////////
    
    @Override public void initSharedScope(S scope, Iterable<L> labels) {
        // scope was shared && we are not owner
        // add (scope, labels) to open edges
        // send INIT(scope, labels) to scope.owner
    }

    @Override public S freshScope(String baseName, Iterable<L> labels, Iterable<Access> data) {
        // create fresh scope
        // add open edges
        // add open data
    }

    @Override public void setDatum(S scope, D datum, Access access) {
        // (scope, access) is open
        // access = PUBLIC ==> (scope, PRIVATE) is not open
    }

    @Override public void addEdge(S source, L label, S target) {
        // (scope, label) is open
        // 
    }

    @Override public void closeEdge(S source, L label) {
        // (scope, label) is open
        // remove (scope, label) from open edges
        // if scope.owner != self:
        //   send CLOSE_EDGE(scope, label) to scope.owner
    }

    @Override public IFuture<Set<IResolutionPath<S, L, D>>> query(S scope, LabelWF<L> labelWF, DataWF<D> dataWF,
            LabelOrder<L> labelOrder, DataLeq<D> dataEquiv) {
    }


}