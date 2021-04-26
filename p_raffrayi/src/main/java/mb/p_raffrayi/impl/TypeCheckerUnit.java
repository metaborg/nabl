package mb.p_raffrayi.impl;

import static com.google.common.collect.Streams.stream;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.ICompletableFuture;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.unit.Unit;

import io.usethesource.capsule.Set;

import mb.p_raffrayi.IScopeGraphLibrary;
import mb.p_raffrayi.ITypeChecker;
import mb.p_raffrayi.ITypeCheckerContext;
import mb.p_raffrayi.IUnitResult;
import mb.p_raffrayi.actors.IActor;
import mb.p_raffrayi.actors.IActorRef;
import mb.p_raffrayi.impl.tokens.Query;
import mb.p_raffrayi.impl.tokens.Snapshot;
import mb.p_raffrayi.nameresolution.DataLeq;
import mb.p_raffrayi.nameresolution.DataWf;
import mb.scopegraph.ecoop21.LabelOrder;
import mb.scopegraph.ecoop21.LabelWf;
import mb.scopegraph.oopsla20.IScopeGraph;
import mb.scopegraph.oopsla20.IScopeGraph.Immutable;
import mb.scopegraph.oopsla20.path.IResolutionPath;
import mb.scopegraph.oopsla20.reference.EdgeOrData;
import mb.scopegraph.oopsla20.reference.Env;
import mb.scopegraph.oopsla20.reference.ScopeGraph;
import mb.scopegraph.oopsla20.terms.newPath.ScopePath;

class TypeCheckerUnit<S, L, D, R> extends AbstractUnit<S, L, D, R> implements ITypeCheckerContext<S, L, D> {


    private static final ILogger logger = LoggerUtils.logger(TypeCheckerUnit.class);

    private final ITypeChecker<S, L, D, R> typeChecker;

    private volatile UnitState state;

    private final ICompletableFuture<IScopeGraph.Immutable<S, L, D>> localScopeGraphCapture = new CompletableFuture<>();

    TypeCheckerUnit(IActor<? extends IUnit<S, L, D, R>> self, @Nullable IActorRef<? extends IUnit<S, L, D, ?>> parent,
            IUnitContext<S, L, D> context, ITypeChecker<S, L, D, R> unitChecker, Iterable<L> edgeLabels) {
        super(self, parent, context, edgeLabels);
        this.typeChecker = unitChecker;
        this.state = UnitState.INIT;
    }

    @Override protected IFuture<D> getExternalDatum(D datum) {
        return typeChecker.getExternalDatum(datum);
    }

    ///////////////////////////////////////////////////////////////////////////
    // IBroker2UnitProtocol interface, called by IBroker implementations
    ///////////////////////////////////////////////////////////////////////////

    @Override public IFuture<IUnitResult<S, L, D, R>> _start(List<S> rootScopes) {
        assertInState(UnitState.INIT);
        resume();

        state = UnitState.ACTIVE;
        doStart(rootScopes);
        final IFuture<R> result = this.typeChecker.run(this, rootScopes).whenComplete((r, ex) -> {
            state = UnitState.DONE;
        });

        waitFor(Snapshot.of(self), self);
        localScopeGraphCapture.whenComplete((sg, ex) -> {
            logger.info("Captured local interface:\n{}", sg);
        });

        return doFinish(result);
    }

    ///////////////////////////////////////////////////////////////////////////
    // ITypeCheckerContext interface, called by ITypeChecker implementations
    ///////////////////////////////////////////////////////////////////////////

    // NB. Invoke methods via `local` so that we have the same scheduling & ordering
    // guarantees as for remote calls.

    @Override public String id() {
        return self.id();
    }

    @Override public <Q> IFuture<IUnitResult<S, L, D, Q>> add(String id, ITypeChecker<S, L, D, Q> unitChecker,
            List<S> rootScopes) {
        assertInState(UnitState.ACTIVE);

        final IFuture<IUnitResult<S, L, D, Q>> result = this.<Q>doAddSubUnit(id, (subself, subcontext) -> {
            return new TypeCheckerUnit<>(subself, self, subcontext, unitChecker, edgeLabels);
        }, rootScopes)._2();

        return ifActive(result);
    }

    @Override public IFuture<IUnitResult<S, L, D, Unit>> add(String id, IScopeGraphLibrary<S, L, D> library,
            List<S> rootScopes) {
        assertInState(UnitState.ACTIVE);

        final IFuture<IUnitResult<S, L, D, Unit>> result = this.<Unit>doAddSubUnit(id, (subself, subcontext) -> {
            return new ScopeGraphLibraryUnit<>(subself, self, subcontext, edgeLabels, library);
        }, rootScopes)._2();

        return ifActive(result);
    }

    @Override public void initScope(S root, Iterable<L> labels, boolean sharing) {
        assertInState(UnitState.ACTIVE);

        final List<EdgeOrData<L>> edges = stream(labels).map(EdgeOrData::edge).collect(Collectors.toList());

        doInitShare(self, root, edges, sharing);
    }

    @Override public S freshScope(String baseName, Iterable<L> edgeLabels, boolean data, boolean sharing) {
        assertInState(UnitState.ACTIVE);

        final S scope = doFreshScope(baseName, edgeLabels, data, sharing);

        return scope;
    }

    @Override public void shareLocal(S scope) {
        assertInState(UnitState.ACTIVE);

        doAddShare(self, scope);
    }

    @Override public void setDatum(S scope, D datum) {
        assertInState(UnitState.ACTIVE);

        doSetDatum(scope, datum);
    }

    @Override public void addEdge(S source, L label, S target) {
        assertInState(UnitState.ACTIVE);

        doAddEdge(self, source, label, target);
    }

    @Override public void closeEdge(S source, L label) {
        assertInState(UnitState.ACTIVE);

        doCloseLabel(self, source, EdgeOrData.edge(label));
    }

    @Override public void closeScope(S scope) {
        assertInState(UnitState.ACTIVE);

        doCloseScope(self, scope);
    }

    @Override public IFuture<Set<IResolutionPath<S, L, D>>> query(S scope, LabelWf<L> labelWF, LabelOrder<L> labelOrder,
            DataWf<S, L, D> dataWF, DataLeq<S, L, D> dataEquiv, @Nullable DataWf<S, L, D> dataWfInternal,
            @Nullable DataLeq<S, L, D> dataEquivInternal) {
        assertInState(UnitState.ACTIVE);

        final ScopePath<S, L> path = new ScopePath<>(scope);

        final IFuture<Env<S, L, D>> result =
                doQuery(self, path, labelWF, labelOrder, dataWF, dataEquiv, dataWfInternal, dataEquivInternal);
        final IFuture<Env<S, L, D>> ret;
        if(result.isDone()) {
            ret = result;
        } else {
            final Query<S, L, D> wf = Query.of(self, path, labelWF, dataWF, labelOrder, dataEquiv, result);
            waitFor(wf, self);
            ret = result.whenComplete((env, ex) -> {
                granted(wf, self);
            });
        }
        stats.localQueries += 1;
        return ifActive(afterCapture(ret)).thenApply(CapsuleUtil::toSet);
    }

    private void capture() {
        IScopeGraph.Transient<S, L, D> snapshot = ScopeGraph.Transient.of();
        scopeGraph.get().getEdges().forEach((src_lbl, tgts) -> {
            final S src = src_lbl.getKey();
            final L lbl = src_lbl.getValue();
            if(isEdgeClosed(src, EdgeOrData.edge(lbl))) {
                tgts.forEach(tgt -> snapshot.addEdge(src, lbl, tgt));
            }
        });
        scopeGraph.get().getData().forEach((s, d) -> {
            snapshot.setDatum(s, typeChecker.explicate(d));
        });
        localScopeGraphCapture.complete(snapshot.freeze());
        granted(Snapshot.of(self), self);
        resume();
    }

    private <Q> IFuture<Q> afterCapture(IFuture<Q> result) {
        return localScopeGraphCapture.thenCompose(__ -> result);
    }

    @Override protected IFuture<Immutable<S, L, D>> localCapture() {
        return localScopeGraphCapture;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Deadlock
    ///////////////////////////////////////////////////////////////////////////

    @Override public java.util.Set<IActorRef<? extends IUnit<S, L, D, ?>>> dependentSet() {
        if(isWaitingFor(Snapshot.of(self))) {
            return Collections.singleton(self);
        }
        return super.dependentSet();
    }

    protected void handleSelfDeadlocked(java.util.Set<IActorRef<? extends IUnit<S, L, D, ?>>> nodes) {
        if(isWaitingFor(Snapshot.of(self))) {
            capture();
        } else {
            super.handleSelfDeadlocked(nodes);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Assertions
    ///////////////////////////////////////////////////////////////////////////

    private void assertInState(UnitState s) {
        if(!state.equals(s)) {
            logger.error("Expected state {}, was {}", s, state);
            throw new IllegalStateException("Expected state " + s + ", was " + state);
        }
    }

    private <Q> IFuture<Q> ifActive(IFuture<Q> result) {
        return result.compose((r, ex) -> {
            if(state.equals(UnitState.ACTIVE)) {
                return CompletableFuture.completed(r, ex);
            } else {
                return CompletableFuture.noFuture();
            }
        });
    }

    ///////////////////////////////////////////////////////////////////////////
    // toString
    ///////////////////////////////////////////////////////////////////////////

    @Override public String toString() {
        return "TypeCheckerUnit{" + self.id() + "}";
    }

}