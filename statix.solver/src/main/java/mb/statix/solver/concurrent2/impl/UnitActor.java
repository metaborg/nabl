package mb.statix.solver.concurrent2.impl;

import static com.google.common.collect.Streams.stream;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

import mb.nabl2.util.collections.MultiSet;
import mb.statix.actors.IActor;
import mb.statix.actors.IActorRef;
import mb.statix.actors.futures.IFuture;
import mb.statix.scopegraph.path.IResolutionPath;
import mb.statix.scopegraph.reference.Access;
import mb.statix.scopegraph.reference.DataLeq;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.EdgeOrData;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.scopegraph.terms.path.Paths;
import mb.statix.solver.concurrent2.ITypeChecker;

public class UnitActor<S, L, D> extends AbstractClient<S, L, D> implements IUnit<S, L, D> {

    private final IActor<? extends IUnit<S, L, D>> self;
    private final IUnitContext<S, L, D> context;
    private final ITypeChecker<S, L, D, ?> unitChecker;

    private final MultiSet.Transient<String> scopeNameCounters;

    public UnitActor(IActor<? extends IUnit<S, L, D>> self, IActorRef<? extends IUnit2UnitProtocol<S, L, D>> parent,
            IUnitContext<S, L, D> context, ITypeChecker<S, L, D, ?> unitChecker, S root, Iterable<L> edgeLabels) {
        super(self, parent, context, root, edgeLabels);
        this.self = self;
        this.context = context;
        this.unitChecker = unitChecker;

        this.scopeNameCounters = MultiSet.Transient.of();
    }

    ///////////////////////////////////////////////////////////////////////////
    // IClientProtocol interface, called by ITypeChecker implementations
    ///////////////////////////////////////////////////////////////////////////

    @Override public void initRoot(S root, Iterable<L> labels) {
        _initRoot(root, labels, self);
    }

    @Override public S freshScope(String baseName, Iterable<L> labels, Iterable<Access> data) {
        final String name = baseName.replace("-", "_");
        final int n = scopeNameCounters.add(name);
        final S scope = context.makeScope(name + "-" + n);

        stream(labels).map(EdgeOrData::edge).forEach(edge -> openEdge(scope, edge));
        stream(data).map(EdgeOrData::<L>data).forEach(edge -> openEdge(scope, edge));

        return scope;
    }

    @Override public void setDatum(S scope, D datum, Access access) {
        _setDatum(scope, datum, access);
    }

    @Override public void addEdge(S source, L label, S target) {
        _addEdge(source, label, target);
    }

    @Override public void closeEdge(S source, L label) {
        _closeEdge(source, label);
    }

    @Override public IFuture<Set<IResolutionPath<S, L, D>>> query(S scope, LabelWF<L> labelWF, DataWF<D> dataWF,
            LabelOrder<L> labelOrder, DataLeq<D> dataEquiv) {
        return _query(Paths.empty(scope), labelWF, dataWF, labelOrder, dataEquiv).thenApply(ImmutableSet::copyOf);
    }

    ///////////////////////////////////////////////////////////////////////////
    // IUnitProtocol interface, called by IUnit implementations
    ///////////////////////////////////////////////////////////////////////////

    @Override public void _start() {
        try {
            context.waitForInit(self, root());
            openScope(root(), self);
            this.unitChecker.run(this, root());
        } catch(InterruptedException e) {
            // FIXME Handle this
            e.printStackTrace();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Deadlock
    ///////////////////////////////////////////////////////////////////////////

}