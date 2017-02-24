package org.metaborg.meta.nabl2.solver;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.IConstraint.CheckedCases;
import org.metaborg.meta.nabl2.constraints.base.ImmutableCFalse;
import org.metaborg.meta.nabl2.constraints.equality.ImmutableCEqual;
import org.metaborg.meta.nabl2.constraints.messages.IMessageContent;
import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.constraints.messages.MessageContent;
import org.metaborg.meta.nabl2.solver.components.AstSolver;
import org.metaborg.meta.nabl2.solver.components.BaseSolver;
import org.metaborg.meta.nabl2.solver.components.EqualitySolver;
import org.metaborg.meta.nabl2.solver.components.NamebindingSolver;
import org.metaborg.meta.nabl2.solver.components.PolymorphismSolver;
import org.metaborg.meta.nabl2.solver.components.RelationSolver;
import org.metaborg.meta.nabl2.solver.components.SetSolver;
import org.metaborg.meta.nabl2.solver.components.SymbolicSolver;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.unification.Unifier;
import org.metaborg.meta.nabl2.util.Unit;
import org.metaborg.meta.nabl2.util.functions.Function1;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class Solver {

    private static final ILogger logger = LoggerUtils.logger(Solver.class);

    final SolverMode mode;
    final Function1<String, ITermVar> fresh;
    final Unifier unifier;

    private final List<SolverComponent<?>> components;
    private final BaseSolver baseSolver;
    private final EqualitySolver equalitySolver;
    private final AstSolver astSolver;
    private final NamebindingSolver namebindingSolver;
    private final RelationSolver relationSolver;
    private final SetSolver setSolver;
    private final SymbolicSolver symbolicSolver;
    private final PolymorphismSolver polySolver;

    private final List<IMessageInfo> messages;

    private Solver(SolverConfig config, Function1<String, ITermVar> fresh, SolverMode mode) {
        this.mode = mode;
        this.fresh = fresh;
        this.unifier = new Unifier();

        this.components = Lists.newArrayList();
        this.components.add(baseSolver = new BaseSolver(this));
        this.components.add(equalitySolver = new EqualitySolver(this));
        this.components.add(astSolver = new AstSolver(this));
        this.components.add(namebindingSolver = new NamebindingSolver(this, config.getResolutionParams()));
        this.components.add(relationSolver = new RelationSolver(this, config.getRelations(), config.getFunctions()));
        this.components.add(setSolver = new SetSolver(this, namebindingSolver.nameSets()));
        this.components.add(symbolicSolver = new SymbolicSolver(this));
        this.components.add(polySolver = new PolymorphismSolver(this));

        this.messages = Lists.newArrayList();
    }

    private void add(Iterable<IConstraint> constraints) throws InterruptedException {
        for(IConstraint constraint : constraints) {
            if(Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }
            try {
                constraint.matchOrThrow(CheckedCases.of(
                    // @formatter:off
                    c -> add(astSolver, c),
                    c -> add(baseSolver, c),
                    c -> add(equalitySolver, c),
                    c -> add(namebindingSolver, c),
                    c -> add(relationSolver, c),
                    c -> add(setSolver, c),
                    c -> add(symbolicSolver, c),
                    c -> add(polySolver, c)
                    // @formatter:on
                ));
            } catch(UnsatisfiableException ex) {
                messages.addAll(ex.getMessages());
            }
        }
    }

    private <C extends IConstraint> Unit add(SolverComponent<C> component, C constraint) throws UnsatisfiableException {
        return component.add(constraint);
    }

    private void iterate() throws InterruptedException {
        boolean progress;
        do {
            progress = false;
            for(SolverComponent<?> component : components) {
                if(Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }
                component.getTimer().start();
                try {
                    progress |= component.iterate();
                } catch(UnsatisfiableException e) {
                    progress = true;
                    messages.addAll(e.getMessages());
                } finally {
                    component.getTimer().stop();
                }
            }
        } while(progress);
    }

    private Iterable<IConstraint> finish(IMessageInfo messageInfo) throws InterruptedException {
        Set<IConstraint> unsolved = Sets.newHashSet();
        for(SolverComponent<?> component : components) {
            if(Thread.interrupted()) {
                throw new InterruptedException();
            }
            component.getTimer().start();
            try {
                unsolved.addAll(Lists.newArrayList(component.finish(messageInfo)));
            } finally {
                component.getTimer().stop();
            }
        }
        switch(mode) {
            case PARTIAL:
                messages.stream().forEach(mi -> {
                    unsolved.add(ImmutableCFalse.of(mi.apply(unifier::find)));
                });
                break;
            case TOTAL:
                unsolved.stream().forEach(c -> {
                    IMessageContent content = MessageContent.builder().append("Unsolved: ").append(c.pp()).build();
                    messages.add(c.getMessageInfo().apply(unifier::find).withDefault(content));
                });
                break;
        }
        return unsolved;
    }

    public static Iterable<IConstraint> solveIncremental(SolverConfig config, Iterable<ITerm> activeTerms,
        Function1<String, ITermVar> fresh, Iterable<IConstraint> constraints, IMessageInfo messageInfo)
        throws UnsatisfiableException, InterruptedException {
        final int n0 = Iterables.size(constraints);
        long t0 = System.nanoTime();
        logger.info(">>> Reducing {} constraints <<<", n0);

        final Unifier unifier = new Unifier();
        for(ITerm activeTerm : activeTerms) {
            unifier.addActive(activeTerm);
        }

        Solver solver = new Solver(config, fresh, SolverMode.PARTIAL);
        solver.add(constraints);
        solver.iterate();
        List<IConstraint> unsolved = Lists.newArrayList();
        Iterables.addAll(unsolved, solver.finish(messageInfo));
        for(ITerm activeTerm : activeTerms) {
            activeTerm.getVars().stream().forEach(var -> {
                unsolved.add(ImmutableCEqual.of(var, unifier.find(var), messageInfo));
            });
        }

        final int n1 = Iterables.size(unsolved);
        long dt = System.nanoTime() - t0;
        logger.info(">>> Reduced {} to {} constraints in {} seconds <<<", n0, n1,
            (Duration.ofNanos(dt).toMillis() / 1000.0));

        return unsolved;
    }

    public static Solution solveFinal(SolverConfig config, Function1<String, ITermVar> fresh,
        Iterable<IConstraint> constraints, IMessageInfo messageInfo)
        throws UnsatisfiableException, InterruptedException {
        final int n = Iterables.size(constraints);
        long t0 = System.nanoTime();
        logger.info(">>> Solving {} constraints <<<", n);

        Solver solver = new Solver(config, fresh, SolverMode.TOTAL);
        solver.add(constraints);
        solver.iterate();
        solver.finish(messageInfo);

        long dt = System.nanoTime() - t0;
        logger.info(">>> Solved {} constraints in {} seconds <<<", n, (Duration.ofNanos(dt).toMillis() / 1000.0));
        logger.info("    * namebinding : {} seconds <<<",
            (Duration.ofNanos(solver.namebindingSolver.getTimer().total()).toMillis() / 1000.0));
        logger.info("    * relations   : {} seconds <<<",
            (Duration.ofNanos(solver.relationSolver.getTimer().total()).toMillis() / 1000.0));

        return ImmutableSolution.of(
            // @formatter:off
            solver.astSolver.getProperties(),
            solver.namebindingSolver.getScopeGraph(),
            solver.namebindingSolver.getNameResolution(),
            solver.namebindingSolver.getProperties(),
            solver.relationSolver.getRelations(),
            solver.unifier,
            solver.symbolicSolver.get(),
            solver.messages
            // @formatter:on
        );
    }

}