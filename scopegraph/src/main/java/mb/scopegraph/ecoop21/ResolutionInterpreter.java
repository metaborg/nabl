package mb.scopegraph.ecoop21;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.metaborg.util.Ref;
import org.metaborg.util.future.AggregateFuture;
import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.Futures;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.tuple.Tuple2;

import mb.scopegraph.oopsla20.reference.Env;
import mb.scopegraph.oopsla20.terms.newPath.ResolutionPath;
import mb.scopegraph.oopsla20.terms.newPath.ScopePath;
import mb.scopegraph.resolution.RExp;
import mb.scopegraph.resolution.RStep;
import mb.scopegraph.resolution.RVar;
import mb.scopegraph.resolution.State;
import mb.scopegraph.resolution.StateMachine;

public class ResolutionInterpreter<S, L, D, M> {

    private final IFuture<Tuple2<Env<S, L, D>, M>> EMPTY_ENV;

    private final ResolutionContext<S, L, D, M> context;
    private final StateMachine<L> stateMachine;

    public ResolutionInterpreter(ResolutionContext<S, L, D, M> context, StateMachine<L> stateMachine) {
        this.context = context;
        this.stateMachine = stateMachine;
        this.EMPTY_ENV = CompletableFuture.completedFuture(Tuple2.of(Env.empty(), context.unitMetadata()));
    }

    public IFuture<Tuple2<Env<S, L, D>, M>> resolve(ScopePath<S, L> path, State<L> state, ICancel cancel)
            throws InterruptedException {
        cancel.throwIfCancelled();

        final Store<S, L, D, M> store = new Store<>();

        for(RStep<L> step : state.resolutionSteps()) {
            evaluateStep(path, step, store, cancel);
        }

        return store.lookup(state.resultVar());
    }

    private void evaluateStep(ScopePath<S, L> path, RStep<L> step, Store<S, L, D, M> store, ICancel cancel) {
        final IFuture<Tuple2<Env<S, L, D>, M>> env = evaluateExp(path, step.getExp(), store, cancel);
        store.store(step.getVar(), env);
    }

    private IFuture<Tuple2<Env<S, L, D>, M>> evaluateExp(ScopePath<S, L> path, RExp<L> exp, Store<S, L, D, M> store,
            ICancel cancel) {
        final S scope = path.getTarget();
        final IFuture<Tuple2<Env<S, L, D>, M>> env = exp.match(new RExp.Cases<L, IFuture<Tuple2<Env<S, L, D>, M>>>() {

            @Override public IFuture<Tuple2<Env<S, L, D>, M>> caseResolve() {
                return context.getDatum(scope).thenCompose(d_opt -> {
                    return d_opt.<IFuture<Tuple2<Env<S, L, D>, M>>>map(d -> {
                        try {
                            return context.dataWf(d, cancel).thenApply(wf -> {
                                return Tuple2.of(wf._1() ? Env.of(path.resolve(d)) : Env.empty(), wf._2());
                            });
                        } catch(InterruptedException e) {
                            return CompletableFuture.completedExceptionally(e);
                        }
                    }).orElse(EMPTY_ENV);
                });
            }

            @Override public IFuture<Tuple2<Env<S, L, D>, M>> caseSubEnv(L label, String stateRef) {
                final State<L> newState = stateMachine.state(stateRef);

                return context.getEdges(scope, label).thenCompose(tgts -> {
                    return AggregateFuture.forAll(tgts, tgt -> {
                        final Optional<ScopePath<S, L>> newPathOpt = path.step(label, tgt);
                        if(newPathOpt.isPresent()) {
                            return context.externalEnv(newPathOpt.get(), newState);
                        } else {
                            return EMPTY_ENV;
                        }
                    }).thenApply(ResolutionInterpreter.this::mergeSubEnvironments);
                });
            }

            @Override public IFuture<Tuple2<Env<S, L, D>, M>> caseMerge(List<RVar> vars) {
                return AggregateFuture.forAll(vars, store::lookup)
                        .thenApply(ResolutionInterpreter.this::mergeSubEnvironments);
            }

            @Override public IFuture<Tuple2<Env<S, L, D>, M>> caseShadow(RVar left, RVar right) {
                final IFuture<Tuple2<Env<S, L, D>, M>> leftEnvFuture = store.lookup(left);
                final IFuture<Tuple2<Env<S, L, D>, M>> rightEnvFuture = store.lookup(right);

                return AggregateFuture.apply(leftEnvFuture, rightEnvFuture).thenCompose(envs -> {
                    final Env<S, L, D> leftEnv = envs._1()._1();
                    final Env<S, L, D> rightEnv = envs._2()._1();
                    M metadata = context.compose(envs._1()._2(), envs._2()._2());

                    if(rightEnv.isEmpty()) {
                        return CompletableFuture.completedFuture(Tuple2.of(leftEnv, metadata));
                    }
                    if(leftEnv.isEmpty()) {
                        return CompletableFuture.completedFuture(Tuple2.of(rightEnv, metadata));
                    }

                    final Ref<Env.Builder<S, L, D>> envBuilderRef = new Ref<>();
                    final IFuture<List<M>> future = AggregateFuture.forAll(rightEnv, path -> {
                        return isShadowed(path.getDatum(), leftEnv, cancel).thenApply(equiv -> {
                            if(!equiv._1()) {
                                if(envBuilderRef.get() == null) {
                                    final Env.Builder<S, L, D> envBuilder = Env.builder();
                                    envBuilder.addAll(leftEnv);
                                    envBuilder.add(path);
                                    envBuilderRef.set(envBuilder);
                                } else {
                                    envBuilderRef.get().add(path);
                                }
                            }
                            return equiv._2();
                        });
                    });

                    return future.thenApply(metadataList -> {
                        final M m = metadataList.stream().reduce(metadata, context::compose);
                        final Env.Builder<S, L, D> envBuilder = envBuilderRef.get();
                        if(envBuilder == null) {
                            return Tuple2.of(leftEnv, m);
                        }
                        return Tuple2.of(envBuilder.build(), m);
                    });
                });
            }

            @Override public IFuture<Tuple2<Env<S, L, D>, M>> caseCExp(RVar envVar, RExp<L> exp) {
                return store.lookup(envVar).thenCompose(env -> {
                    if(env._1().isEmpty()) {
                        return evaluateExp(path, exp, store, cancel).thenApply(res -> {
                            return Tuple2.of(res._1(), context.compose(env._2(), res._2()));
                        });
                    }
                    return CompletableFuture.completedFuture(env);
                });
            }

        });
        return env;
    }

    private IFuture<Tuple2<Boolean, M>> isShadowed(D datum, Iterable<ResolutionPath<S, L, D>> specifics,
            ICancel cancel) {
        return Futures.reduce(Tuple2.of(false, context.unitMetadata()), specifics, (acc, path) -> {
            return acc._1() ? CompletableFuture.completedFuture(acc)
                    : context.dataLeq(datum, path.getDatum(), cancel).thenApply(res -> {
                        return Tuple2.of(res._1(), context.compose(acc._2(), res._2()));
                    });
        });
    }

    private Tuple2<Env<S, L, D>, M> mergeSubEnvironments(Iterable<Tuple2<Env<S, L, D>, M>> envs) {
        M metadata = context.unitMetadata();
        Env<S, L, D> firstEnv = null;
        final Iterator<Tuple2<Env<S, L, D>, M>> envIterator = envs.iterator();

        while(envIterator.hasNext()) {
            final Tuple2<Env<S, L, D>, M> env = envIterator.next();
            metadata = context.compose(metadata, env._2());
            if(!env._1().isEmpty()) {
                firstEnv = env._1();
                break;
            }
        }

        if(firstEnv == null) {
            return Tuple2.of(Env.empty(), metadata);
        }

        Env.Builder<S, L, D> envBuilder = null;
        while(envIterator.hasNext()) {
            final Tuple2<Env<S, L, D>, M> env = envIterator.next();
            metadata = context.compose(metadata, env._2());
            if(!env._1().isEmpty()) {
                envBuilder = Env.builder();
                envBuilder.addAll(firstEnv);
                envBuilder.addAll(env._1());
                break;
            }
        }

        if(envBuilder == null) {
            return Tuple2.of(firstEnv, metadata);
        }

        while(envIterator.hasNext()) {
            final Tuple2<Env<S, L, D>, M> env = envIterator.next();
            metadata = context.compose(metadata, env._2());
            if(!env._1().isEmpty()) {
                envBuilder.addAll(env._1());
            }
        }

        return Tuple2.of(envBuilder.build(), metadata);
    }

    public interface ResolutionContext<S, L, D, M> {

        IFuture<Tuple2<Env<S, L, D>, M>> externalEnv(ScopePath<S, L> path, State<L> state);

        IFuture<Iterable<S>> getEdges(S scope, L label);

        IFuture<Optional<D>> getDatum(S scope);

        IFuture<Tuple2<Boolean, M>> dataWf(D d, ICancel cancel) throws InterruptedException;

        IFuture<Tuple2<Boolean, M>> dataLeq(D d1, D d2, ICancel cancel) throws InterruptedException;

        M unitMetadata();

        M compose(M metadata1, M metadata2);

    }

    private static class Store<S, L, D, M> {

        private final HashMap<RVar, IFuture<Tuple2<Env<S, L, D>, M>>> store = new HashMap<>();

        public void store(RVar var, IFuture<Tuple2<Env<S, L, D>, M>> value) {
            store.put(var, value);
        }

        public IFuture<Tuple2<Env<S, L, D>, M>> lookup(RVar var) {
            final IFuture<Tuple2<Env<S, L, D>, M>> value = store.get(var);
            if(value == null) {
                throw new IllegalStateException("Variable " + var + " does not exist.");
            }
            return value;
        }

    }

}
