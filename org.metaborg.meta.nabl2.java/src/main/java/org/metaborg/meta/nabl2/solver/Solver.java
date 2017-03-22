package org.metaborg.meta.nabl2.solver;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.metaborg.meta.nabl2.constraints.Constraints;
import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.IConstraint.CheckedCases;
import org.metaborg.meta.nabl2.constraints.base.ImmutableCFalse;
import org.metaborg.meta.nabl2.constraints.base.ImmutableCTrue;
import org.metaborg.meta.nabl2.constraints.equality.ImmutableCEqual;
import org.metaborg.meta.nabl2.constraints.messages.IMessageContent;
import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.constraints.messages.MessageContent;
import org.metaborg.meta.nabl2.scopegraph.terms.Scope;
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
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.unification.Unifier;
import org.metaborg.meta.nabl2.util.Unit;
import org.metaborg.meta.nabl2.util.functions.Function1;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class Solver {

    private static final ILogger logger = LoggerUtils.logger(Solver.class);

    final SolverMode mode;
    final Function1<String, ITermVar> fresh;
    final Unifier<IConstraint> unifier;
    final ICancel cancel;
    final IProgress progress;

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

    private Solver(SolverConfig config, Function1<String, ITermVar> fresh, SolverMode mode, IProgress progress,
        ICancel cancel) {
        this.mode = mode;
        this.fresh = fresh;
        this.unifier = new Unifier<>();
        this.cancel = cancel;
        this.progress = progress;

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
        final int n = Iterables.size(constraints);
        progress.setWorkRemaining(n + 1);
        for(IConstraint constraint : constraints) {
            cancel.throwIfCancelled();
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
                cancel.throwIfCancelled();
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
            cancel.throwIfCancelled();
            component.getTimer().start();
            try {
                Iterables.addAll(unsolved, component.finish(messageInfo));
            } finally {
                component.getTimer().stop();
            }
        }
        switch(mode) {
            case PARTIAL:
                messages.stream().map(ImmutableCFalse::of).forEach(unsolved::add);
            case TOTAL:
            default:
                unsolved.stream().forEach(c -> {
                    IMessageContent content = MessageContent.builder().append("Unsolved: ").append(c.pp()).build();
                    messages.add(c.getMessageInfo().withDefaultContent(content));
                });
        }
        unsolved = unsolved.stream().map(c -> Constraints.find(c, unifier)).collect(Collectors.toSet());
        // this must be added after unification, because we want to retain the active variable on the left
        if(SolverMode.PARTIAL.equals(mode)) {
            for(ITermVar var : unifier.getActiveVars()) {
                final ITerm rep = unifier.find(var);
                if(!rep.equals(var)) {
                    unsolved.add(ImmutableCEqual.of(var, rep, messageInfo));
                }
            }
        }
        return unsolved;
    }

    private void check(Collection<IConstraint> unsolved) {
    }

    public static Iterable<IConstraint> solveIncremental(SolverConfig config, Iterable<ITerm> activeTerms,
        Function1<String, ITermVar> fresh, Iterable<IConstraint> constraints, IMessageInfo messageInfo,
        IProgress progress, ICancel cancel) throws SolverException, InterruptedException {
        final int n0 = Iterables.size(constraints);
        long t0 = System.nanoTime();
        logger.info(">>> Reducing {} constraints <<<", n0);

        Solver solver = new Solver(config, fresh, SolverMode.PARTIAL, progress, cancel);
        for(ITerm activeTerm : activeTerms) {
            solver.unifier.addActive(activeTerm, ImmutableCTrue.of(messageInfo));
            solver.namebindingSolver.addActive(M.collecttd(Scope.matcher()).apply(activeTerm));
        }

        List<IConstraint> unsolved = Lists.newArrayList();
        try {
            solver.add(constraints);
            solver.iterate();
            Iterables.addAll(unsolved, solver.finish(messageInfo));
        } catch(RuntimeException ex) {
            throw new SolverException("Internal solver error.", ex);
        }
        solver.check(unsolved);

        final int n1 = Iterables.size(unsolved);
        long dt = System.nanoTime() - t0;
        logger.info(">>> Reduced {} to {} constraints in {} seconds <<<", n0, n1,
            (Duration.ofNanos(dt).toMillis() / 1000.0));

        return unsolved;
    }

    public static Solution solveFinal(SolverConfig config, Function1<String, ITermVar> fresh,
        Iterable<IConstraint> constraints, IMessageInfo messageInfo, IProgress progress, ICancel cancel)
        throws SolverException, InterruptedException {
        final int n = Iterables.size(constraints);
        long t0 = System.nanoTime();
        logger.info(">>> Solving {} constraints <<<", n);

        Solver solver = new Solver(config, fresh, SolverMode.TOTAL, progress, cancel);
        List<IConstraint> unsolved = Lists.newArrayList();
        try {
            solver.add(constraints);
            solver.iterate();
            Iterables.addAll(unsolved, solver.finish(messageInfo));
        } catch(RuntimeException ex) {
            throw new SolverException("Internal solver error.", ex);
        }
        solver.check(unsolved);

        long dt = System.nanoTime() - t0;
        logger.info(">>> Solved {} constraints in {} seconds <<<", n, (Duration.ofNanos(dt).toMillis() / 1000.0));
        logger.info("    * namebinding : {} seconds",
            (Duration.ofNanos(solver.namebindingSolver.getTimer().total()).toMillis() / 1000.0));
        logger.info("    * relations   : {} seconds",
            (Duration.ofNanos(solver.relationSolver.getTimer().total()).toMillis() / 1000.0));
        logger.info("    * unsolved    : {}", unsolved.size());

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