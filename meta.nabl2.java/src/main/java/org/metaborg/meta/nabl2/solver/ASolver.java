package org.metaborg.meta.nabl2.solver;

import java.util.Optional;

import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.unification.IUnifier;
import org.metaborg.util.iterators.Iterables2;
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

    protected IUnifier unifier() {
        return core.unifier;
    }

    protected String fresh(String base) {
        return core.fresh.apply(base);
    }

    protected Optional<ITerm> callExternal(String name, ITerm... args) {
        return core.callExternal.apply(name, Iterables2.from(args));
    }

}