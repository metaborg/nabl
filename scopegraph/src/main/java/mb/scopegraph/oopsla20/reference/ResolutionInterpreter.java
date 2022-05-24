package mb.scopegraph.oopsla20.reference;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.metaborg.util.functions.Predicate2;
import org.metaborg.util.task.ICancel;

import mb.scopegraph.oopsla20.IScopeGraph;
import mb.scopegraph.oopsla20.terms.newPath.ResolutionPath;
import mb.scopegraph.oopsla20.terms.newPath.ScopePath;
import mb.scopegraph.resolution.RExp;
import mb.scopegraph.resolution.RStep;
import mb.scopegraph.resolution.RVar;
import mb.scopegraph.resolution.State;
import mb.scopegraph.resolution.StateMachine;

public class ResolutionInterpreter<S, L, D> {

    private final IScopeGraph.Immutable<S, L, D> scopeGraph;

    private final DataWF<D> dataWf;
    private final DataLeq<D> dataEquiv;

    private final StateMachine<L> stateMachine;

    private final Predicate2<S, EdgeOrData<L>> isComplete;

    public ResolutionInterpreter(IScopeGraph.Immutable<S, L, D> scopeGraph, DataWF<D> dataWf, DataLeq<D> dataEquiv,
            StateMachine<L> stateMachine, Predicate2<S, EdgeOrData<L>> isComplete) {
        this.scopeGraph = scopeGraph;
        this.dataWf = dataWf;
        this.dataEquiv = dataEquiv;
        this.stateMachine = stateMachine;
        this.isComplete = isComplete;
    }

    public Env<S, L, D> resolve(S scope, ICancel cancel) throws ResolutionException, InterruptedException {
        return resolve(new ScopePath<S, L>(scope), stateMachine.initial(), cancel);
    }

    private Env<S, L, D> resolve(ScopePath<S, L> path, State<L> state, ICancel cancel)
            throws ResolutionException, InterruptedException {
        cancel.throwIfCancelled();

        final Store<S, L, D> store = new Store<>();
        for(RStep<L> step : state.resolutionSteps()) {
            evaluateStep(path, step, store, cancel);
        }

        return store.lookup(state.resultVar());
    }

    private void evaluateStep(ScopePath<S, L> path, RStep<L> step, Store<S, L, D> store, ICancel cancel)
            throws ResolutionException, InterruptedException {
        final Env<S, L, D> env = evaluateExp(path, step.getExp(), store, cancel);
        store.store(step.getVar(), env);
    }

    private Env<S, L, D> evaluateExp(ScopePath<S, L> path, RExp<L> exp, Store<S, L, D> store, ICancel cancel)
            throws ResolutionException, InterruptedException {
        final S scope = path.getTarget();
        final Env<S, L, D> env = exp.matchInResolution(new RExp.ResolutionCases<L, Env<S, L, D>>() {

            @Override public Env<S, L, D> caseResolve() throws ResolutionException, InterruptedException {
                checkComplete(scope, EdgeOrData.data());

                Optional<D> datum = scopeGraph.getData(scope);
                if(datum.isPresent() && dataWf.wf(datum.get())) {
                    return Env.of(path.resolve(datum.get()));
                }
                return Env.empty();
            }

            @Override public Env<S, L, D> caseSubEnv(L label, String stateRef)
                    throws ResolutionException, InterruptedException {
                checkComplete(scope, EdgeOrData.edge(label));

                final State<L> newState = stateMachine.state(stateRef);
                final Iterator<S> targetIterator = scopeGraph.getEdges(scope, label).iterator();

                Env<S, L, D> firstEnv = null;
                while(targetIterator.hasNext()) {
                    final S target = targetIterator.next();
                    final Optional<ScopePath<S, L>> pathOpt = path.step(label, target);
                    if(pathOpt.isPresent()) {
                        final Env<S, L, D> targetEnv = resolve(pathOpt.get(), newState, cancel);
                        if(!targetEnv.isEmpty()) {
                            firstEnv = targetEnv;
                            break;
                        }
                    }
                }

                if(firstEnv == null) {
                    // No non-empty subenvironment found
                    return Env.empty();
                }

                Env.Builder<S, L, D> envBuilder = null;
                while(targetIterator.hasNext()) {
                    final S target = targetIterator.next();
                    final Optional<ScopePath<S, L>> pathOpt = path.step(label, target);
                    if(pathOpt.isPresent()) {
                        final Env<S, L, D> targetEnv = resolve(pathOpt.get(), newState, cancel);
                        if(!targetEnv.isEmpty()) {
                            envBuilder = Env.builder();
                            envBuilder.addAll(firstEnv);
                            envBuilder.addAll(targetEnv);
                            break;
                        }
                    }
                }

                if(envBuilder == null) {
                    // No second non-empty environment found
                    return firstEnv;
                }

                while(targetIterator.hasNext()) {
                    final S target = targetIterator.next();
                    final Optional<ScopePath<S, L>> pathOpt = path.step(label, target);
                    if(pathOpt.isPresent()) {
                        final Env<S, L, D> targetEnv = resolve(pathOpt.get(), newState, cancel);
                        if(!targetEnv.isEmpty()) {
                            envBuilder.addAll(targetEnv);
                        }
                    }
                }

                return envBuilder.build();
            }

            @Override public Env<S, L, D> caseMerge(List<RVar> envs) {
                final Iterator<RVar> varIterator = envs.iterator();
                Env<S, L, D> firstEnv = null;
                while(varIterator.hasNext()) {
                    final Env<S, L, D> env = store.lookup(varIterator.next());
                    if(!env.isEmpty()) {
                        firstEnv = env;
                        break;
                    }
                }

                if(firstEnv == null) {
                    // No non-empty environment found
                    return Env.empty();
                }

                Env.Builder<S, L, D> envBuilder = null;
                while(varIterator.hasNext()) {
                    final Env<S, L, D> env = store.lookup(varIterator.next());
                    if(!env.isEmpty()) {
                        envBuilder = Env.builder();
                        envBuilder.addAll(firstEnv);
                        envBuilder.addAll(env);
                        break;
                    }
                }

                if(envBuilder == null) {
                    // No second non-empty environment found
                    return firstEnv;
                }

                while(varIterator.hasNext()) {
                    final Env<S, L, D> env = store.lookup(varIterator.next());
                    if(!env.isEmpty()) {
                        envBuilder.addAll(env);
                    }
                }

                return envBuilder.build();
            }

            @Override public Env<S, L, D> caseShadow(RVar left, RVar right)
                    throws ResolutionException, InterruptedException {
                final Env<S, L, D> leftEnv = store.lookup(left);
                final Env<S, L, D> rightEnv = store.lookup(right);
                if(leftEnv.isEmpty()) {
                    return rightEnv;
                }
                if(rightEnv.isEmpty()) {
                    return leftEnv;
                }

                final Env.Builder<S, L, D> envBuilder = Env.builder();
                envBuilder.addAll(leftEnv);

                for(ResolutionPath<S, L, D> path : rightEnv) {
                    if(!isShadowed(path.getDatum(), leftEnv)) {
                        envBuilder.add(path);
                    }
                }

                return envBuilder.build();
            }

            @Override public Env<S, L, D> caseCExp(RVar envVar, RExp<L> exp)
                    throws ResolutionException, InterruptedException {
                final Env<S, L, D> env = store.lookup(envVar);
                if(!env.isEmpty()) {
                    return env;
                }

                return this.match(exp);
            }

        });
        return env;
    }

    private void checkComplete(S scope, EdgeOrData<L> edge) throws IncompleteException {
        if(!isComplete.test(scope, edge)) {
            throw new IncompleteException(scope, edge);
        }
    }

    private boolean isShadowed(D datum, Iterable<ResolutionPath<S, L, D>> specifics)
            throws ResolutionException, InterruptedException {
        for(ResolutionPath<S, L, D> p : specifics) {
            if(dataEquiv.leq(p.getDatum(), datum)) {
                return true;
            }
        }
        return false;
    }

    private static class Store<S, L, D> {

        private final HashMap<RVar, Env<S, L, D>> store = new HashMap<>();

        public void store(RVar var, Env<S, L, D> value) {
            store.put(var, value);
        }

        public Env<S, L, D> lookup(RVar var) {
            final Env<S, L, D> value = store.get(var);
            if(value == null) {
                throw new IllegalStateException("Variable " + var + " does not exist.");
            }
            return value;
        }

    }

}
