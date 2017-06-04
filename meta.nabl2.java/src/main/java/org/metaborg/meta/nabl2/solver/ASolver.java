package org.metaborg.meta.nabl2.solver;

import java.util.Optional;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.util.time.AggregateTimer;

public abstract class ASolver<C extends IConstraint, R> implements ISolver<C, R> {

    private final SolverCore core;
    private final AggregateTimer timer;

    public ASolver(SolverCore core) {
        this.core = core;
        this.timer = new AggregateTimer();
    }

    public SeedResult seed(R solution, IMessageInfo message) throws InterruptedException {
        return SeedResult.empty();
    }

    public Optional<SolveResult> solve(C constraint) throws InterruptedException {
        return Optional.empty();
    }

    @Override public boolean update() throws InterruptedException {
        return false;
    }

    final public AggregateTimer getTimer() {
        return timer;
    }

    // --- delegate to solver core ---

    protected SolverConfig config() {
        return core.config;
    }

    protected ITerm find(ITerm term) {
        return core.unifier.find(term);
    }

    protected ITermVar fresh(String base) {
        return core.fresh.apply(base);
    }

}