package mb.nabl2.solver;

import java.util.Arrays;
import java.util.Optional;

import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;
import org.metaborg.util.time.AggregateTimer;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.u.IUnifier;

public abstract class ASolver {

    private final SolverCore core;
    private final AggregateTimer timer;
    protected final ICancel cancel;
    protected final IProgress progress;

    public ASolver(SolverCore core) {
        this.core = core;
        this.timer = new AggregateTimer();
        this.cancel = core.cancel;
        this.progress = core.progress;
    }

    final public AggregateTimer getTimer() {
        return timer;
    }

    // --- delegate to solver core ---

    protected SolverConfig config() {
        return core.config;
    }

    protected IUnifier unifier() {
        return core.unifier.get();
    }

    protected String fresh(String base) {
        return core.fresh.apply(base);
    }

    protected Optional<ITerm> callExternal(String name, ITerm... args) {
        return core.callExternal.apply(name, Arrays.asList(args));
    }

}