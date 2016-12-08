package org.metaborg.meta.nabl2.solver;

import java.time.Duration;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.IConstraint.CheckedCases;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class Solver {

    private static final ILogger logger = LoggerUtils.logger(Solver.class);

    private final BaseSolver baseSolver;
    private final EqualitySolver equalitySolver;
    private final NamebindingSolver namebindingSolver;

    private final Multimap<ITerm,String> errors;
    private final Multimap<ITerm,String> warnings;
    private final Multimap<ITerm,String> notes;

    private Solver() {
        this.baseSolver = new BaseSolver();
        this.equalitySolver = new EqualitySolver();
        this.namebindingSolver = new NamebindingSolver(equalitySolver);

        this.errors = HashMultimap.create();
        this.warnings = HashMultimap.create();
        this.notes = HashMultimap.create();
    }

    private void add(Iterable<IConstraint> constraints) throws UnsatisfiableException {
        for (IConstraint constraint : constraints) {
            constraint.matchOrThrow(CheckedCases.of(baseSolver::add, equalitySolver::add, namebindingSolver::add));
        }
    }

    private void iterate() throws UnsatisfiableException {
        boolean progress;
        do {
            progress = false;
            progress |= baseSolver.iterate();
            progress |= equalitySolver.iterate();
            progress |= namebindingSolver.iterate();
        } while (progress);
    }

    private void finish() throws UnsatisfiableException {
        baseSolver.finish();
        equalitySolver.finish();
        namebindingSolver.finish();
    }

    public static Solution solve(Iterable<IConstraint> constraints) throws UnsatisfiableException {
        long t0 = System.nanoTime();
        logger.info(">>> Solving constraints <<<");
        Solver solver = new Solver();
        try {
            solver.add(constraints);
            solver.iterate();
            solver.finish();
        } catch (UnsatisfiableException e) {
            for (IConstraint c : e.getUnsatCore()) {
                c.getOriginatingTerm().ifPresent(t -> solver.errors.put(t, e.getMessage()));
            }
        }
        long dt = System.nanoTime() - t0;
        logger.info(">>> Solved constraints ({} s) <<<", (Duration.ofNanos(dt).toMillis() / 1000.0));
        return ImmutableSolution.of(solver.namebindingSolver.getScopeGraph(), solver.namebindingSolver
                .getNameResolution(), solver.errors, solver.warnings, solver.notes);
    }

}