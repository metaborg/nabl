package mb.nabl2.solver.solvers;

import java.util.Collection;
import java.util.Deque;
import java.util.List;

import org.metaborg.util.Ref;
import org.metaborg.util.functions.Action1;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;

import com.google.common.collect.Lists;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import mb.nabl2.constraints.IConstraint;
import mb.nabl2.scopegraph.esop.CriticalEdge;
import mb.nabl2.solver.ISolver;
import mb.nabl2.solver.ISolver.SolveResult;
import mb.nabl2.solver.ImmutableSolveResult;
import mb.nabl2.solver.exceptions.CriticalEdgeDelayException;
import mb.nabl2.solver.exceptions.DelayException;
import mb.nabl2.solver.exceptions.InterruptedDelayException;
import mb.nabl2.solver.exceptions.RelationDelayException;
import mb.nabl2.solver.exceptions.UnconditionalDelayExpection;
import mb.nabl2.solver.exceptions.VariableDelayException;
import mb.nabl2.solver.messages.IMessages;
import mb.nabl2.solver.messages.Messages;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.u.IUnifier;
import mb.nabl2.util.collections.IndexedBag;
import mb.nabl2.util.collections.IndexedBag.RemovalPolicy;

public class FixedPointSolver {

    private static final ILogger log = LoggerUtils.logger(FixedPointSolver.class);

    private final PublishSubject<Step> stepSubject;

    private final ICancel cancel;
    private final IProgress progress;

    private final ISolver component;

    public FixedPointSolver(ICancel cancel, IProgress progress, ISolver component) {
        this.cancel = cancel;
        this.progress = progress;
        this.component = component;
        this.stepSubject = PublishSubject.create();
    }

    public SolveResult solve(Iterable<? extends IConstraint> initialConstraints, Ref<IUnifier.Immutable> unifier)
            throws InterruptedException {
        final IMessages.Transient messages = Messages.Transient.of();

        final Deque<IConstraint> constraints = Lists.newLinkedList(initialConstraints);
        final IndexedBag<IConstraint, CriticalEdge> criticalEdgeDelays = new IndexedBag<>(RemovalPolicy.ALL);
        final IndexedBag<IConstraint, String> relationDelays = new IndexedBag<>(RemovalPolicy.ALL);
        final IndexedBag<IConstraint, ITermVar> variableDelays = new IndexedBag<>(RemovalPolicy.ANY);
        final List<IConstraint> unsolved = Lists.newArrayList();

        final Action1<Iterable<CriticalEdge>> resolveCriticalEdges = es -> {
            es.forEach(e -> {
                Collection<IConstraint> newConstraints = criticalEdgeDelays.reindex(e, ce -> Iterables2.empty());
                newConstraints.forEach(constraints::addFirst);
            });
        };
        final Action1<Iterable<String>> resolveRelation = names -> {
            names.forEach(name -> {
                Collection<IConstraint> newConstraints = relationDelays.reindex(name, ce -> Iterables2.empty());
                newConstraints.forEach(constraints::addFirst);
            });
        };

        int solvedCount = 0;
        int criticalEdgeDelayCount = 0;
        int relationDelayCount = 0;
        int unconditionalDelayCount = 0;
        int variableDelayCount = 0;

        boolean progress;
        do {
            progress = false;
            while(!constraints.isEmpty()) {
                cancel.throwIfCancelled();
                final IConstraint constraint = constraints.removeFirst();

                final SolveResult result;
                try {
                    result = component.apply(constraint);
                } catch(InterruptedDelayException e) {
                    throw e.getCause();
                } catch(UnconditionalDelayExpection e) {
                    unsolved.add(constraint);
                    unconditionalDelayCount++;
                    continue;
                } catch(CriticalEdgeDelayException e) {
                    criticalEdgeDelays.add(constraint, e.getCause().criticalEdges());
                    criticalEdgeDelayCount++;
                    continue;
                } catch(VariableDelayException e) {
                    variableDelays.add(constraint, e.variables());
                    variableDelayCount++;
                    continue;
                } catch(RelationDelayException e) {
                    relationDelays.add(constraint, Iterables2.singleton(e.relation()));
                    relationDelayCount++;
                    continue;
                } catch(DelayException e) {
                    throw new IllegalStateException(e);
                }

                result.unifierDiff().varSet().forEach(v -> {
                    constraints.addAll(variableDelays.reindex(v, unifier.get()::getVars));
                });

                messages.addAll(result.messages());

                result.constraints().forEach(constraints::addFirst);

                stepSubject.onNext(new Step(constraint, result, resolveCriticalEdges, resolveRelation));

                this.progress.work(1);
                progress |= true;
                solvedCount++;
            }
        } while(progress);

      //log.info("Solved {} with {} delays, {} var delays, {} critical edge delays, {} relation delays", solvedCount,
      //        unconditionalDelayCount, variableDelayCount, criticalEdgeDelayCount, relationDelayCount);

        unsolved.addAll(constraints);
        unsolved.addAll(variableDelays.values());
        unsolved.addAll(criticalEdgeDelays.values());
        unsolved.addAll(relationDelays.values());

        return ImmutableSolveResult.builder()
        // @formatter:off
                .messages(messages.freeze())
                .constraints(unsolved)
                // @formatter:on
                .build();
    }

    public Observable<Step> step() {
        return stepSubject;
    }

    public class Step {

        public final IConstraint constraint;
        public final SolveResult result;

        private final Action1<Iterable<CriticalEdge>> resolveCriticalEdges;
        private final Action1<Iterable<String>> resolveRelations;

        private Step(IConstraint constraint, SolveResult result, Action1<Iterable<CriticalEdge>> release,
                Action1<Iterable<String>> resolveRelations) {
            this.constraint = constraint;
            this.result = result;
            this.resolveCriticalEdges = release;
            this.resolveRelations = resolveRelations;
        }

        public void resolveCriticalEdges(Iterable<CriticalEdge> criticalEdges) {
            resolveCriticalEdges.apply(criticalEdges);
        }

        public void resolveRelations(Iterable<String> names) {
            resolveRelations.apply(names);
        }

    }

}
