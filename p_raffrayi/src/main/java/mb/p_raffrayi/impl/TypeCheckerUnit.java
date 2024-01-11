package mb.p_raffrayi.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.annotation.Nullable;

import org.metaborg.util.Ref;
import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.collection.MultiSet;
import org.metaborg.util.collection.MultiSetMap;
import org.metaborg.util.collection.SetMultimap;
import org.metaborg.util.collection.Sets;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.functions.Function2;
import org.metaborg.util.future.AggregateFuture;
import org.metaborg.util.future.AggregateFuture.SC;
import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.ICompletableFuture;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.tuple.Tuple2;
import org.metaborg.util.unit.Unit;

import io.usethesource.capsule.Set;
import mb.p_raffrayi.IIncrementalTypeCheckerContext;
import mb.p_raffrayi.IRecordedQuery;
import mb.p_raffrayi.ITypeChecker;
import mb.p_raffrayi.ITypeChecker.IOutput;
import mb.p_raffrayi.ITypeChecker.IState;
import mb.p_raffrayi.IUnitResult;
import mb.p_raffrayi.IUnitResult.TransitionTrace;
import mb.p_raffrayi.actors.IActor;
import mb.p_raffrayi.actors.IActorRef;
import mb.p_raffrayi.impl.confirm.ConfirmResult;
import mb.p_raffrayi.impl.confirm.IConfirmation;
import mb.p_raffrayi.impl.confirm.IConfirmationContext;
import mb.p_raffrayi.impl.confirm.IConfirmationFactory;
import mb.p_raffrayi.impl.confirm.OptimisticConfirmation;
import mb.p_raffrayi.impl.diff.AddingDiffer;
import mb.p_raffrayi.impl.diff.IDifferContext;
import mb.p_raffrayi.impl.diff.IDifferDataOps;
import mb.p_raffrayi.impl.diff.IDifferOps;
import mb.p_raffrayi.impl.diff.IScopeGraphDiffer;
import mb.p_raffrayi.impl.diff.MatchingDiffer;
import mb.p_raffrayi.impl.diff.ScopeDiff;
import mb.p_raffrayi.impl.diff.ScopeGraphDiffer;
import mb.p_raffrayi.impl.diff.StaticDifferContext;
import mb.p_raffrayi.impl.envdiff.EnvDiffer;
import mb.p_raffrayi.impl.envdiff.IEnvDiff;
import mb.p_raffrayi.impl.envdiff.IEnvDiffer;
import mb.p_raffrayi.impl.envdiff.IEnvDifferContext;
import mb.p_raffrayi.impl.envdiff.IndexedEnvDiffer;
import mb.p_raffrayi.impl.tokens.CloseLabel;
import mb.p_raffrayi.impl.tokens.CloseScope;
import mb.p_raffrayi.impl.tokens.Confirm;
import mb.p_raffrayi.impl.tokens.DifferState;
import mb.p_raffrayi.impl.tokens.EnvDifferState;
import mb.p_raffrayi.impl.tokens.IWaitFor;
import mb.p_raffrayi.impl.tokens.InitScope;
import mb.p_raffrayi.impl.tokens.Query;
import mb.p_raffrayi.nameresolution.DataLeq;
import mb.p_raffrayi.nameresolution.DataWf;
import mb.p_raffrayi.nameresolution.IQuery;
import mb.p_raffrayi.nameresolution.NameResolutionQuery;
import mb.p_raffrayi.nameresolution.StateMachineQuery;
import mb.scopegraph.ecoop21.LabelOrder;
import mb.scopegraph.ecoop21.LabelWf;
import mb.scopegraph.library.IScopeGraphLibrary;
import mb.scopegraph.oopsla20.IScopeGraph;
import org.metaborg.util.collection.BiMap;
import mb.scopegraph.oopsla20.path.IResolutionPath;
import mb.scopegraph.oopsla20.reference.EdgeOrData;
import mb.scopegraph.oopsla20.reference.ScopeGraph;
import mb.scopegraph.oopsla20.terms.newPath.ScopePath;
import mb.scopegraph.patching.IPatchCollection;
import mb.scopegraph.patching.PatchCollection;
import mb.scopegraph.patching.Patcher;
import mb.scopegraph.resolution.StateMachine;

class TypeCheckerUnit<S, L, D, R extends IOutput<S, L, D>, T extends IState<S, L, D>>
        extends AbstractUnit<S, L, D, Result<S, L, D, R, T>> implements IIncrementalTypeCheckerContext<S, L, D, R, T> {


    private static final ILogger logger = LoggerUtils.logger(TypeCheckerUnit.class);

    private final ITypeChecker<S, L, D, R, T> typeChecker;
    private final boolean changed;
    private final @Nullable IUnitResult<S, L, D, Result<S, L, D, R, T>> previousResult;

    private volatile UnitState state;

    private final IPatchCollection.Transient<S> matchedBySharing = PatchCollection.Transient.of();

    private final Ref<IScopeGraph.Immutable<S, L, D>> localScopeGraph = new Ref<>(ScopeGraph.Immutable.of());

    private final Set.Transient<String> addedUnitIds = CapsuleUtil.transientSet();

    private final Ref<StateCapture<S, L, D, T>> localCapture = new Ref<>();
    private final ICompletableFuture<Unit> whenActive = new CompletableFuture<>(); // activated when transitioning from unknown to active/released.
    private final ICompletableFuture<Unit> whenContextActivated = new CompletableFuture<>(); // activated when transitioning from local/unknown to active.

    private IEnvDiffer<S, L, D> envDiffer;

    private final IPatchCollection.Transient<S> externalMatches = PatchCollection.Transient.of();

    private final IConfirmationFactory<S, L, D> confirmation;
    private final ICompletableFuture<Optional<IPatchCollection.Immutable<S>>> confirmationResult =
            new CompletableFuture<>();

    // Intermediate confirmation result aggregations
    private final IPatchCollection.Transient<S> resultPatches = PatchCollection.Transient.of();
    private final IPatchCollection.Transient<S> globalPatches = PatchCollection.Transient.of();
    private final Set.Transient<IRecordedQuery<S, L, D>> addedQueries = CapsuleUtil.transientSet();
    private final Set.Transient<IRecordedQuery<S, L, D>> removedQueries = CapsuleUtil.transientSet();

    TypeCheckerUnit(IActor<? extends IUnit<S, L, D, Result<S, L, D, R, T>>> self,
            @Nullable IActorRef<? extends IUnit<S, L, D, ?>> parent, IUnitContext<S, L, D> context,
            ITypeChecker<S, L, D, R, T> unitChecker, Iterable<L> edgeLabels, boolean inputChanged,
            IUnitResult<S, L, D, Result<S, L, D, R, T>> previousResult) {
        super(self, parent, context, edgeLabels);
        this.typeChecker = unitChecker;
        this.changed = inputChanged;
        this.previousResult = previousResult;
        this.state = UnitState.INIT_UNIT;
        this.confirmation = OptimisticConfirmation.factory();
    }

    TypeCheckerUnit(IActor<? extends IUnit<S, L, D, Result<S, L, D, R, T>>> self,
            @Nullable IActorRef<? extends IUnit<S, L, D, ?>> parent, IUnitContext<S, L, D> context,
            ITypeChecker<S, L, D, R, T> unitChecker, Iterable<L> edgeLabels) {
        this(self, parent, context, unitChecker, edgeLabels, true, null);
    }

    private final MultiSetMap.Transient<D, ICompletableFuture<D>> pendingExternalDatums = MultiSetMap.Transient.of();

    @Override protected IFuture<D> getExternalDatum(D datum) {
        if(!changed && previousResult != null && previousResult.result().localState() != null) {
            // when previous result is present, reliable (i.e., !changed), and has local state
            // try to find datum from previous state
            final Optional<D> datumOpt =
                    previousResult.result().localState().typeCheckerState().tryGetExternalDatum(datum);
            if(datumOpt.isPresent()) {
                return CompletableFuture.completedFuture(datumOpt.get());
            }
        }
        return whenActive.compose((u, ex) -> {
            if(this.state.equals(UnitState.RELEASED) || this.state.equals(UnitState.DONE)
                    && this.stateTransitionTrace.equals(TransitionTrace.RELEASED)) {
                final D result = previousResult.result().analysis().getExternalRepresentation(datum);
                return CompletableFuture
                        .completedFuture(context.substituteScopes(result, resultPatches.patches().invert()));
            }
            final IFuture<D> future = typeChecker.getExternalDatum(datum);
            if(future.isDone()) {
                return future;
            }
            final ICompletableFuture<D> result = new CompletableFuture<>();
            pendingExternalDatums.put(datum, result);
            future.whenComplete(result::complete);
            return result.whenComplete((__, ex2) -> {
                pendingExternalDatums.remove(datum, result);
            });
        });
    }

    @Override protected D getPreviousDatum(D datum) {
        assertPreviousResultProvided();
        return previousResult.result().analysis().getExternalRepresentation(datum);
    }

    ///////////////////////////////////////////////////////////////////////////
    // IBroker2UnitProtocol interface, called by IBroker implementations
    ///////////////////////////////////////////////////////////////////////////

    @Override public IFuture<IUnitResult<S, L, D, Result<S, L, D, R, T>>> _start(List<S> rootScopes) {
        assertInState(UnitState.INIT_UNIT);
        resume();

        doStart(rootScopes);
        if(isIncrementalEnabled() && previousResult != null) {
            if(previousResult.rootScopes().size() != rootScopes.size()) {
                throw new IllegalStateException("Root scope counts do not match.");
            }
            Iterables2.zip(rootScopes, previousResult.rootScopes(), Tuple2::of).forEach(match -> {
                matchedBySharing.put(match._1(), match._2());
                externalMatches.put(match._1(), match._2());
            });
        }

        final IFuture<Result<S, L, D, R, T>> result;
        try {
            state = UnitState.INIT_TC;
            result = this.typeChecker.run(this, rootScopes).whenComplete((r, ex) -> {
                if(state == UnitState.INIT_TC) {
                    stateTransitionTrace = TransitionTrace.INITIALLY_STARTED;
                }
                if(inLocalPhase()) {
                    doCapture();
                }
                state = UnitState.DONE;
                resume();
                tryFinish();
            }).thenApply(r -> {
                return Result.<S, L, D, R, T>of(r, localCapture.get(), localScopeGraph.get(),
                        matchedBySharing.patchRange());
            });
        } catch(Exception e) {
            logger.error("Exception starting type-checker {}.", e);
            return doFinish(CompletableFuture.completedExceptionally(e));
        }

        // Start phantom units for all units that have not yet been restarted
        if(isIncrementalEnabled() && previousResult != null) {
            for(String removedId : Sets.difference(previousResult.subUnitResults().keySet(), addedUnitIds)) {
                @SuppressWarnings("unchecked") final IUnitResult<S, L, D, ? extends IOutput<S, L, D>> subResult =
                        (IUnitResult<S, L, D, ? extends IOutput<S, L, D>>) previousResult.subUnitResults()
                                .get(removedId);
                this.<Unit>doAddSubUnit(removedId,
                        (subself, subcontext) -> new PhantomUnit<>(subself, self, subcontext, edgeLabels, subResult),
                        new ArrayList<>(), true);
            }
        }

        if(state == UnitState.INIT_TC) {
            // runIncremental not called, so start eagerly
            doRestart(false);
        } else if(state == UnitState.DONE) {
            // Completed synchronously
            whenActive.complete(Unit.unit);
            confirmationResult.complete(Optional.of(PatchCollection.Immutable.of()));
        }

        return doFinish(result);
    }

    ///////////////////////////////////////////////////////////////////////////
    // IUnit2UnitProtocol interface, called by IUnit implementations
    ///////////////////////////////////////////////////////////////////////////

    @Override public IFuture<ConfirmResult<S, L, D>> _confirm(S scope, LabelWf<L> labelWF, DataWf<S, L, D> dataWF,
            boolean prevEnvEmpty) {
        assertConfirmationEnabled();
        stats.incomingConfirmations++;
        final IActorRef<? extends IUnit<S, L, D, ?>> sender = self.sender(TYPE);
        final ICompletableFuture<ConfirmResult<S, L, D>> result = new CompletableFuture<>();
        whenActive.whenComplete((__, ex) -> {
            if(ex != null && ex != Release.instance) {
                result.completeExceptionally(ex);
            } else {
                // Cannot immediately confirm on `Release.instance` exception
                // because subunits might change edges in shared scopes.
                confirmation.getConfirmation(new ConfirmationContext(sender))
                        .confirm(scope, labelWF, dataWF, prevEnvEmpty).whenComplete(result::complete);
            }
        });
        return result;
    }

    @Override public IFuture<IQueryAnswer<S, L, D>> _queryPrevious(ScopePath<S, L> path, IQuery<S, L, D> query,
            DataWf<S, L, D> dataWF, DataLeq<S, L, D> dataEquiv) {
        assertPreviousResultProvided();
        return doQueryPrevious(self.sender(TYPE), previousResult.scopeGraph(), path, query, dataWF, dataEquiv);
    }

    @Override public IFuture<Optional<S>> _match(S previousScope) {
        assertOwnScope(previousScope);
        if(matchedBySharing.identityPatches().contains(previousScope)) {
            return CompletableFuture.completedFuture(Optional.of(previousScope));
        }
        if(matchedBySharing.patchDomain().contains(previousScope)) {
            return CompletableFuture.completedFuture(Optional.of(matchedBySharing.patch(previousScope)));
        }
        if(!changed && previousResult.result().localState() != null
                && previousResult.result().localState().scopes().contains(previousScope)) {
            return CompletableFuture.completedFuture(Optional.of(previousScope));
        }
        return super._match(previousScope);
    }

    @Override public IFuture<StateSummary<S, L, D>> _state() {
        // resume();
        if(state.equals(UnitState.ACTIVE)
                || (state == UnitState.DONE && stateTransitionTrace != TransitionTrace.RELEASED)) {
            return CompletableFuture.completedFuture(StateSummary.restart(process, dependentSet()));
        }

        if(state.equals(UnitState.RELEASED)
                || (state == UnitState.DONE && stateTransitionTrace == TransitionTrace.RELEASED)) {
            return CompletableFuture.completedFuture(StateSummary.released(process, dependentSet()));
        }

        return CompletableFuture.completedFuture(StateSummary.release(process, dependentSet()));
    }

    @Override public void _restart() {
        assertIncrementalEnabled();
        if(doRestart(true)) {
            stateTransitionTrace = TransitionTrace.RESTARTED;
        }
    }

    @Override public void _release() {
        if(state == UnitState.UNKNOWN) {
            doRelease(CapsuleUtil.toSet(this.addedQueries), CapsuleUtil.toSet(this.removedQueries),
                    PatchCollection.Immutable.<S>of().putAll(this.resultPatches),
                    PatchCollection.Immutable.<S>of().putAll(globalPatches));
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // ITypeCheckerContext interface, called by ITypeChecker implementations
    ///////////////////////////////////////////////////////////////////////////

    // NB. Invoke methods via `local` so that we have the same scheduling & ordering
    // guarantees as for remote calls.

    @Override public String id() {
        return self.id();
    }

    @SuppressWarnings("unchecked") @Override public <Q extends IOutput<S, L, D>, U extends IState<S, L, D>>
            IFuture<IUnitResult<S, L, D, Result<S, L, D, Q, U>>>
            add(String id, ITypeChecker<S, L, D, Q, U> unitChecker, List<S> rootScopes, boolean changed) {
        assertActive();

        // No previous result for subunit
        if(this.previousResult == null || !this.previousResult.subUnitResults().containsKey(id)) {
            this.addedUnitIds.__insert(id);

            return ifActive(whenContextActive(this.<Result<S, L, D, Q, U>>doAddSubUnit(id, (subself, subcontext) -> {
                return new TypeCheckerUnit<>(subself, self, subcontext, unitChecker, edgeLabels);
            }, rootScopes, false)._2()));
        }

        final IUnitResult<S, L, D, Result<S, L, D, Q, U>> subUnitPreviousResult;
        if(isIncrementalEnabled()) {

            subUnitPreviousResult =
                    (IUnitResult<S, L, D, Result<S, L, D, Q, U>>) this.previousResult.subUnitResults().get(id);


            // When a scope is shared, the shares must be consistent.
            // Also, it is not necessary that shared scopes are reachable from the root scopes
            // (A unit started by the Broker does not even have root scopes)
            // Therefore we enforce here that the current root scopes and the previous ones match.
            List<S> previousRootScopes = subUnitPreviousResult.rootScopes();

            int pSize = previousRootScopes.size();
            int cSize = rootScopes.size();
            if(cSize != pSize) {
                logger.error("Unit {} adds subunit {} with initial state but with different root scope count.");
                throw new IllegalStateException("Different root scope count.");
            }

            final BiMap.Transient<S> req = BiMap.Transient.of();
            for(int i = 0; i < rootScopes.size(); i++) {
                final S cRoot = rootScopes.get(i);
                final S pRoot = previousRootScopes.get(i);
                req.put(cRoot, pRoot);
                if(isOwner(cRoot)) {
                    matchedBySharing.put(cRoot, pRoot);
                }
            }

            if(isDifferEnabled()) {
                whenDifferActivated.thenAccept(__ -> {
                    if(!differ.matchScopes(req.freeze())) {
                        logger.error("Unit {} adds subunit {} with initial state but with different root scope count.");
                        throw new IllegalStateException("Could not match.");
                    }
                });
            }

            this.addedUnitIds.__insert(id);
        } else {
            subUnitPreviousResult = null;
        }
        final IFuture<IUnitResult<S, L, D, Result<S, L, D, Q, U>>> result =
                this.<Result<S, L, D, Q, U>>doAddSubUnit(id, (subself, subcontext) -> {
                    return new TypeCheckerUnit<S, L, D, Q, U>(subself, self, subcontext, unitChecker, edgeLabels,
                            changed, subUnitPreviousResult);
                }, rootScopes, false)._2();

        return ifActive(whenContextActive(result));
    }

    @Override public IFuture<IUnitResult<S, L, D, Unit>> add(String id, IScopeGraphLibrary<S, L, D> library,
            List<S> rootScopes) {
        assertActive();

        final IFuture<IUnitResult<S, L, D, Unit>> result = this.<Unit>doAddSubUnit(id, (subself, subcontext) -> {
            IUnitResult<S, L, D, ?> previousResult =
                    this.previousResult != null ? this.previousResult.subUnitResults().get(id) : null;

            return new ScopeGraphLibraryUnit<>(subself, self, subcontext, edgeLabels, library, previousResult);
        }, rootScopes, false)._2();

        this.addedUnitIds.__insert(id);
        return ifActive(whenContextActive(result));
    }

    @Override public void initScope(S root, Collection<L> labels, boolean sharing) {
        assertActive();

        final List<EdgeOrData<L>> edges = labels.stream().map(EdgeOrData::edge).collect(Collectors.toList());

        doInitShare(self, root, edges, sharing);
    }

    @Override public S freshScope(String baseName, Iterable<L> edgeLabels, boolean data, boolean sharing) {
        assertActive();
        doImplicitActivate();

        final S scope = doFreshScope(baseName, edgeLabels, data, sharing);

        return scope;
    }

    @Override public S stableFreshScope(String name, Iterable<L> edgeLabels, boolean data) {
        assertActive();

        final S scope = doStableFreshScope(name, edgeLabels, data);

        return scope;
    }

    @Override public void shareLocal(S scope) {
        assertActive();

        doAddShare(self, scope);
    }

    @Override public void setDatum(S scope, D datum) {
        assertActive();

        doSetDatum(scope, datum);
        localScopeGraph.set(localScopeGraph.get().setDatum(scope, datum));
    }

    @Override public void addEdge(S source, L label, S target) {
        assertActive();
        doImplicitActivate();

        doAddEdge(self, source, label, target);
        localScopeGraph.set(localScopeGraph.get().addEdge(source, label, target));
    }

    @Override public void closeEdge(S source, L label) {
        assertActive();

        doCloseLabel(self, source, EdgeOrData.edge(label));
    }

    @Override public void closeScope(S scope) {
        assertActive();

        doCloseScope(self, scope);
    }

    @Override public IFuture<? extends java.util.Set<IResolutionPath<S, L, D>>> query(S scope, IQuery<S, L, D> query,
            DataWf<S, L, D> dataWF, DataLeq<S, L, D> dataEquiv, DataWf<S, L, D> dataWfInternal,
            DataLeq<S, L, D> dataEquivInternal) {
        assertActive();
        doImplicitActivate();

        final ScopePath<S, L> path = new ScopePath<>(scope);
        final IFuture<IQueryAnswer<S, L, D>> result =
                doQuery(self, self, true, path, query, dataWF, dataEquiv, dataWfInternal, dataEquivInternal);
        final IFuture<IQueryAnswer<S, L, D>> ret;
        if(result.isDone()) {
            ret = result;
        } else {
            final Query<S, L, D> wf = Query.of(self, path, query, dataWF, dataEquiv, result);
            waitFor(wf, self);
            ret = result.whenComplete((env, ex) -> {
                granted(wf, self);
                resume();
            });
        }
        stats.localQueries += 1;
        return ifActive(ret.thenCompose(ans -> {
            final IFuture<IQueryAnswer<S, L, D>> res = CompletableFuture.completedFuture(ans);
            // TODO: Does the relation q.transitiveQueries().isEmpty() <=> q non-local always hold?
            if(ans.transitiveQueries().isEmpty() && ans.predicateQueries().isEmpty()) {
                return res;
            } else {
                return whenContextActive(res);
            }
        })).thenApply(ans -> {
            return CapsuleUtil.toSet(ans.env());
        });
    }

    @Override public IFuture<? extends java.util.Set<IResolutionPath<S, L, D>>> query(S scope, LabelWf<L> labelWF,
            LabelOrder<L> labelOrder, DataWf<S, L, D> dataWF, DataLeq<S, L, D> dataEquiv,
            @Nullable DataWf<S, L, D> dataWfInternal, @Nullable DataLeq<S, L, D> dataEquivInternal) {
        return query(scope, new NameResolutionQuery<>(labelWF, labelOrder, edgeLabels), dataWF, dataEquiv,
                dataWfInternal, dataEquivInternal);
    }

    @Override public IFuture<? extends java.util.Set<IResolutionPath<S, L, D>>> query(S scope,
            StateMachine<L> stateMachine, DataWf<S, L, D> dataWF, DataLeq<S, L, D> dataEquiv,
            DataWf<S, L, D> dataWfInternal, DataLeq<S, L, D> dataEquivInternal) {
        return query(scope, new StateMachineQuery<>(stateMachine), dataWF, dataEquiv, dataWfInternal,
                dataEquivInternal);
    }

    @Override public <Q> IFuture<R> runIncremental(Function1<Optional<T>, IFuture<Q>> runLocalTypeChecker,
            Function1<R, Q> extractLocal, Function2<Q, IPatchCollection.Immutable<S>, Q> patch,
            Function2<Q, Throwable, IFuture<R>> combine) {
        assertInState(UnitState.INIT_TC);
        state = UnitState.UNKNOWN;
        if(!isIncrementalEnabled() || changed) {
            logger.debug("Unit changed or no previous result was available.");
            stateTransitionTrace = TransitionTrace.INITIALLY_STARTED;
            doRestart(false);
            return runLocalTypeChecker.apply(Optional.empty()).compose(combine::apply);
        }

        // Invariant: added units are marked as changed.
        // Therefore, if unit is not changed, previousResult cannot be null.
        assertPreviousResultProvided();

        if(previousResult.result().localState() != null) {
            doRestore(previousResult.result().localState());
        }

        doConfirmQueries();

        return confirmationResult.thenCompose(patches -> {
            if(patches.isPresent()) {
                final Q previousLocalResult = extractLocal.apply(previousResult.result().analysis());
                return combine.apply(patch.apply(previousLocalResult, patches.get()), null);
            } else {
                final Optional<T> initialState =
                        Optional.ofNullable(previousResult.result().localState()).map(StateCapture::typeCheckerState);
                return runLocalTypeChecker.apply(initialState).compose(combine::apply);
            }
        });
    }

    ///////////////////////////////////////////////////////////////////////////
    // Implementation -- confirmation, restart and reuse
    ///////////////////////////////////////////////////////////////////////////

    private void doConfirmQueries() {
        logger.debug("Confirming queries from previous result.");
        assertInState(UnitState.UNKNOWN);
        assertIncrementalEnabled();
        assertPreviousResultProvided();
        resume();

        if(previousResult.queries().isEmpty()) {
            logger.debug("Releasing - no queries in previous result.");
            doRelease(CapsuleUtil.immutableSet(), CapsuleUtil.immutableSet(), PatchCollection.Immutable.of(),
                    PatchCollection.Immutable.of());
            return;
        }

        final IConfirmation<S, L, D> confirmaton = confirmation.getConfirmation(new ConfirmationContext(self));

        // @formatter:off
        final List<IFuture<SC<Boolean, Boolean>>> futures = previousResult.queries().stream()
                .map(confirmaton::confirm)
                .<IFuture<SC<Boolean, Boolean>>>map(intermediateFuture -> {
                    return intermediateFuture.thenApply(intermediate -> intermediate.match(
                        () -> SC.shortCircuit(false),
                        (addedQueries, removedQueries, resultPatches, globalPatches) -> {
                            // Store intermediate set of patches, to be used on deadlock.
                            this.resultPatches.putAll(resultPatches);
                            this.globalPatches.putAll(globalPatches);
                            this.addedQueries.__insertAll(addedQueries);
                            this.removedQueries.__insertAll(removedQueries);
                            return SC.<Boolean, Boolean>of(true);
                        }
                    ));
                }).collect(Collectors.toList());
        // @formatter:on

        // @formatter:off
        AggregateFuture.ofShortCircuitable(patchCollections -> true, futures).whenComplete((r, ex) -> {
                if(ex == Release.instance) {
                    logger.debug("Confirmation received release.");
                    // Do nothing, unit is already released by deadlock resolution.
                } else if(ex != null) {
                    logger.error("Failure in confirmation.", ex);
                    failures.add(ex);
                } else {
                    if(r) {
                        logger.debug("Queries confirmed - releasing.");
                        this.doRelease(addedQueries.freeze(), removedQueries.freeze(), resultPatches.freeze(), globalPatches.freeze());
                    } else {
                        // No confirmation, hence restart
                        logger.debug("Query confirmation denied - restarting.");
                        if(doRestart(true)) {
                            stateTransitionTrace = TransitionTrace.RESTARTED;
                        }
                    }
                }
            });
        // @formatter:on
    }

    private void doRelease(Set.Immutable<IRecordedQuery<S, L, D>> addedQueries,
            Set.Immutable<IRecordedQuery<S, L, D>> removedQueries, IPatchCollection.Immutable<S> resultPatches,
            IPatchCollection.Immutable<S> globalPatches) {
        assertIncrementalEnabled();
        resultPatches.assertConsistent();
        globalPatches.assertConsistent();

        if(state == UnitState.UNKNOWN) {
            logger.debug("{} releasing.", this);
            logger.trace("Patches: result: {}; global: {}.", resultPatches, globalPatches);
            assertPreviousResultProvided();

            // TODO When a unit has active subunits, the matching differ cannot be used, because these units may add/remove edges.
            // However, a full-blown scope graph differ may impose quite some overhead. We should invent a smarter solution for this.
            // - Perhaps only maintain local scope graphs (i.e. no edge sharing), and relay queries to subunits
            //   - Con: very similar to epsilon edges, which degraded performance a lot, and complicated shadowing
            // - Somehow inform differ of local edges and subunit edges, and perform diff based on those.

            // Copy scopes
            // No need to patch, because they can only be local, and local state is reused.
            this.scopes.__insertAll(previousResult.scopes());

            final IPatchCollection.Immutable<S> localPatches = matchedBySharing.freeze();
            // @formatter:off
            final IScopeGraphDiffer<S, L, D> differ = localPatches.isIdentity() && this.addedUnitIds.isEmpty() ?
                new MatchingDiffer<S, L, D>(differOps(), differContext(typeChecker::internalData), resultPatches.allPatches()) :
                new ScopeGraphDiffer<>(differContext(typeChecker::internalData), new StaticDifferContext<>(previousResult.scopeGraph(),
                        previousResult.scopes(), new DifferDataOps()), differOps(), edgeLabels);
            // @formatter:on

            final Collection<S> openScopes =
                    this.addedUnitIds.isEmpty() ? Collections.emptySet() : previousResult.rootScopes();
            if(isConfirmationEnabled()) {
                this.envDiffer = new IndexedEnvDiffer<>(new EnvDiffer<>(envDifferContext, differOps()));
            }
            initDiffer(differ, previousResult.scopeGraph(), previousResult.scopes(),
                    previousResult.result().sharedScopes(), localPatches, openScopes, MultiSetMap.Immutable.of());

            logger.debug("Rebuilding scope graph.");
            // @formatter:off
            final Patcher<S, L, D> patcher = new Patcher.Builder<S, L, D>()
                .patchSources(localPatches)
                .patchEdgeTargets(resultPatches)
                .patchDatumSources(localPatches)
                .patchDatums(resultPatches, context::substituteScopes)
                .build();

            final IScopeGraph.Immutable<S, L, D> patchedLocalScopeGraph = patcher.<Boolean>apply(
                previousResult.result().scopeGraph(),
                (oldSource, newSource) -> isOwner(newSource),
                (oldSource, newSource, lbl, oldTarget, newTarget, sourceLocal) -> {
                    if(!sourceLocal) {
                        doAddEdge(self, newSource, lbl, newTarget);
                    }
                },
                Patcher.DataPatchCallback.noop()
            );

            scopeGraph.set(scopeGraph.get().addAll(patchedLocalScopeGraph));
            localScopeGraph.set(localScopeGraph.get().addAll(patchedLocalScopeGraph));

            // initialize all scopes that are pending, and close all open labels.
            // these should be set by the now reused scopegraph.
            logger.debug("Close pending tokens.");
            final HashSet<S> initScopes = new HashSet<>();
            final SetMultimap<S, EdgeOrData<L>> closeEdges = new SetMultimap<>();

            // @formatter:off
            final IWaitFor.Cases<S, L, D> cases = IWaitFor.cases(
                initScope -> initScopes.add(initScope.scope()),
                closeScope -> {},
                closeLabel -> closeEdges.put(closeLabel.scope(), closeLabel.label()),
                query -> {},
                pQuery -> {},
                confirm -> {},
                match -> {},
                result -> {},
                typeCheckerState -> {},
                differResult -> {},
                differState -> {},
                envDifferState -> {},
                unitAdd -> {}
            );
            // @formatter:on
            for(IWaitFor<S, L, D> wf : ownTokens()) {
                wf.visit(cases);
            }
            for(S scope : initScopes) {
                doInitShare(self, scope, Collections.emptySet(), false);
            }
            closeEdges.entries().forEach((Map.Entry<S, EdgeOrData<L>> entry) -> {
                doCloseLabel(self, entry.getKey(), entry.getValue());
            });

            pendingExternalDatums.asMap().forEach((d, futures) -> futures.elementSet().forEach(future -> {
                final D datum = previousResult.result().analysis().getExternalRepresentation(d);
                final D result = context.substituteScopes(datum, resultPatches.patches().invert());
                self.complete(future, result, null);
            }));

            final IPatchCollection.Immutable<S> allPatches = resultPatches.putAll(globalPatches);
            java.util.Set<IRecordedQuery<S, L, D>> newRecordedQueries =
                    previousResult.queries().stream().map(q -> q.patch(allPatches)).collect(Collectors.toSet());
            java.util.Set<IRecordedQuery<S, L, D>> patchedRemovedQueries =
                    removedQueries.stream().map(q -> q.patch(allPatches)).collect(Collectors.toSet());
            newRecordedQueries.removeAll(patchedRemovedQueries);
            newRecordedQueries.addAll(addedQueries);
            recordedQueries.addAll(newRecordedQueries);

            stateTransitionTrace = TransitionTrace.RELEASED;

            self.complete(confirmationResult, Optional.of(resultPatches), null);
            // Cancel all futures waiting for activation
            self.complete(whenActive, null, Release.instance);
            state = UnitState.RELEASED;

            resume();
            tryFinish(); // FIXME needed?
            logger.debug("{} released.", this);
        }
    }

    private boolean doRestart(boolean external) {
        if(state == UnitState.INIT_TC || state == UnitState.UNKNOWN) {
            logger.debug("{} restarting.", this);
            state = UnitState.ACTIVE;
            self.complete(confirmationResult, Optional.empty(), null);
            self.complete(whenActive, Unit.unit, null);
            // Not activating context here, as `doRestart` is also used before non-incremental runs.
            if(isDifferEnabled()) {
                final IDifferContext<S, L, D> context = differContext(typeChecker::internalData);
                final IDifferOps<S, L, D> differOps = differOps();
                if(previousResult != null) {
                    final IDifferContext<S, L, D> differContext = new StaticDifferContext<>(previousResult.scopeGraph(),
                            previousResult.scopes(), new DifferDataOps());

                    if(isConfirmationEnabled()) {
                        this.envDiffer = new IndexedEnvDiffer<>(new EnvDiffer<>(envDifferContext, differOps()));
                    }

                    if(external) {
                        final StateCapture<S, L, D, T> capture = previousResult.result().localState();
                        final java.util.Set<S> openScopes = CapsuleUtil.toSet(Sets.union(capture.openScopes().elementSet(),
                                capture.unInitializedScopes().elementSet()));

                        initDiffer(new ScopeGraphDiffer<>(context, differContext, differOps, edgeLabels),
                                capture.scopeGraph(), capture.scopes(), previousResult.result().sharedScopes(),
                                matchedBySharing.freeze(), openScopes, capture.openEdges());
                    } else {
                        initDiffer(new ScopeGraphDiffer<>(context, differContext, differOps, edgeLabels),
                                this.rootScopes, previousResult.rootScopes());
                    }
                } else {
                    initDiffer(new AddingDiffer<>(context, differOps, edgeLabels), Collections.emptyList(),
                            Collections.emptyList());
                }
            }
            pendingExternalDatums.asMap().forEach((d, futures) -> {
                typeChecker.getExternalDatum(d).whenComplete((d2, ex) -> futures.forEach(future -> {
                    future.complete(d2, ex);
                }));
            });

            resume();
            tryFinish(); // FIXME needed?
            logger.debug("{} restarted.", this);
            return true;
        }
        return false;
    }

    private void doImplicitActivate() {
        // If invoked synchronously, do a implicit transition to ACTIVE
        // runIncremental may not be used anymore!
        if(state == UnitState.INIT_TC && doRestart(false)) {
            logger.debug("{} implicitly activated.");
            this.stateTransitionTrace = TransitionTrace.INITIALLY_STARTED;
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Deadlock
    ///////////////////////////////////////////////////////////////////////////

    @Override public java.util.Set<IProcess<S, L, D>> dependentSet() {
        if(state.active() && inLocalPhase()) {
            return Collections.singleton(process);
        }
        return super.dependentSet();
    }

    @Override protected void handleDeadlock(java.util.Set<IProcess<S, L, D>> nodes) {
        if(nodes.size() == 1 && !whenActive.isDone()) {
            assertInState(UnitState.UNKNOWN);
            logger.debug("{} self-deadlocked before activation, releasing", this);
            doRelease(CapsuleUtil.toSet(addedQueries), CapsuleUtil.toSet(removedQueries),
                    PatchCollection.Immutable.<S>of().putAll(externalMatches),
                    PatchCollection.Immutable.<S>of().putAll(globalPatches));
        } else if(state.active() && inLocalPhase()) {
            doCapture();
            resume();
        } else {
            super.handleDeadlock(nodes);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Confirmation
    ///////////////////////////////////////////////////////////////////////////

    private class DifferDataOps implements IDifferDataOps<D> {

        @Override public D getExternalRepresentation(D datum) {
            return previousResult.result().analysis().getExternalRepresentation(datum);
        }

    }

    private final IEnvDifferContext<S, L, D> envDifferContext = new IEnvDifferContext<S, L, D>() {

        @Override public IFuture<ScopeDiff<S, L, D>> scopeDiff(S previousScope, L label) {
            final IFuture<ScopeDiff<S, L, D>> result = differ.scopeDiff(previousScope, label);
            if(result.isDone()) {
                return result;
            }
            final ICompletableFuture<ScopeDiff<S, L, D>> future = new CompletableFuture<>();
            final DifferState<S, L, D> state = DifferState.ofDiff(self, previousScope, label, future);
            waitFor(state, self);
            result.whenComplete(future::complete);
            future.whenComplete((r, ex) -> {
                granted(state, self);
            });
            return future;
        }

        @Override public IFuture<Optional<S>> match(S previousScope) {
            return differ.match(previousScope);
        };

        @Override public Set.Immutable<L> edgeLabels() {
            return edgeLabels;
        };
    };

    private class ConfirmationContext implements IConfirmationContext<S, L, D> {

        private final IActorRef<? extends IUnit<S, L, D, ?>> sender;

        public ConfirmationContext(IActorRef<? extends IUnit<S, L, D, ?>> sender) {
            this.sender = sender;
        }

        @Override public IFuture<IQueryAnswer<S, L, D>> query(ScopePath<S, L> scopePath, LabelWf<L> labelWf,
                LabelOrder<L> labelOrder, DataWf<S, L, D> dataWf, DataLeq<S, L, D> dataEquiv) {
            logger.debug("query from env differ.");
            return doQuery(sender, sender, false, scopePath, new NameResolutionQuery<>(labelWf, labelOrder, edgeLabels),
                    dataWf, dataEquiv, null, null);
        }

        @Override public IFuture<IQueryAnswer<S, L, D>> queryPrevious(ScopePath<S, L> scopePath, LabelWf<L> labelWf,
                DataWf<S, L, D> dataWf, LabelOrder<L> labelOrder, DataLeq<S, L, D> dataEquiv) {
            assertPreviousResultProvided();
            logger.debug("previous query from env differ.");
            return doQueryPrevious(sender, previousResult.scopeGraph(), scopePath,
                    new NameResolutionQuery<>(labelWf, labelOrder, edgeLabels), dataWf, dataEquiv);
        }

        @Override public IFuture<Optional<ConfirmResult<S, L, D>>> externalConfirm(S scope, LabelWf<L> labelWF,
                DataWf<S, L, D> dataWF, boolean prevEnvEmpty) {
            logger.debug("{} try external confirm.", this);
            return getOwner(scope).thenCompose(owner -> {
                if(owner.equals(self)) {
                    logger.debug("{} local confirm.", this);
                    return CompletableFuture.completedFuture(Optional.empty());
                }
                logger.debug("{} external confirm.", this);
                final ICompletableFuture<ConfirmResult<S, L, D>> result = new CompletableFuture<>();
                final Confirm<S, L, D> confirm = Confirm.of(self, scope, labelWF, dataWF, result);
                waitFor(confirm, owner);
                self.async(owner)._confirm(scope, labelWF, dataWF, prevEnvEmpty).whenComplete((v, ex) -> {
                    logger.trace("{} rec external confirm: {}.", this, v);
                    granted(confirm, owner);
                    resume(); // necessary!
                    if(ex == Release.instance) {
                        logger.debug("{} got release, confirming.", this);
                        result.complete(ConfirmResult.confirm());
                    } else if(ex != null) {
                        result.completeExceptionally(ex);
                    } else {
                        logger.trace("confirm: {}.", v);
                        result.complete(v);
                    }
                });
                return result.thenApply(Optional::of);
            });
        }

        @Override public IFuture<IEnvDiff<S, L, D>> envDiff(S scope, LabelWf<L> labelWf) {
            assertConfirmationEnabled();
            logger.debug("{} local env diff: {}/{}.", this, scope, labelWf);
            return whenDifferActivated.thenCompose(__ -> {
                final IFuture<IEnvDiff<S, L, D>> result = envDiffer.diff(scope, labelWf);
                if(result.isDone()) {
                    return result;
                }
                final ICompletableFuture<IEnvDiff<S, L, D>> future = new CompletableFuture<>();
                final EnvDifferState<S, L, D> state = EnvDifferState.of(sender, scope, labelWf, future);
                waitFor(state, self);
                result.whenComplete(future::complete);
                future.whenComplete((r, ex) -> {
                    logger.debug("{} granted local env diff: {}/{}: {}.", this, scope, labelWf, r);
                    granted(state, self);
                });
                return future;
            });
        }

        @Override public IFuture<Optional<S>> match(S scope) {
            return getOwner(scope).thenCompose(owner -> {
                final ICompletableFuture<Optional<S>> result = new CompletableFuture<>();

                final DifferState<S, L, D> differState = DifferState.ofMatch(self, scope, result);
                waitFor(differState, owner);

                final IFuture<Optional<S>> future = self.async(owner)._match(scope);
                future.whenComplete(result::complete);

                result.whenComplete((__, ex) -> {
                    granted(differState, owner);
                });

                return result;
            });
        }

    }

    ///////////////////////////////////////////////////////////////////////////
    // Scopegraph Capture
    ///////////////////////////////////////////////////////////////////////////

    protected boolean inLocalPhase() {
        return !whenContextActivated.isDone();
    }

    protected <Q> IFuture<Q> whenContextActive(IFuture<Q> future) {
        if(!inLocalPhase()) {
            return future;
        }
        return whenContextActivated.thenCompose(__ -> future);
    }

    private void doCapture() {
        final T snapshot = typeChecker.snapshot();
        final StateCapture.Builder<S, L, D, T> builder = StateCapture.builder();
        final Set.Immutable<S> scopes = CapsuleUtil.toSet(this.scopes);
        builder.scopes(scopes);
        builder.scopeGraph(localScopeGraph.get());
        localScopeGraph.set(ScopeGraph.Immutable.of());

        final Set.Transient<EdgeOrData<L>> _edgeLabels = CapsuleUtil.transientSet(EdgeOrData.data());
        this.edgeLabels.forEach(lbl -> _edgeLabels.__insert(EdgeOrData.edge(lbl)));
        final Set.Immutable<EdgeOrData<L>> edgeLabels = _edgeLabels.freeze();

        final MultiSet.Transient<S> unInitializedScopes = MultiSet.Transient.of();
        final MultiSet.Transient<S> openScopes = MultiSet.Transient.of();
        final MultiSetMap.Transient<S, EdgeOrData<L>> openEdges = MultiSetMap.Transient.of();

        for(S scope : scopes) {
            final int initCount = countWaitingFor(InitScope.of(self, scope), self);
            for(int i = 0; i < initCount; i++) {
                unInitializedScopes.add(scope);
            }

            final int closeCount = countWaitingFor(CloseScope.of(self, scope), self);
            for(int i = 0; i < closeCount; i++) {
                openScopes.add(scope);
            }

            for(EdgeOrData<L> label : edgeLabels) {
                final int closeLabelCount = countWaitingFor(CloseLabel.of(self, scope, label), self);
                for(int i = 0; i < closeLabelCount; i++) {
                    openEdges.put(scope, label);
                }
            }
        }

        builder.unInitializedScopes(unInitializedScopes.freeze());
        builder.openScopes(openScopes.freeze());
        builder.openEdges(openEdges.freeze());

        builder.scopeNameCounters(MultiSet.Immutable.copyOf(this.scopeNameCounters));

        final Set.Transient<String> stableIdentities = CapsuleUtil.transientSet();
        stableIdentities.__insertAll(usedStableScopes);
        builder.usedStableScopes(stableIdentities.freeze());

        builder.typeCheckerState(snapshot);
        localCapture(builder.build());
    }

    private void doRestore(StateCapture<S, L, D, T> snapshot) {
        // TODO: assert only root scopes in this.scopes?
        this.scopes.__insertAll(snapshot.scopes());

        final Set.Transient<String> currentIdentities = CapsuleUtil.transientSet();
        currentIdentities.__insertAll(usedStableScopes);
        final Set.Immutable<String> stableScopes =
                Set.Immutable.intersect(snapshot.usedStableScopes(), currentIdentities.freeze());
        this.scopeNameCounters = snapshot.scopeNameCounters().melt();

        for(S scope : snapshot.unInitializedScopes()) {
            doAddShare(self, scope);
        }

        for(S scope : snapshot.openScopes()) {
            doAddShare(self, scope);
            doInitShare(self, scope, Arrays.asList(), true);
        }

        for(S scope : snapshot.openEdges().keySet()) {
            doAddShare(self, scope);
            doInitShare(self, scope, snapshot.openEdges().get(scope), false);
        }

        // TODO: assert empty?
        // TODO: patch root scopes?
        scopeGraph.set(snapshot.scopeGraph());

        final BiMap.Transient<S> scopesToProcess = BiMap.Transient.of();
        stableScopes.forEach(name -> {
            final S scope = makeStableScope(name);
            scopesToProcess.put(scope, scope);
        });

        final Iterator<S> pScopeIterator = previousResult.rootScopes().iterator();
        for(S currentScope : rootScopes) {
            final S previousScope = pScopeIterator.next();
            scopesToProcess.put(currentScope, previousScope);
        }
        for(Map.Entry<S, S> pair : scopesToProcess.entrySet()) {
            final S currentScope = pair.getKey();
            final S previousScope = pair.getValue();
            final boolean ownedScope = context.scopeId(currentScope).equals(self.id());
            // @formatter:off
            final java.util.Set<EdgeOrData<L>> edges = edgeLabels.stream()
                .filter(label -> !snapshot.scopeGraph().getEdges(previousScope, label).isEmpty())
                .map(EdgeOrData::edge)
                .collect(Collectors.toCollection(HashSet::new));
            // @formatter:on
            doInitShare(self, currentScope, edges, false);
            for(L label : edgeLabels) { // iterate over all labels, so that also labels for which no edge exist are properly closed.
                for(S target : snapshot.scopeGraph().getEdges(previousScope, label)) {
                    if(!ownedScope) {
                        final S newTarget = matchedBySharing.patch(target);
                        self.async(parent)._addEdge(currentScope, label, newTarget);
                    }
                }
                final CloseLabel<S, L, D> closeEdge = CloseLabel.of(self, currentScope, EdgeOrData.edge(label));
                if(!snapshot.isOpen(previousScope, EdgeOrData.edge(label)) && isWaitingFor(closeEdge, self)) {
                    doCloseLabel(self, currentScope, EdgeOrData.edge(label));
                }
            }
        }

        // TODO: patch capture with new root scopes?
        localCapture(snapshot);
    }

    private void localCapture(StateCapture<S, L, D, T> capture) {
        if(localCapture.get() != null) {
            logger.error("Cannot create multiple local captures.");
            throw new IllegalStateException("Cannot create multiple local captures.");
        }
        localCapture.set(capture);
        whenContextActivated.complete(Unit.unit); // Synchronously, to ensure `inLocalPhase` works properly.
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
            if(state != UnitState.DONE) {
                return CompletableFuture.completed(r, ex);
            } else {
                return CompletableFuture.noFuture();
            }
        });
    }

    ///////////////////////////////////////////////////////////////////////////
    // Incremental settings
    ///////////////////////////////////////////////////////////////////////////

    protected void assertIncrementalEnabled() {
        if(!isIncrementalEnabled()) {
            logger.error("Incremental analysis is not enabled");
            throw new IllegalStateException("Incremental analysis is not enabled");
        }
    }

    protected void assertPreviousResultProvided() {
        if(previousResult == null) {
            logger.error("Cannot confirm queries when no previous result is provided.");
            throw new IllegalStateException("Cannot confirm queries when no previous result is provided.");
        }
    }

    protected boolean isConfirmationEnabled() {
        return context.settings().confirmation();
    }

    protected void assertConfirmationEnabled() {
        if(!isConfirmationEnabled()) {
            logger.error("Environment differ not enabled.");
            throw new IllegalStateException("Environment differ not enabled.");
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // toString
    ///////////////////////////////////////////////////////////////////////////

    @Override public String toString() {
        return "TypeCheckerUnit{" + self.id() + "}";
    }

}
