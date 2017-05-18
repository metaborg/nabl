package org.metaborg.meta.nabl2.solver_new;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.solver.SolverConfig;
import org.metaborg.meta.nabl2.solver.messages.IMessages;
import org.metaborg.meta.nabl2.solver.messages.Messages;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.unification.IUnifier;
import org.metaborg.meta.nabl2.unification.Unifier;
import org.metaborg.meta.nabl2.unification.VarTracker;
import org.metaborg.meta.nabl2.util.functions.Function1;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;

public class SolverCore {

    final SolverConfig config;
    final ICancel cancel;
    final IProgress progress;

    final Function1<String, ITermVar> fresh;
    final IUnifier.Transient unifier;
    final VarTracker<IConstraint> tracker;
    final IMessages.Builder messages;

    public SolverCore(SolverConfig config, Function1<String, ITermVar> fresh, ICancel cancel, IProgress progress,
            CoreResult... results) {
        this.config = config;
        this.cancel = cancel;
        this.progress = progress;
        this.fresh = fresh;
        this.unifier = new Unifier.Transient();
        this.tracker = new VarTracker<>(unifier);
        this.messages = new Messages.Builder();
        for(CoreResult result : results) {
            unifier.merge(result.unifier());
            messages.merge(result.messages());
        }
    }

    public CoreResult finish() {
        return ImmutableCoreResult.of(unifier.freeze(), messages.build());
    }

    @Value.Immutable
    @Serial.Version(42L)
    public static abstract class CoreResult {

        @Value.Parameter public abstract IUnifier unifier();

        @Value.Parameter public abstract IMessages messages();

    }

}