package org.metaborg.meta.nabl2.solver;

import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.util.time.AggregateTimer;

public abstract class ASolver {

    private final SolverCore core;
    private final AggregateTimer timer;

    public ASolver(SolverCore core) {
        this.core = core;
        this.timer = new AggregateTimer();
    }

    final public AggregateTimer getTimer() {
        return timer;
    }

    // --- delegate to solver core ---

    protected SolverConfig config() {
        return core.config;
    }

    protected ITerm find(ITerm term) {
        return core.find.apply(term);
    }

    protected ITermVar fresh(String base) {
        return core.fresh.apply(base);
    }

}