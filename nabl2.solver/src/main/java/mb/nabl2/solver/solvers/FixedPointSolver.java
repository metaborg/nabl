package mb.nabl2.solver.solvers;

import java.util.Deque;
import java.util.List;
import java.util.Set;

import org.metaborg.util.functions.Action1;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import mb.nabl2.constraints.IConstraint;
import mb.nabl2.scopegraph.esop.CriticalEdge;
import mb.nabl2.solver.CriticalEdgeDelayException;
import mb.nabl2.solver.DelayException;
import mb.nabl2.solver.ISolver;
import mb.nabl2.solver.ISolver.SolveResult;
import mb.nabl2.solver.ImmutableSolveResult;
import mb.nabl2.solver.InterruptedDelayException;
import mb.nabl2.solver.UnconditionalDelayExpection;
import mb.nabl2.solver.messages.IMessages;
import mb.nabl2.solver.messages.Messages;
import mb.nabl2.solver.properties.IConstraintSetProperty;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.util.collections.IndexedBag;
import mb.nabl2.util.collections.IndexedBag.RemovalPolicy;
import rx.Observable;
import rx.subjects.PublishSubject;

public class FixedPointSolver {

    private static final ILogger log = LoggerUtils.logger(FixedPointSolver.class);

    private final PublishSubject<Step> stepSubject;

    private final ICancel cancel;
    private final IProgress progress;

    private final ISolver component;
    private final java.util.Set<IConstraintSetProperty> properties;

    public FixedPointSolver(ICancel cancel, IProgress progress, ISolver component,
            Iterable<? extends IConstraintSetProperty> properties) {
        this(cancel, progress, component, Sets.newHashSet(properties));
    }

    public FixedPointSolver(ICancel cancel, IProgress progress, ISolver component,
            Set<IConstraintSetProperty> properties) {
        this.cancel = cancel;
        this.progress = progress;
        this.component = component;
        this.properties = properties;
        this.stepSubject = PublishSubject.create();
    }

    public SolveResult solve(Iterable<? extends IConstraint> initialConstraints) throws InterruptedException {
        propertiesAddAll(initialConstraints);

        final IMessages.Transient messages = Messages.Transient.of();

        final Deque<IConstraint> constraints = Lists.newLinkedList(initialConstraints);
        final IndexedBag<IConstraint, CriticalEdge> edgeDelayedConstraints = new IndexedBag<>(RemovalPolicy.ALL);
        final List<IConstraint> unsolvedConstraints = Lists.newArrayList();
        final List<IConstraint> delayed = Lists.newArrayList();
        int criticalEdgeDelays = 0;
        int unconditionalDelays = 0;
        int delays = 0;
        int solves = 0;
        boolean progress;
        do {
            progress = false;
            while(!constraints.isEmpty()) {
                cancel.throwIfCancelled();
                final IConstraint constraint = constraints.removeFirst();
                propertiesRemove(constraint); // property only on other constraints

                final SolveResult result;
                try {
                    result = component.apply(constraint).orElse(null);
                } catch(InterruptedDelayException e) {
                    throw e.getCause();
                } catch(UnconditionalDelayExpection e) {
                    propertiesAdd(constraint);
                    unsolvedConstraints.add(constraint);
                    unconditionalDelays++;
                    continue;
                } catch(CriticalEdgeDelayException e) {
                    propertiesAdd(constraint);
                    edgeDelayedConstraints.add(constraint, e.getCause().incompletes());
                    criticalEdgeDelays++;
                    continue;
                } catch(DelayException e) {
                    throw new IllegalStateException(e);
                }
                if(result == null) {
                    propertiesAdd(constraint);
                    delayed.add(constraint);
                    delays++;
                    continue;
                }

                updateVars(result.unifierDiff().varSet());

                messages.addAll(result.messages());

                propertiesAddAll(result.constraints());
                result.constraints().forEach(constraints::addFirst);

                stepSubject.onNext(new Step(result, es -> {
                    es.forEach(e -> {
                        constraints.addAll(edgeDelayedConstraints.reindex(e, ce -> Iterables2.empty()));
                    });
                }));

                this.progress.work(1);
                progress |= true;
                solves++;
            }
            constraints.addAll(delayed);
            delayed.clear();
        } while(progress);

        log.info("Solved {}", solves);
        log.info("Delays (regular) {}", delays);
        log.info("Delays (critical edge) {}", criticalEdgeDelays);
        log.info("Delays (unconditional) {}", unconditionalDelays);

        unsolvedConstraints.addAll(constraints);
        unsolvedConstraints.addAll(edgeDelayedConstraints.values());

        return ImmutableSolveResult.builder()
        // @formatter:off
                .messages(messages.freeze())
                .constraints(unsolvedConstraints)
                // @formatter:on
                .build();
    }

    private void propertiesAddAll(Iterable<? extends IConstraint> constraints) {
        for(IConstraintSetProperty property : properties) {
            property.addAll(constraints);
        }
    }

    private void propertiesAdd(IConstraint constraint) {
        for(IConstraintSetProperty property : properties) {
            property.add(constraint);
        }
    }

    private void propertiesRemove(IConstraint constraint) {
        for(IConstraintSetProperty property : properties) {
            property.remove(constraint);
        }
    }

    private void updateVars(Set<ITermVar> vars) {
        for(IConstraintSetProperty property : properties) {
            component.update(vars);
            property.update(vars);
        }
    }

    public Observable<Step> step() {
        return stepSubject;
    }

    public class Step {

        public final SolveResult result;

        private Action1<Iterable<CriticalEdge>> release;

        private Step(SolveResult result, Action1<Iterable<CriticalEdge>> release) {
            this.result = result;
            this.release = release;
        }

        public void release(Iterable<CriticalEdge> es) {
            release.apply(es);
        }

    }

}