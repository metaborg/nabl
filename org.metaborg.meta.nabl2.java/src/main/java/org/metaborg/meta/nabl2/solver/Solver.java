package org.metaborg.meta.nabl2.solver;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.metaborg.meta.nabl2.config.NaBL2DebugConfig;
import org.metaborg.meta.nabl2.constraints.Constraints;
import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.IConstraint.CheckedCases;
import org.metaborg.meta.nabl2.constraints.base.ImmutableCTrue;
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
import org.metaborg.meta.nabl2.solver.messages.Messages;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.unification.IUnifier;
import org.metaborg.meta.nabl2.unification.UnificationException;
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

    private final SolverConfig config;
    final SolverMode mode;
    final Function1<String, ITermVar> fresh;
    final Unifier<IConstraint> unifier;
    final ICancel cancel;
    final IProgress progress;
    final NaBL2DebugConfig debugConfig;

    private final List<SolverComponent<?>> components;
    private final BaseSolver baseSolver;
    private final EqualitySolver equalitySolver;
    private final AstSolver astSolver;
    private final NamebindingSolver namebindingSolver;
    private final RelationSolver relationSolver;
    private final SetSolver setSolver;
    private final SymbolicSolver symbolicSolver;
    private final PolymorphismSolver polySolver;

    private final Messages messages;

    private Solver(SolverConfig config, Function1<String, ITermVar> fresh, SolverMode mode, IProgress progress,
            ICancel cancel, NaBL2DebugConfig debugConfig) {
        this.config = config;
        this.mode = mode;
        this.fresh = fresh;
        this.unifier = new Unifier<>();
        this.cancel = cancel;
        this.progress = progress;
        this.debugConfig = debugConfig;

        this.components = Lists.newArrayList();
        this.components.add(baseSolver = new BaseSolver(this));
        this.components.add(equalitySolver = new EqualitySolver(this));
        this.components.add(astSolver = new AstSolver(this));
        this.components.add(namebindingSolver = new NamebindingSolver(this, config.getResolutionParams()));
        this.components.add(relationSolver = new RelationSolver(this, config.getRelations(), config.getFunctions()));
        this.components.add(setSolver = new SetSolver(this, namebindingSolver.nameSets()));
        this.components.add(symbolicSolver = new SymbolicSolver(this));
        this.components.add(polySolver = new PolymorphismSolver(this));

        this.messages = new Messages();
    }

    // --- solver life cycle ---

    private void addAll(Collection<PartialSolution> partialSolutions, IMessageInfo protoMessage)
            throws InterruptedException {
        for(PartialSolution partialSolution : partialSolutions) {
            if(!partialSolution.getConfig().equals(config)) {
                throw new IllegalArgumentException(
                        "Partial solution was computed with a different solver configuration.");
            }
            messages.addAll(partialSolution.getMessages());
            IUnifier otherUnifier = partialSolution.getUnifier();
            for(ITermVar var : otherUnifier.getAllVars()) {
                try {
                    unifier.unify(var, otherUnifier.find(var));
                } catch(UnificationException ex) {
                    messages.add(protoMessage.withDefaultContent(ex.getMessageContent()));
                }
            }
            addAll(partialSolution.getResidualConstraints());
        }
    }

    private void addAll(Collection<IConstraint> constraints) throws InterruptedException {
        List<IConstraint> theConstraints = Lists.newArrayList(constraints);
        Collections.shuffle(theConstraints);
        progress.setWorkRemaining(theConstraints.size() + 1);
        for(IConstraint constraint : theConstraints) {
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

    private Set<IConstraint> finish(IMessageInfo messageInfo) throws InterruptedException {
        Set<IConstraint> unsolved = Sets.newHashSet();
        for(SolverComponent<?> component : components) {
            cancel.throwIfCancelled();
            component.getTimer().start();
            try {
                unsolved.addAll(component.finish(messageInfo));
            } finally {
                component.getTimer().stop();
            }
        }
        return unsolved.stream().map(c -> Constraints.find(c, unifier)).collect(Collectors.toSet());
    }

    // --- static interface to solver ---

    public static PartialSolution solveIncremental(SolverConfig config, Iterable<ITerm> interfaceTerms,
            Function1<String, ITermVar> fresh, Collection<IConstraint> constraints, IMessageInfo messageInfo,
            IProgress progress, ICancel cancel, NaBL2DebugConfig debugConfig)
            throws SolverException, InterruptedException {
        final int n0 = constraints.size();
        long t0 = System.nanoTime();
        if(debugConfig.resolution()) {
            logger.info("Incremental solving {} constraints.", n0);
        }

        Solver solver = new Solver(config, fresh, SolverMode.PARTIAL, progress, cancel, debugConfig);
        for(ITerm activeTerm : interfaceTerms) {
            solver.unifier.addActive(activeTerm, ImmutableCTrue.of(messageInfo));
            solver.namebindingSolver.addActive(M.collecttd(Scope.matcher()).apply(activeTerm));
        }

        Set<IConstraint> unsolved = Sets.newHashSet();
        try {
            solver.addAll(constraints);
            solver.iterate();
            Iterables.addAll(unsolved, solver.finish(messageInfo));
        } catch(RuntimeException ex) {
            throw new SolverException("Internal solver error.", ex);
        }

        final int n1 = unsolved.size();
        long dt = System.nanoTime() - t0;
        if(debugConfig.resolution() || debugConfig.timing()) {
            logger.info("Reduced {} to {} constraints in {}s.", n0, n1, (Duration.ofNanos(dt).toMillis() / 1000.0));
        }

        return ImmutablePartialSolution.of(
            // @formatter:off
            config,
            interfaceTerms,
            constraints,
            solver.unifier,
            solver.messages
            // @formatter:on
        );
    }

    public static Solution solveFinal(SolverConfig config, Function1<String, ITermVar> fresh,
            Collection<IConstraint> constraints, Collection<PartialSolution> partialSolutions, IMessageInfo messageInfo,
            IProgress progress, ICancel cancel, NaBL2DebugConfig debugConfig)
            throws SolverException, InterruptedException {
        final int n = constraints.size() + partialSolutions.stream().map(PartialSolution::getResidualConstraints)
                .map(Collection::size).reduce(1, (i, j) -> i + j);
        long t0 = System.nanoTime();
        if(debugConfig.resolution()) {
            logger.info("Solving {} constraints.", n, partialSolutions.size());
        }

        Solver solver = new Solver(config, fresh, SolverMode.TOTAL, progress, cancel, debugConfig);
        List<IConstraint> unsolved = Lists.newArrayList();
        try {
            solver.addAll(partialSolutions, messageInfo);
            solver.addAll(constraints);
            solver.iterate();
            Iterables.addAll(unsolved, solver.finish(messageInfo));
        } catch(RuntimeException ex) {
            throw new SolverException("Internal solver error.", ex);
        }

        long dt = System.nanoTime() - t0;
        if(debugConfig.resolution() || debugConfig.timing()) {
            logger.info("Solved {} constraints in {}s.", n, (Duration.ofNanos(dt).toMillis() / 1000.0));
            logger.info(" * namebinding : {}s",
                    (Duration.ofNanos(solver.namebindingSolver.getTimer().total()).toMillis() / 1000.0));
            logger.info(" * relations   : {}s",
                    (Duration.ofNanos(solver.relationSolver.getTimer().total()).toMillis() / 1000.0));
            logger.info(" * unsolved    : {}", unsolved.size());
        }

        return ImmutableSolution.of(
            // @formatter:off
            config,
            solver.astSolver.getProperties(),
            solver.namebindingSolver.getScopeGraph(),
            solver.namebindingSolver.getNameResolution(),
            solver.namebindingSolver.getProperties(),
            solver.relationSolver.getRelations(),
            solver.unifier,
            solver.symbolicSolver.get(),
            solver.messages,
            unsolved
            // @formatter:on
        );
    }

    public static Set<IMessageInfo> unsolvedErrors(Collection<IConstraint> constraints) {
        return constraints.stream().map(c -> {
            IMessageContent content = MessageContent.builder().append("Unsolved: ").append(c.pp()).build();
            return c.getMessageInfo().withDefaultContent(content);
        }).collect(Collectors.toSet());
    }

}
