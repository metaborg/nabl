package mb.statix.solver.concurrent2.impl;

import mb.statix.actors.CompletableFuture;
import mb.statix.actors.IFuture;
import mb.statix.scopegraph.reference.Access;
import mb.statix.solver.concurrent2.ITypeChecker;
import mb.statix.solver.concurrent2.IUnit;

public class Unit<S, L, D> implements IUnit<S, L, D>, IUnitProtocol<S, L, D> {

    private final IBrokerProtocol<S, L, D> broker;
    private final ITypeChecker<S, L, D> unitChecker;

    // scope graph
    // open edges
    // open data
    // open sub units

    public Unit(IBrokerProtocol<S, L, D> broker, ITypeChecker<S, L, D> unitChecker) {
        this.broker = broker;
        this.unitChecker = unitChecker;
    }

    ///////////////////////////////////////////////////////////////////////////
    // IUnitProtocol, called by other units
    ///////////////////////////////////////////////////////////////////////////

    @Override public void start(S root) {
        try {
            unitChecker.run(this, root);
        } catch(InterruptedException e) {
            broker.fail();
        }
    }

    @Override public IFuture<String> query(S scope, String text, Access access) {
        System.out.println("Received query " + scope + " " + text);
        CompletableFuture<String> result = new CompletableFuture<>();
        result.complete("Hello!");
        return result;
    }

    ///////////////////////////////////////////////////////////////////////////
    // IUnit interface, called by ITypeChecker implementations
    ///////////////////////////////////////////////////////////////////////////

    @Override public IFuture<String> query(S scope, String text) {
        final String owner = broker.scopeImpl().id(scope);
        final Access access = owner.equals(broker.id()) ? Access.INTERNAL : Access.EXTERNAL;
        return broker.get(owner).query(scope, text, access);
    }

    //    @Override public void initSharedScope(S scope, Iterable<L> labels) {
    //        // scope was shared && we are not owner
    //        // add (scope, labels) to open edges
    //        // send INIT(scope, labels) to scope.owner
    //    }
    //
    //    @Override public S freshScope(String baseName, Iterable<L> labels, Iterable<Access> data) {
    //        // create fresh scope
    //        // add open edges
    //        // add open data
    //    }
    //
    //    @Override public void setDatum(S scope, D datum, Access access) {
    //        // (scope, access) is open
    //        // access = PUBLIC ==> (scope, PRIVATE) is not open
    //    }
    //
    //    @Override public void addEdge(S source, L label, S target) {
    //        // (scope, label) is open
    //        // 
    //    }
    //
    //    @Override public void closeEdge(S source, L label) {
    //        // (scope, label) is open
    //        // remove (scope, label) from open edges
    //        // if scope.owner != self:
    //        //   send CLOSE_EDGE(scope, label) to scope.owner
    //    }
    //
    //    @Override public IFuture<Set<IResolutionPath<S, L, D>>> query(S scope, LabelWF<L> labelWF, DataWF<D> dataWF,
    //            LabelOrder<L> labelOrder, DataLeq<D> dataEquiv) {
    //    }


}