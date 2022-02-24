package mb.scopegraph.ecoop21;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.metaborg.util.future.AggregateFuture;
import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.Futures;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.unit.Unit;

import mb.scopegraph.oopsla20.reference.Env;
import mb.scopegraph.oopsla20.terms.newPath.ResolutionPath;
import mb.scopegraph.oopsla20.terms.newPath.ScopePath;
import mb.scopegraph.resolution.RExp;
import mb.scopegraph.resolution.RStep;
import mb.scopegraph.resolution.RVar;
import mb.scopegraph.resolution.State;
import mb.scopegraph.resolution.StateMachine;

public class ResolutionInterpreter<S, L, D> {

    private final IFuture<Env<S, L, D>> EMPTY_ENV = CompletableFuture.completedFuture(Env.empty());

    private final ResolutionContext<S, L, D> context;
    private final StateMachine<L> stateMachine;

    public ResolutionInterpreter(ResolutionContext<S, L, D> context, StateMachine<L> stateMachine) {
        this.context = context;
        this.stateMachine = stateMachine;
    }

    public IFuture<Env<S, L, D>> resolve(ScopePath<S, L> path, State<L> state, LabelWf<L> labelWf /* FIXME: for confirmation. */, ICancel cancel)
            throws InterruptedException {
        cancel.throwIfCancelled();

        final Store<S, L, D> store = new Store<>();
        for(RStep<L> step : state.resolutionSteps()) {
            evaluateStep(path, step, store, labelWf, cancel);
        }

        return store.lookup(state.resultVar());
    }

    private void evaluateStep(ScopePath<S, L> path, RStep<L> step, Store<S, L, D> store, LabelWf<L> labelWf, ICancel cancel) {
        final IFuture<Env<S, L, D>> env = evaluateExp(path, step.getExp(), store, labelWf, cancel);
        store.store(step.getVar(), env);
    }

    private IFuture<Env<S, L, D>> evaluateExp(ScopePath<S, L> path, RExp<L> exp, Store<S, L, D> store, LabelWf<L> labelWf, ICancel cancel) {
        final S scope = path.getTarget();
        final IFuture<Env<S, L, D>> env = exp.match(new RExp.Cases<L, IFuture<Env<S, L, D>>>() {

            @Override public IFuture<Env<S, L, D>> caseResolve() {
                return context.getDatum(scope).<Env<S, L, D>>thenCompose(d_opt -> {
                    return d_opt.<IFuture<Env<S, L, D>>>map(d -> {
                        try {
                            return context.dataWf(d, cancel).<Env<S, L, D>>thenApply(wf -> {
                                return wf ? Env.of(path.resolve(d)) : Env.empty();
                            });
                        } catch(InterruptedException e) {
                            return CompletableFuture.completedExceptionally(e);
                        }
                    }).orElse(EMPTY_ENV);
                });
            }

            @Override public IFuture<Env<S, L, D>> caseSubEnv(L label, String stateRef) {
                final State<L> newState = stateMachine.state(stateRef);

                return context.getEdges(scope, label).thenCompose(tgts -> {
                    final IFuture<List<Env<S, L, D>>> envsFuture = AggregateFuture.forAll(tgts, tgt -> {
                        final Optional<ScopePath<S, L>> newPathOpt = path.step(label, tgt);
                        if(newPathOpt.isPresent()) {
                            return context.externalEnv(newPathOpt.get(), newState, labelWf.step(label).get() /* Safe in valid SM */);
                        } else {
                            return EMPTY_ENV;
                        }
                    });

                    return envsFuture.thenApply(envs -> {
                        final Env.Builder<S, L, D> envBuilder = Env.builder();
                        for(Env<S, L, D> env : envs) {
                            envBuilder.addAll(env);
                        }
                        return envBuilder.build();
                    });
                });
            }

            @Override public IFuture<Env<S, L, D>> caseMerge(List<RVar> vars) {
                final IFuture<List<Env<S, L, D>>> envsFuture = AggregateFuture.forAll(vars, store::lookup);
                return envsFuture.thenApply(envs -> {
                    final Env.Builder<S, L, D> envBuilder = Env.builder();
                    envs.forEach(envBuilder::addAll);
                    return envBuilder.build();
                });
            }

            @Override public IFuture<Env<S, L, D>> caseShadow(RVar left, RVar right) {
                final IFuture<Env<S, L, D>> leftEnvFuture = store.lookup(left);
                final IFuture<Env<S, L, D>> rightEnvFuture = store.lookup(right);

                return AggregateFuture.apply(leftEnvFuture, rightEnvFuture).thenCompose(envs -> {
                    final Env<S, L, D> leftEnv = envs._1();
                    final Env<S, L, D> rightEnv = envs._2();

                    final Env.Builder<S, L, D> envBuilder = Env.builder();
                    envBuilder.addAll(leftEnv);

                    final IFuture<List<Unit>> future = AggregateFuture.forAll(rightEnv, path -> {
                        return isShadowed(path.getDatum(), leftEnv, cancel).thenApply(equiv -> {
                            if(!equiv) {
                                envBuilder.add(path);
                            }
                            return Unit.unit;
                        });
                    });

                    return future.thenApply(__ -> envBuilder.build());
                });
            }

            @Override public IFuture<Env<S, L, D>> caseCExp(RVar envVar, RExp<L> exp) {
                return store.lookup(envVar).thenCompose(env -> {
                    return env.isEmpty() ? evaluateExp(path, exp, store, labelWf, cancel)
                            : CompletableFuture.completedFuture(env);
                });
            }

        });
        return env;
    }

    private IFuture<Boolean> isShadowed(D datum, Iterable<ResolutionPath<S, L, D>> specifics, ICancel cancel) {
        return Futures.reduce(false, specifics, (acc, path) -> {
            return acc ? CompletableFuture.completedFuture(true) : context.dataLeq(datum, path.getDatum(), cancel);
        });
    }

    public interface ResolutionContext<S, L, D> {

        IFuture<Env<S, L, D>> externalEnv(ScopePath<S, L> path, State<L> state, LabelWf<L> labelWf /* FIXME: For confirmation */);

        IFuture<Iterable<S>> getEdges(S scope, L label);

        IFuture<Optional<D>> getDatum(S scope);

        IFuture<Boolean> dataWf(D d, ICancel cancel) throws InterruptedException;

        IFuture<Boolean> dataLeq(D d1, D d2, ICancel cancel) throws InterruptedException;

    }

    private static class Store<S, L, D> {

        private final HashMap<RVar, IFuture<Env<S, L, D>>> store = new HashMap<>();

        public void store(RVar var, IFuture<Env<S, L, D>> value) {
            store.put(var, value);
        }

        public IFuture<Env<S, L, D>> lookup(RVar var) {
            final IFuture<Env<S, L, D>> value = store.get(var);
            if(value == null) {
                throw new IllegalStateException("Variable " + var + " does not exist.");
            }
            return value;
        }

    }

}
