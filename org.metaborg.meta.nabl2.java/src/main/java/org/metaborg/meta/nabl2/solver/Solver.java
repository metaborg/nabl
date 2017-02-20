package org.metaborg.meta.nabl2.solver;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.metaborg.meta.nabl2.constraints.IConstraint;
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

    private final Unifier unifier;

    private final Map<Class<? extends IConstraint>, ISolverComponent<?>> components;
    private final AstSolver astSolver;
    private final NamebindingSolver namebindingSolver;
    private final RelationSolver relationSolver;
    private final SymbolicSolver symSolver;

    private final Set<IConstraint> unsolved;
    private final List<IMessageInfo> messages;

    private Solver(SolverConfig config, Function1<String, ITermVar> fresh) {
        this.unifier = new Unifier();
        this.components = Maps.newHashMap();
        this.unsolved = Sets.newHashSet();

        addComponent(new BaseSolver());
        addComponent(new EqualitySolver(unifier));
        addComponent(this.astSolver = new AstSolver(unifier));
        addComponent(this.namebindingSolver = new NamebindingSolver(config.getResolutionParams(), unifier));
        addComponent(this.relationSolver = new RelationSolver(config.getRelations(), config.getFunctions(), unifier));
        addComponent(new SetSolver(namebindingSolver.nameSets(), unifier));
        addComponent(this.symSolver = new SymbolicSolver());
        addComponent(new PolymorphismSolver(unifier, fresh));

        this.messages = Lists.newArrayList();
    }

    private void addComponent(ISolverComponent<?> component) {
        components.put(component.getConstraintClass(), component);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" }) private Optional<ISolverComponent<IConstraint>>
        findComponent(Class<? extends IConstraint> constraintClass) {
        ISolverComponent component;
        if((component = components.get(constraintClass)) == null) {
            for(Entry<Class<? extends IConstraint>, ISolverComponent<?>> entry : components.entrySet()) {
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
        outer: while(true) {
            if(Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }
            for(ISolverComponent<?> component : components.values()) {
                component.getTimer().start();
                try {
                    if(component.iterate()) {
                        continue outer;
                    }
                } catch(UnsatisfiableException e) {
                    messages.addAll(e.getMessages());
                } finally {
                    component.getTimer().stop();
                }
            }
            return;
        }
    }

    private void finish(boolean errorsOnUnsolved) throws InterruptedException {
        for(ISolverComponent<?> component : components.values()) {
            if(Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }
            component.getTimer().start();
            try {
                unsolved.addAll(Lists.newArrayList(component.finish()));
            } finally {
                component.getTimer().stop();
            }
        }
        if(errorsOnUnsolved) {
            unsolved.stream().forEach(c -> {
                IMessageContent content = MessageContent.builder().append("Unsolved: ").append(c.pp()).build();
                messages.add(c.getMessageInfo().withDefault(content));
            });
        }
    }

    public static Solution solve(SolverConfig config, Function1<String, ITermVar> fresh,
        Iterable<IConstraint> constraints) throws UnsatisfiableException, InterruptedException {
        final int n = Iterables.size(constraints);
        long t0 = System.nanoTime();
        logger.info(">>> Solving {} constraints <<<", n);
        Solver solver = new Solver(config, fresh);
        solver.add(constraints);
        solver.iterate();
        solver.finish(true);
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
            solver.symSolver.get(),
            solver.messages
            // @formatter:on
        );
    }

}