package mb.p_raffrayi.nameresolution;

import java.util.Optional;

import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.task.ICancel;

import mb.scopegraph.ecoop21.LabelWf;
import mb.scopegraph.ecoop21.ResolutionInterpreter;
import mb.scopegraph.ecoop21.ResolutionInterpreter.ResolutionContext;
import mb.scopegraph.oopsla20.reference.Env;
import mb.scopegraph.oopsla20.terms.newPath.ScopePath;
import mb.scopegraph.resolution.State;
import mb.scopegraph.resolution.StateMachine;

public class StateMachineQuery<S, L, D> implements IQuery<S, L, D> {

    private final StateMachine<L> stateMachine;
    private final State<L> state;

    private final LabelWf<L> labelWf;

    public StateMachineQuery(StateMachine<L> stateMachine, LabelWf<L> labelWf) {
        this.stateMachine = stateMachine;
        this.state = stateMachine.initial();
        this.labelWf = labelWf;
    }

    public StateMachineQuery(StateMachine<L> stateMachine, State<L> state, LabelWf<L> labelWf) {
        this.stateMachine = stateMachine;
        this.state = state;
        this.labelWf = labelWf;
    }

    @Override public IFuture<Env<S, L, D>> resolve(IResolutionContext<S, L, D> context, ScopePath<S, L> path, ICancel cancel) {
        final ResolutionContext<S, L, D> resolutionContext = new ResolutionContext<S, L, D>() {

            @Override public IFuture<Env<S, L, D>> externalEnv(ScopePath<S, L> path, State<L> state, LabelWf<L> labelWf) {
                return context.externalEnv(path, new StateMachineQuery<>(stateMachine, state, labelWf), cancel);
            }

            @Override public IFuture<Iterable<S>> getEdges(S scope, L label) {
                return context.getEdges(scope, label);
            }

            @Override public IFuture<Optional<D>> getDatum(S scope) {
                return context.getDatum(scope);
            }

            @Override public IFuture<Boolean> dataWf(D d, ICancel cancel) throws InterruptedException {
                return context.dataWf(d, cancel);
            }

            @Override public IFuture<Boolean> dataLeq(D d1, D d2, ICancel cancel) throws InterruptedException {
                return context.dataEquiv(d1, d2, cancel);
            }
        };

        try {
            final ResolutionInterpreter<S, L, D> interp = new ResolutionInterpreter<>(resolutionContext, stateMachine);
            return interp.resolve(path, state, labelWf, cancel);
        } catch(InterruptedException e) {
            return CompletableFuture.completedExceptionally(e);
        }
    }

    @Override public LabelWf<L> labelWf() {
        return labelWf;
    }

}
