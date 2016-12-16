package org.metaborg.meta.nabl2.solver;

import java.time.Duration;
import java.util.List;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.IConstraint.CheckedCases;
import org.metaborg.meta.nabl2.relations.terms.Relations;
import org.metaborg.meta.nabl2.scopegraph.terms.ResolutionParameters;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.unification.Unifier;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.Lists;

public class Solver {

    private static final ILogger logger = LoggerUtils.logger(Solver.class);

    private final Unifier unifier;
    private final AstSolver astSolver;
    private final BaseSolver baseSolver;
    private final EqualitySolver equalitySolver;
    private final NamebindingSolver namebindingSolver;
    private final RelationSolver relationSolver;
    private final SetSolver setSolver;

    private final List<Message> errors;
    private final List<Message> warnings;
    private final List<Message> notes;

    private Solver(ResolutionParameters resolutionParams, Relations<ITerm> relations) {
        this.unifier = new Unifier();
        this.baseSolver = new BaseSolver(unifier);
        this.equalitySolver = new EqualitySolver(unifier);
        this.astSolver = new AstSolver(unifier);
        this.namebindingSolver = new NamebindingSolver(resolutionParams, unifier);
        this.relationSolver = new RelationSolver(relations, unifier);
        this.setSolver = new SetSolver(namebindingSolver.nameSets(), unifier);

        this.errors = Lists.newArrayList();
        this.warnings = Lists.newArrayList();
        this.notes = Lists.newArrayList();
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
        for (UnsatisfiableException ex : baseSolver.finish()) {
            addErrors(ex);
        }
        for (UnsatisfiableException ex : astSolver.finish()) {
            addErrors(ex);
        }
        for (UnsatisfiableException ex : equalitySolver.finish()) {
            addErrors(ex);
        }
        for (UnsatisfiableException ex : namebindingSolver.finish()) {
            addErrors(ex);
        }
        for (UnsatisfiableException ex : relationSolver.finish()) {
            addErrors(ex);
        }
        for (UnsatisfiableException ex : setSolver.finish()) {
            addErrors(ex);
        }
    }

    private void addErrors(UnsatisfiableException e) {
        for (ITerm t : e.getProgramPoints()) {
            ImmutableMessage message = ImmutableMessage.of(t, e.getMessage());
            switch (e.getKind()) {
            case ERROR:
                errors.add(message);
                break;
            case WARNING:
                warnings.add(message);
                break;
            case NOTE:
                notes.add(message);
                break;
            }
        }
    }

    public static Solution solve(ResolutionParameters resolutionParams, Relations<ITerm> relations,
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