package org.metaborg.meta.nabl2.solver;

import java.time.Duration;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.IConstraint.CheckedCases;
import org.metaborg.meta.nabl2.scopegraph.terms.ResolutionParameters;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.unification.Unifier;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class Solver {

    private static final ILogger logger = LoggerUtils.logger(Solver.class);

    private final Unifier unifier;
    private final AstSolver astSolver;
    private final BaseSolver baseSolver;
    private final EqualitySolver equalitySolver;
    private final NamebindingSolver namebindingSolver;
    private final RelationSolver relationSolver;
    private final SetSolver setSolver;

    private final Multimap<ITerm,String> errors;
    private final Multimap<ITerm,String> warnings;
    private final Multimap<ITerm,String> notes;

    private Solver(ResolutionParameters resolutionParams, Relations relations) {
        this.unifier = new Unifier();
        this.baseSolver = new BaseSolver();
        this.equalitySolver = new EqualitySolver(unifier);
        this.astSolver = new AstSolver(unifier);
        this.namebindingSolver = new NamebindingSolver(resolutionParams, unifier);
        this.relationSolver = new RelationSolver(relations, unifier);
        this.setSolver = new SetSolver(namebindingSolver.nameSets(), unifier);

        this.errors = HashMultimap.create();
        this.warnings = HashMultimap.create();
        this.notes = HashMultimap.create();
    }

    private void add(Iterable<IConstraint> constraints) {
        for (IConstraint constraint : constraints) {
            try {
                constraint.matchOrThrow(CheckedCases.of(astSolver::add, baseSolver::add, equalitySolver::add,
                        namebindingSolver::add, relationSolver::add, setSolver::add));
            } catch (UnsatisfiableException e) {
                addErrors(e);
            }
        }
    }

    private void iterate() {
        boolean progress;
        do {
            progress = false;
            progress |= baseSolver.iterate();
            progress |= astSolver.iterate();
            try {
                progress |= equalitySolver.iterate();
                progress |= namebindingSolver.iterate();
                progress |= relationSolver.iterate();
                progress |= setSolver.iterate();
            } catch (UnsatisfiableException e) {
                progress = true;
                addErrors(e);
            }
        } while (progress);
    }

    private void finish() {
        baseSolver.finish();
        astSolver.finish();
        try {
            equalitySolver.finish();
        } catch (UnsatisfiableException e) {
            addErrors(e);
        }
        try {
            namebindingSolver.finish();
        } catch (UnsatisfiableException e) {
            addErrors(e);
        }
        try {
            relationSolver.finish();
        } catch (UnsatisfiableException e) {
            addErrors(e);
        }
        try {
            setSolver.finish();
        } catch (UnsatisfiableException e) {
            addErrors(e);
        }
    }

    private void addErrors(UnsatisfiableException e) {
        for (IConstraint c : e.getUnsatCore()) {
            c.getOriginatingTerm().ifPresent(t -> {
                errors.put(t, e.getMessage());
            });
        }
    }

    public static Solution solve(ResolutionParameters resolutionParams, Relations relations,
            Iterable<IConstraint> constraints) throws UnsatisfiableException {
        long t0 = System.nanoTime();
        logger.info(">>> Solving constraints <<<");
        Solver solver = new Solver(resolutionParams, relations);
        solver.add(constraints);
        solver.iterate();
        solver.finish();
        long dt = System.nanoTime() - t0;
        logger.info(">>> Solved constraints ({} s) <<<", (Duration.ofNanos(dt).toMillis() / 1000.0));
        return ImmutableSolution.of(
            // @formatter:off
            solver.astSolver.getProperties(),
            solver.namebindingSolver.getScopeGraph(),
            solver.namebindingSolver.getNameResolution(),
            solver.namebindingSolver.getProperties(),
            solver.relationSolver.getRelations(),
            solver.unifier,
            solver.errors, solver.warnings, solver.notes
            // @formatter:on
        );
    }

}