package org.metaborg.meta.nabl2.solver;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.base.ImmutableCFalse;
import org.metaborg.meta.nabl2.constraints.messages.IMessageContent;
import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.constraints.messages.MessageContent;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.unification.Unifier;
import org.metaborg.meta.nabl2.util.functions.Function1;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class Solver {

    private static final ILogger logger = LoggerUtils.logger(Solver.class);

    private final Map<Class<?>, ISolverComponent<?>> components;
    private final Set<IConstraint> unsolved;
    private final List<IMessageInfo> messages;

    private Solver(SolverConfig config, Function1<String, ITermVar> fresh, ISolverComponent<?>... components) {
        this.components = Maps.newHashMap();
        this.unsolved = Sets.newHashSet();
        for(ISolverComponent<?> component : components) {
            addComponent(component);
        }
        this.messages = Lists.newArrayList();
    }

    private void addComponent(ISolverComponent<?> component) {
        components.put(component.getConstraintClass(), component);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" }) private Optional<ISolverComponent<IConstraint>>
        findComponent(Class<? extends IConstraint> constraintClass) {
        ISolverComponent component;
        if((component = components.get(constraintClass)) == null) {
            for(Entry<Class<?>, ISolverComponent<?>> entry : components.entrySet()) {
                if(entry.getKey().isAssignableFrom(constraintClass)) {
                    component = entry.getValue();
                    break;
                }
            }
        }
        return Optional.ofNullable(component);
    }

    private void add(Iterable<IConstraint> constraints) throws InterruptedException {
        for(IConstraint constraint : constraints) {
            if(Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }
            Optional<ISolverComponent<IConstraint>> maybeComponent = findComponent(constraint.getClass());
            if(maybeComponent.isPresent()) {
                ISolverComponent<IConstraint> component = maybeComponent.get();
                component.getTimer().start();
                try {
                    component.add(constraint);
                } catch(UnsatisfiableException e) {
                    messages.addAll(e.getMessages());
                } finally {
                    component.getTimer().stop();
                }
            } else {
                unsolved.add(constraint);
            }
        }
    }

    private void iterate() throws InterruptedException {
        boolean progress;
        do {
            progress = false;
            for(ISolverComponent<?> component : components.values()) {
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

    private void finishIncremental() throws InterruptedException {
        finishComponents();
        messages.stream().forEach(messageInfo -> {
            unsolved.add(ImmutableCFalse.of(messageInfo));
        });
    }

    private void finishFinal() throws InterruptedException {
        finishComponents();
        unsolved.stream().forEach(c -> {
            IMessageContent content = MessageContent.builder().append("Unsolved: ").append(c.pp()).build();
            messages.add(c.getMessageInfo().withDefault(content));
        });
    }

    private void finishComponents() throws InterruptedException {
        for(ISolverComponent<?> component : components.values()) {
            if(Thread.interrupted()) {
                throw new InterruptedException();
            }
            component.getTimer().start();
            try {
                unsolved.addAll(Lists.newArrayList(component.finish()));
            } finally {
                component.getTimer().stop();
            }
        }
    }
    
    public static Iterable<IConstraint> solveIncremental(SolverConfig config, IncrementalSolverConfig incrementalConfig,
        Function1<String, ITermVar> fresh, Iterable<IConstraint> constraints)
        throws UnsatisfiableException, InterruptedException {
        final int n0 = Iterables.size(constraints);
        long t0 = System.nanoTime();
        logger.info(">>> Reducing {} constraints <<<", n0);

        final Unifier unifier = new Unifier();
        incrementalConfig.getActiveVars().stream().forEach(unifier::addActive);

        final BaseSolver baseSolver;
        final EqualitySolver equalitySolver;
        final AstSolver astSolver;
        final SymbolicSolver symbolicSolver;
        final PolymorphismSolver polymorphismSolver;
        Solver solver = new Solver(config, fresh,
            // @formatter:off
            baseSolver = new BaseSolver(),
            equalitySolver = new EqualitySolver(unifier),
            astSolver = new AstSolver(unifier),
            symbolicSolver = new SymbolicSolver(unifier),
            polymorphismSolver = new PolymorphismSolver(unifier, fresh)
            // @formatter:on
        );

        solver.add(constraints);
        solver.iterate();
        solver.finishIncremental();

        List<IConstraint> result = Lists.newArrayList();
        result.addAll(baseSolver.getNormalizedConstraints(incrementalConfig.getMessageInfo()));
        result.addAll(equalitySolver.getNormalizedConstraints(incrementalConfig.getMessageInfo()));
        result.addAll(astSolver.getNormalizedConstraints(incrementalConfig.getMessageInfo()));
        result.addAll(symbolicSolver.getNormalizedConstraints(incrementalConfig.getMessageInfo()));
        result.addAll(polymorphismSolver.getNormalizedConstraints(incrementalConfig.getMessageInfo()));
        result.addAll(solver.unsolved);
        final int n1 = result.size();

        long dt = System.nanoTime() - t0;
        logger.info(">>> Reduced {} to {} constraints in {} seconds <<<", n0, n1,
            (Duration.ofNanos(dt).toMillis() / 1000.0));

        return result;
    }

    public static Solution solveFinal(SolverConfig config, Function1<String, ITermVar> fresh,
        Iterable<IConstraint> constraints) throws UnsatisfiableException, InterruptedException {
        final int n = Iterables.size(constraints);
        long t0 = System.nanoTime();
        logger.info(">>> Solving {} constraints <<<", n);

        final Unifier unifier = new Unifier();
        final AstSolver astSolver;
        final NamebindingSolver namebindingSolver;
        final RelationSolver relationSolver;
        final SymbolicSolver symbolicSolver;
        Solver solver = new Solver(config, fresh,
            // @formatter:off
            new BaseSolver(),
            new EqualitySolver(unifier),
            astSolver = new AstSolver(unifier),
            namebindingSolver = new NamebindingSolver(config.getResolutionParams(), unifier),
            relationSolver = new RelationSolver(config.getRelations(), config.getFunctions(), unifier),
            new SetSolver(namebindingSolver.nameSets(), unifier),
            symbolicSolver = new SymbolicSolver(unifier),
            new PolymorphismSolver(unifier, fresh)
            // @formatter:on
        );

        solver.add(constraints);
        solver.iterate();
        solver.finishFinal();

        long dt = System.nanoTime() - t0;
        logger.info(">>> Solved {} constraints in {} seconds <<<", n, (Duration.ofNanos(dt).toMillis() / 1000.0));
        logger.info("    * namebinding : {} seconds <<<",
            (Duration.ofNanos(namebindingSolver.getTimer().total()).toMillis() / 1000.0));
        logger.info("    * relations   : {} seconds <<<",
            (Duration.ofNanos(relationSolver.getTimer().total()).toMillis() / 1000.0));

        return ImmutableSolution.of(
            // @formatter:off
            astSolver.getProperties(),
            namebindingSolver.getScopeGraph(),
            namebindingSolver.getNameResolution(),
            namebindingSolver.getProperties(),
            relationSolver.getRelations(),
            unifier,
            symbolicSolver.get(),
            solver.messages
            // @formatter:on
        );
    }

}