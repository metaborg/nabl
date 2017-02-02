package org.metaborg.meta.nabl2.solver;

import java.time.Duration;
import java.util.List;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.IConstraint.CheckedCases;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.unification.Unifier;
import org.metaborg.meta.nabl2.util.functions.Function2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.Iterables;
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
    private final SymbolicSolver symSolver;
    private final PolymorphismSolver polySolver;

    private final List<Message> errors;
    private final List<Message> warnings;
    private final List<Message> notes;

    private Solver(SolverConfig config, Function2<String,String,String> fresh) {
        this.unifier = new Unifier();
        this.baseSolver = new BaseSolver(unifier);
        this.equalitySolver = new EqualitySolver(unifier);
        this.astSolver = new AstSolver(unifier);
        this.namebindingSolver = new NamebindingSolver(config.getResolutionParams(), unifier);
        this.relationSolver = new RelationSolver(config.getRelations(), config.getFunctions(), unifier);
        this.setSolver = new SetSolver(namebindingSolver.nameSets(), unifier);
        this.symSolver = new SymbolicSolver();
        this.polySolver = new PolymorphismSolver(unifier, fresh);

        this.errors = Lists.newArrayList();
        this.warnings = Lists.newArrayList();
        this.notes = Lists.newArrayList();
    }

    private void add(Iterable<IConstraint> constraints) {
        for (IConstraint constraint : constraints) {
            try {
                constraint.matchOrThrow(CheckedCases.of(astSolver::add, baseSolver::add, equalitySolver::add,
                        namebindingSolver::add, relationSolver::add, setSolver::add, symSolver::add, polySolver::add));
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
                progress |= symSolver.iterate();
                progress |= polySolver.iterate();
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
        for (UnsatisfiableException ex : symSolver.finish()) {
            addErrors(ex);
        }
        for (UnsatisfiableException ex : polySolver.finish()) {
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

    public static Solution solve(SolverConfig config, Function2<String, String, String> fresh, Iterable<IConstraint> constraints) throws UnsatisfiableException {
        final int n = Iterables.size(constraints);
        long t0 = System.nanoTime();
        logger.info(">>> Solving {} constraints <<<", n);
        Solver solver = new Solver(config, fresh);
        solver.add(constraints);
        solver.iterate();
        solver.finish();
        long dt = System.nanoTime() - t0;
        logger.info(">>> Solved {} constraints in {} seconds <<<", n, (Duration.ofNanos(dt).toMillis() / 1000.0));
        return ImmutableSolution.of(
            // @formatter:off
            solver.astSolver.getProperties(),
            solver.namebindingSolver.getScopeGraph(),
            solver.namebindingSolver.getNameResolution(),
            solver.namebindingSolver.getProperties(),
            solver.relationSolver.getRelations(),
            solver.unifier,
            solver.symSolver.get(),
            solver.errors, solver.warnings, solver.notes
            // @formatter:on
        );
    }

}