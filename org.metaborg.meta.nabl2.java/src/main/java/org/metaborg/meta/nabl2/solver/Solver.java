package org.metaborg.meta.nabl2.solver;

import java.time.Duration;
import java.util.List;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.IConstraint.CheckedCases;
import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.unification.Unifier;
import org.metaborg.meta.nabl2.util.functions.Function1;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class Solver {

    private static final ILogger logger = LoggerUtils.logger(Solver.class);

    private final Unifier unifier;

    private final List<ISolverComponent<?>> components;
    private final AstSolver astSolver;
    private final BaseSolver baseSolver;
    private final EqualitySolver equalitySolver;
    private final NamebindingSolver namebindingSolver;
    private final RelationSolver relationSolver;
    private final SetSolver setSolver;
    private final SymbolicSolver symSolver;
    private final PolymorphismSolver polySolver;


    private final List<IMessageInfo> messages;

    private Solver(SolverConfig config, Function1<String, ITermVar> fresh) {
        this.unifier = new Unifier();
        this.components = Lists.newArrayList();

        components.add(this.baseSolver = new BaseSolver());
        components.add(this.equalitySolver = new EqualitySolver(unifier));
        components.add(this.astSolver = new AstSolver(unifier));
        components.add(this.namebindingSolver = new NamebindingSolver(config.getResolutionParams(), unifier));
        components.add(this.relationSolver = new RelationSolver(config.getRelations(), config.getFunctions(), unifier));
        components.add(this.setSolver = new SetSolver(namebindingSolver.nameSets(), unifier));
        components.add(this.symSolver = new SymbolicSolver());
        components.add(this.polySolver = new PolymorphismSolver(unifier, fresh));

        this.messages = Lists.newArrayList();
    }

    private void add(Iterable<IConstraint> constraints) {
        for(IConstraint constraint : constraints) {
            try {
                constraint.matchOrThrow(CheckedCases.of(astSolver::add, baseSolver::add, equalitySolver::add,
                    namebindingSolver::add, relationSolver::add, setSolver::add, symSolver::add, polySolver::add));
            } catch(UnsatisfiableException e) {
                messages.addAll(e.getMessages());
            }
        }
    }

    private void iterate() {
        boolean progress;
        do {
            progress = false;
            for(ISolverComponent<?> component : components) {
                try {
                    progress |= component.iterate();
                } catch(UnsatisfiableException e) {
                    progress = true;
                    messages.addAll(e.getMessages());
                }
            }
        } while(progress);
    }

    private void finish() {
        for(ISolverComponent<?> component : components) {
            messages.addAll(Lists.newArrayList(component.finish()));
        }
    }

    public static Solution solve(SolverConfig config, Function1<String, ITermVar> fresh,
        Iterable<IConstraint> constraints) throws UnsatisfiableException {
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
            solver.messages
            // @formatter:on
        );
    }

}