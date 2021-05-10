package mb.p_raffrayi.impl;

import static com.google.common.collect.Streams.stream;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.functions.Function2;
import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.Futures;
import org.metaborg.util.future.ICompletableFuture;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.unit.Unit;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import mb.p_raffrayi.IIncrementalTypeCheckerContext;
import mb.p_raffrayi.IScopeGraphLibrary;
import mb.p_raffrayi.IScopeImpl;
import mb.p_raffrayi.ITypeChecker;
import mb.p_raffrayi.IUnitResult;
import mb.p_raffrayi.actors.IActor;
import mb.p_raffrayi.actors.IActorRef;
import mb.p_raffrayi.impl.diff.IScopeGraphDifferOps;
import mb.p_raffrayi.impl.tokens.Activate;
import mb.p_raffrayi.impl.tokens.IWaitFor;
import mb.p_raffrayi.impl.tokens.Query;
import mb.p_raffrayi.nameresolution.DataLeq;
import mb.p_raffrayi.nameresolution.DataWf;
import mb.scopegraph.ecoop21.LabelOrder;
import mb.scopegraph.ecoop21.LabelWf;
import mb.scopegraph.oopsla20.IScopeGraph;
import mb.scopegraph.oopsla20.diff.BiMap;
import mb.scopegraph.oopsla20.path.IResolutionPath;
import mb.scopegraph.oopsla20.reference.EdgeOrData;
import mb.scopegraph.oopsla20.reference.Env;
import mb.scopegraph.oopsla20.reference.ScopeGraph;
import mb.scopegraph.oopsla20.terms.newPath.ScopePath;

class TypeCheckerUnit<S, L, D, R> extends AbstractUnit<S, L, D, R>
        implements IIncrementalTypeCheckerContext<S, L, D, R> {


    private static final ILogger logger = LoggerUtils.logger(TypeCheckerUnit.class);

    private final ITypeChecker<S, L, D, R> typeChecker;

    private volatile UnitState state;

    private final IScopeImpl<S, D> scopeImpl; // TODO: remove field, and move methods to IUnitContext?
    private final IScopeGraphDifferOps<S, D> differOps;

    private final ICompletableFuture<Unit> whenActive = new CompletableFuture<>();
    private final ICompletableFuture<Boolean> confirmationResult = new CompletableFuture<>();

    TypeCheckerUnit(IActor<? extends IUnit<S, L, D, R>> self, @Nullable IActorRef<? extends IUnit<S, L, D, ?>> parent,
            IUnitContext<S, L, D> context, ITypeChecker<S, L, D, R> unitChecker, Iterable<L> edgeLabels,
            IInitialState<S, L, D, R> initialState, IScopeImpl<S, D> scopeImpl, IScopeGraphDifferOps<S, D> differOps) {
        super(self, parent, context, edgeLabels, initialState, differOps);
        this.typeChecker = unitChecker;
        this.differOps = differOps;
        this.scopeImpl = scopeImpl;
        this.state = UnitState.INIT_UNIT;
    }

    @Override protected IFuture<D> getExternalDatum(D datum) {
        return typeChecker.getExternalDatum(datum);
    }

    ///////////////////////////////////////////////////////////////////////////
    // IBroker2UnitProtocol interface, called by IBroker implementations
    ///////////////////////////////////////////////////////////////////////////

    @Override public IFuture<IUnitResult<S, L, D, R>> _start(List<S> rootScopes) {
        assertInState(UnitState.INIT_UNIT);
        resume();

        doStart(rootScopes);
        state = UnitState.INIT_TC;
        final IFuture<R> result = this.typeChecker.run(this, rootScopes).whenComplete((r, ex) -> {
            state = UnitState.DONE;
        });

        if(state == UnitState.INIT_TC) {
            // runIncremental not called, so start eagerly
            doRestart();
        } else if (state == UnitState.DONE) {
            // Completed synchronously
            whenActive.complete(Unit.unit);
            confirmationResult.complete(true);
        }

        if(!whenActive.isDone()) {
            final Activate<S, L, D> activate = Activate.of(self, whenActive);
            waitFor(activate, self);
            whenActive.whenComplete((u, ex) -> {
                granted(activate, self);
                resume();
            });
        }

        return doFinish(result);
    }

    ///////////////////////////////////////////////////////////////////////////
    // IUnit2UnitProtocol interface, called by IUnit implementations
    ///////////////////////////////////////////////////////////////////////////

    @Override public IFuture<Env<S, L, D>> _confirm(ScopePath<S, L> path, LabelWf<L> labelWF, DataWf<S, L, D> dataWF,
            LabelOrder<L> labelOrder, DataLeq<S, L, D> dataEquiv) {
        stats.incomingConfirmations++;
        return whenActive.thenCompose(__ -> doQuery(self.sender(TYPE), path, labelWF, labelOrder, dataWF, dataEquiv, null, null));
    }

    @Override public IFuture<Boolean> _requireRestart() {
        return CompletableFuture.completedFuture(state.equals(UnitState.ACTIVE));
    }

    @Override public void _restart() {
        doRestart();
    }

    @Override public void _release() {
        // TODO: collect patches (as part of _waitForLocalState???)
        // TODO: what if message received, but not part of 'real' deadlock cluster?
        doRelease(CapsuleUtil.immutableMap());
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
            List<S> rootScopes, IInitialState<S, L, D, Q> initialState) {
        assertActive();

        initialState.previousResult().map(IUnitResult::rootScopes).ifPresent(previousRootScopes -> {
            // When a scope is shared, the shares must be consistent.
            // Also, it is not necessary that shared scopes are reachable from the root scopes
            // (A unit started by the Broker does not even have root scopes)
            // Therefore we enforce here that the current root scopes and the previous ones match.

            int pSize = previousRootScopes.size();
            int cSize = rootScopes.size();
            if(cSize != pSize) {
                logger.error("Unit {} adds subunit {} with initial state but with different root scope count.");
                throw new IllegalStateException("Different root scope count.");
            }

            BiMap.Transient<S> req = BiMap.Transient.of();
            for(int i = 0; i < rootScopes.size(); i++) {
                req.put(rootScopes.get(i), previousRootScopes.get(i));
            }

            if(!differ.matchScopes(req.freeze())) {
                logger.error("Unit {} adds subunit {} with initial state but with different root scope count.");
                throw new IllegalStateException("Could not match.");
            }

        });

        final IFuture<IUnitResult<S, L, D, Q>> result = this.<Q>doAddSubUnit(id, (subself, subcontext) -> {
            return new TypeCheckerUnit<>(subself, self, subcontext, unitChecker, edgeLabels, initialState, scopeImpl,
                    differOps);
        }, rootScopes)._2();

        return ifActive(result);
    }

    @Override public IFuture<IUnitResult<S, L, D, Unit>> add(String id, IScopeGraphLibrary<S, L, D> library,
            List<S> rootScopes) {
        assertActive();

        final IFuture<IUnitResult<S, L, D, Unit>> result = this.<Unit>doAddSubUnit(id, (subself, subcontext) -> {
            return new ScopeGraphLibraryUnit<>(subself, self, subcontext, edgeLabels, library, differOps);
        }, rootScopes)._2();

        return ifActive(result);
    }

    @Override public void initScope(S root, Iterable<L> labels, boolean sharing) {
        assertActive();

        final List<EdgeOrData<L>> edges = stream(labels).map(EdgeOrData::edge).collect(Collectors.toList());

        doInitShare(self, root, edges, sharing);
    }

    @Override public S freshScope(String baseName, Iterable<L> edgeLabels, boolean data, boolean sharing) {
        assertActive();

        final S scope = doFreshScope(baseName, edgeLabels, data, sharing);

        return scope;
    }

    @Override public void shareLocal(S scope) {
        assertActive();

        doAddShare(self, scope);
    }

    @Override public void setDatum(S scope, D datum) {
        assertActive();

        doSetDatum(scope, datum);
    }

    @Override public void addEdge(S source, L label, S target) {
        assertActive();

        doAddEdge(self, source, label, target);
    }

    @Override public void closeEdge(S source, L label) {
        assertActive();

        doCloseLabel(self, source, EdgeOrData.edge(label));
    }

    @Override public void closeScope(S scope) {
        assertActive();

        doCloseScope(self, scope);
    }

    @Override public IFuture<Set<IResolutionPath<S, L, D>>> query(S scope, LabelWf<L> labelWF, LabelOrder<L> labelOrder,
            DataWf<S, L, D> dataWF, DataLeq<S, L, D> dataEquiv, @Nullable DataWf<S, L, D> dataWfInternal,
            @Nullable DataLeq<S, L, D> dataEquivInternal) {
        // TODO: After doing a query, runIncremental may not be used anymore
        assertActive();

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
        return ifActive(ret).thenApply(CapsuleUtil::toSet);
    }

    @Override public <Q> IFuture<R> runIncremental(Function1<Boolean, IFuture<Q>> runLocalTypeChecker,
            Function1<R, Q> extractLocal, Function2<Q, Throwable, IFuture<R>> combine) {
        state = UnitState.UNKNOWN;
        if(initialState.changed()) {
            logger.debug("Unit changed or no previous result was available.");
            doRestart();
            return runLocalTypeChecker.apply(false).compose(combine::apply);
        }

        doConfirmQueries();

        // Invariant: added units are marked as changed.
        // Therefore, if unit is not changed, a previous result must be given.
        IUnitResult<S, L, D, R> previousResult = initialState.previousResult().get();

        return confirmationResult.thenCompose(validated -> {
            if(validated) {
                Q previousLocalResult = extractLocal.apply(previousResult.analysis());
                return combine.apply(previousLocalResult, null);
            } else {
                return runLocalTypeChecker.apply(true).compose(combine::apply);
            }
        });
    }

    ///////////////////////////////////////////////////////////////////////////
    // Implementation -- confirmation, restart and reuse
    ///////////////////////////////////////////////////////////////////////////

    private void doConfirmQueries() {
        assertInState(UnitState.UNKNOWN);
        resume();

        final List<IFuture<Boolean>> futures = new ArrayList<>();
        initialState.previousResult().get().queries().forEach(rq -> {
            final ICompletableFuture<Boolean> future = new CompletableFuture<>();
            futures.add(future);
            future.thenAccept(res -> {
                // Immediately restart when a query is invalidated
                if(!res) {
                    doRestart();
                }
            });
            final IActorRef<? extends IUnit<S, L, D, ?>> owner = context.owner(rq.scope());
            self.async(owner)._match(rq.scope()).whenComplete((m, ex) -> {
                if(ex != null) {
                    future.completeExceptionally(ex);
                } else if(!m.isPresent()) {
                    // No match, so result is the same iff previous environment was empty.
                    future.complete(rq.result().isEmpty());
                } else {
                    self.async(owner)
                            ._confirm(new ScopePath<>(m.get()), rq.labelWf(), rq.dataWf(), rq.labelOrder(), rq.dataLeq())
                            .thenAccept(env -> {
                                // Query is valid iff environments are equal
                                // TODO: compare environments with scope patches.
                                future.complete(env.equals(rq.result()));
                            });
                }
            });
        });

        Futures.noneMatch(futures, p -> p.thenApply(v -> !v)).whenComplete((r, ex) -> {
            if(ex != null) {
                failures.add(ex);
                doRestart();
            } else {
                // TODO: collect patches
                doRelease(CapsuleUtil.immutableMap());
            }
        });
    }

    private void doRelease(Map.Immutable<S, S> patches) {
        if(state == UnitState.UNKNOWN) {
            state = UnitState.RELEASED;
            final IUnitResult<S, L, D, R> previousResult = initialState.previousResult().get();

            final IScopeGraph.Transient<S, L, D> newScopeGraph = ScopeGraph.Transient.of();
            previousResult.scopeGraph().getEdges().forEach((entry, targets) -> {
                final S oldSource = entry.getKey();
                final S newSource = patches.getOrDefault(oldSource, oldSource);
                targets.forEach(targetScope -> {
                    newScopeGraph.addEdge(newSource, entry.getValue(), patches.getOrDefault(targetScope, targetScope));
                });
            });
            previousResult.scopeGraph().getData().forEach((oldScope, datum) -> {
                final S newScope = patches.getOrDefault(oldScope, oldScope);
                newScopeGraph.setDatum(newScope, scopeImpl.substituteScopes(datum, patches));
            });
            scopeGraph.set(newScopeGraph.freeze());

            // initialize all scopes that are pending, and close all open labels.
            // these should be set by the now reused scopegraph.
            waitForsByActor.get(self).forEach(wf -> {
                wf.visit(IWaitFor.cases(
                    initScope -> doInitShare(self, initScope.scope(), CapsuleUtil.immutableSet(), false),
                    closeScope -> {},
                    closeLabel -> doCloseLabel(self, closeLabel.scope(), closeLabel.label()),
                    query -> {},
                    complete -> {},
                    datum -> {},
                    match -> {},
                    result -> {},
                    typeCheckerState -> {},
                    differResult -> {},
                    activate -> {}
                ));
            });

            // TODO: patch result
            // TODO: is this way of setting the result correct?
            analysis.set(initialState.previousResult().get().analysis());
            confirmationResult.complete(true);

            this.recordedQueries.addAll(previousResult.queries());

            // Cancel all futures waiting for activation
            whenActive.completeExceptionally(new Release());

            tryFinish();
        }
    }

    private void doRestart() {
        if(state == UnitState.INIT_TC || state == UnitState.UNKNOWN) {
            state = UnitState.ACTIVE;
            whenActive.complete(Unit.unit);
            confirmationResult.complete(false);
            tryFinish(); // FIXME needed?
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Deadlock handling
    ///////////////////////////////////////////////////////////////////////////

    @Override public void handleDeadlock(java.util.Set<IActorRef<? extends IUnit<S, L, D, ?>>> nodes) {
        if(state.equals(UnitState.UNKNOWN)) {
            Futures.noneMatch(nodes, node -> {
                return self.async(node)._requireRestart();
            }).whenComplete((w, ex) -> {
                if(ex == null && w) {
                    nodes.forEach(node -> self.async(node)._release());
                } else {
                    if(ex != null) {
                        failures.add(ex);
                    }
                    nodes.forEach(node -> self.async(node)._restart());
                }
                resume();
            });
            resume();
        } else {
            super.handleDeadlock(nodes);
            resume();
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

    private void assertActive() {
        if(!state.active()) {
            logger.error("Expected active state, was {}", state);
            throw new IllegalStateException("Expected active state, but was " + state);
        }
    }


    private <Q> IFuture<Q> ifActive(IFuture<Q> result) {
        return result.compose((r, ex) -> {
            if(state.active()) {
                return CompletableFuture.completed(r, ex);
            } else {
                return CompletableFuture.noFuture();
            }
        });
    }

    ///////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    // toString
    ///////////////////////////////////////////////////////////////////////////

    @Override public String toString() {
        return "TypeCheckerUnit{" + self.id() + "}";
    }

}