package mb.p_raffrayi.impl;

import static com.google.common.collect.Streams.stream;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.functions.Function2;
import org.metaborg.util.future.AggregateFuture;
import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.ICompletableFuture;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.tuple.Tuple2;
import org.metaborg.util.unit.Unit;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import io.usethesource.capsule.Set;
import io.usethesource.capsule.util.stream.CapsuleCollectors;
import mb.p_raffrayi.IIncrementalTypeCheckerContext;
import mb.p_raffrayi.IScopeGraphLibrary;
import mb.p_raffrayi.IScopeImpl;
import mb.p_raffrayi.ITypeChecker;
import mb.p_raffrayi.IUnitResult;
import mb.p_raffrayi.actors.IActor;
import mb.p_raffrayi.actors.IActorRef;
import mb.p_raffrayi.impl.diff.IScopeGraphDifferOps;
import mb.p_raffrayi.impl.tokens.Query;
import mb.p_raffrayi.nameresolution.DataLeq;
import mb.p_raffrayi.nameresolution.DataWf;
import mb.scopegraph.ecoop21.LabelOrder;
import mb.scopegraph.ecoop21.LabelWf;
import mb.scopegraph.oopsla20.IScopeGraph;
import mb.scopegraph.oopsla20.path.IResolutionPath;
import mb.scopegraph.oopsla20.reference.EdgeOrData;
import mb.scopegraph.oopsla20.reference.Env;
import mb.scopegraph.oopsla20.reference.ScopeGraph;
import mb.scopegraph.oopsla20.terms.newPath.ScopePath;

class TypeCheckerUnit<S, L, D, R> extends AbstractUnit<S, L, D, R> implements IIncrementalTypeCheckerContext<S, L, D, R> {


    private static final ILogger logger = LoggerUtils.logger(TypeCheckerUnit.class);

    private final ITypeChecker<S, L, D, R> typeChecker;

    private volatile UnitState state;

    private final IScopeImpl<S, D> scopeImpl; // TODO: remove field, and move methods to IUnitContext?
    private final IScopeGraphDifferOps<S, D> differOps;

    TypeCheckerUnit(IActor<? extends IUnit<S, L, D, R>> self, @Nullable IActorRef<? extends IUnit<S, L, D, ?>> parent,
            IUnitContext<S, L, D> context, ITypeChecker<S, L, D, R> unitChecker, Iterable<L> edgeLabels,
            IInitialState<S, L, D, R> initialState, IScopeImpl<S, D> scopeImpl, IScopeGraphDifferOps<S, D> differOps) {
        super(self, parent, context, edgeLabels, initialState, differOps);
        this.typeChecker = unitChecker;
        this.differOps = differOps;
        this.scopeImpl = scopeImpl;
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
            List<S> rootScopes, IInitialState<S, L, D, Q> initialState) {
        assertInState(UnitState.ACTIVE);

        final IFuture<IUnitResult<S, L, D, Q>> result = this.<Q>doAddSubUnit(id, (subself, subcontext) -> {
            return new TypeCheckerUnit<>(subself, self, subcontext, unitChecker, edgeLabels, initialState, scopeImpl, differOps);
        }, rootScopes)._2();

        return ifActive(result);
    }

    @Override public IFuture<IUnitResult<S, L, D, Unit>> add(String id, IScopeGraphLibrary<S, L, D> library,
            List<S> rootScopes) {
        assertInState(UnitState.ACTIVE);

        final IFuture<IUnitResult<S, L, D, Unit>> result = this.<Unit>doAddSubUnit(id, (subself, subcontext) -> {
            return new ScopeGraphLibraryUnit<>(subself, self, subcontext, edgeLabels, library, differOps);
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
        return ifActive(ret).thenApply(CapsuleUtil::toSet);
    }

    @Override public <Q> IFuture<R> runIncremental(Function1<Boolean, IFuture<Q>> runLocalTypeChecker,
            Function1<R, Q> extractLocal, Function2<Q, Throwable, IFuture<R>> combine) {
        if(initialState.changed()) {
            logger.debug("Unit changed or no previous result was available.");
            return runLocalTypeChecker.apply(false).compose(combine::apply);
        }

        // Invariant: added units are marked as changed.
        // Therefore, if unit is not changed, a previous result must be given.
        IUnitResult<S, L, D, R> previousResult = initialState.previousResult().get();

        final ICompletableFuture<Boolean> confirmationResult = new CompletableFuture<>();

        return confirmationResult.thenCompose(validated -> {
            if(validated) {
                Q previousLocalResult = extractLocal.apply(previousResult.analysis());
                return combine.apply(previousLocalResult, null);
            } else {
                return runLocalTypeChecker.apply(true).compose(combine::apply);
            }
        });
    }

    @SuppressWarnings("unused") private void confirmQueries(List<S> rootScopes) {
        // assertInState(UnitState.INIT, /* or */ UnitState.UNKNOWN);
        resume();

        // state = UnitState.UNKNOWN;
        ICompletableFuture<Tuple2<Boolean, io.usethesource.capsule.Map.Immutable<S, S>>> confirmationsComplete = new CompletableFuture<>();
        // TODO deadlock handling
        confirmationsComplete.whenComplete((v, ex) -> {
            if (ex != null) {
                logger.error("{} confirmation failed: {}", this, ex);
                failures.add(ex);
                tryFinish();
            } else if(!v._1()) {
                logger.info("{} confirmations denied, restarting", this);
                assertInState(UnitState.UNKNOWN);
                // startTypeChecker(rootScopes); // TODO: check proper starting
            } else {
                logger.info("{} confirmations confirmed", this);
                release(v._2());
            }
        });

        // @formatter:off
        new AggregateFuture<Tuple2<Boolean, io.usethesource.capsule.Map.Immutable<S, S>>>(initialState.previousResult()
            .orElseThrow(() -> new IllegalStateException("Cannot confirm queries when no previous result is provided"))
            .queries()
            .stream()
            .map(recordedQuery -> confirmation.confirm(recordedQuery)
                .whenComplete((v, ex) -> {
                    // When confirmation denied, eagerly restart type-checker
                    if(ex == null && (v == null || !v._1())) {
                        confirmationsComplete.complete(v, ex);
                    }
                }))
            .collect(Collectors.toSet()))
            .whenComplete((v, ex) -> {
                if(ex != null) {
                    confirmationsComplete.complete(Tuple2.of(false, CapsuleUtil.immutableMap()), ex);
                } else if(v.stream().allMatch(x -> x != null && x._1())) {
                    // All queries confirmed, aggregate patches and complete
                    io.usethesource.capsule.Map.Transient<S, S> patches = CapsuleUtil.transientMap();
                    // TODO: optimize for duplicates?
                    v.forEach(result -> patches.__putAll(result._2()));
                    confirmationsComplete.complete(Tuple2.of(true, patches.freeze()), ex);
                }
                // in the else case, one of the futures has restarted the type checker, so we don't handle that case here.
            });
        // @formatter:on
    }

    private void release(io.usethesource.capsule.Map.Immutable<S, S> patches) {
        IUnitResult<S, L, D, R> previousResult = initialState.previousResult().get();

        IScopeGraph.Transient<S, L, D> newScopeGraph = ScopeGraph.Transient.of();
        previousResult.scopeGraph().getEdges().forEach((entry, targets) -> {
            S oldSource = entry.getKey();
            S newSource = patches.getOrDefault(oldSource, oldSource);
            targets.forEach(targetScope -> {
                newScopeGraph.addEdge(newSource, entry.getValue(), patches.getOrDefault(targetScope, targetScope));
            });
        });
        previousResult.scopeGraph().getData().forEach((oldScope, datum) -> {
            S newScope = patches.getOrDefault(oldScope, oldScope);
            newScopeGraph.setDatum(newScope, scopeImpl.substituteScopes(datum, patches));
        });

        scopeGraph.set(newScopeGraph.freeze());
        analysis.set(initialState.previousResult().get().analysis());
        tryFinish();
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