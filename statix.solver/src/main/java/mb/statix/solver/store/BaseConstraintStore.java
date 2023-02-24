package mb.statix.solver.store;

import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.log.Level;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.SetMultimap;
import mb.nabl2.terms.ITermVar;
import mb.statix.solver.CriticalEdge;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IConstraintStore;
import mb.statix.solver.log.IDebugContext;

public class BaseConstraintStore implements IConstraintStore {

    final IDebugContext debug;
    private final Deque<IConstraint> active;
    private final SetMultimap.Transient<ITermVar, Delayed> stuckOnVar;
    private final SetMultimap.Transient<CriticalEdge, Delayed> stuckOnEdge;

    public BaseConstraintStore(IDebugContext debug) {
        this.debug = debug;
        this.active = new ConcurrentLinkedDeque<>();
        this.stuckOnVar = SetMultimap.Transient.of();
        this.stuckOnEdge = SetMultimap.Transient.of();
    }

    @Override public int activeSize() {
        return active.size();
    }

    @Override public int delayedSize() {
        return stuckOnVar.size() + stuckOnEdge.size();
    }

    @Override public void add(IConstraint constraint) {
        active.push(constraint);
    }

    @Override public IConstraint remove() throws NoSuchElementException {
        return active.poll();
    }

    @Override public void delay(IConstraint constraint, Delay delay) {
        final Delayed delayed = new Delayed(constraint);
        if(!delay.vars().isEmpty()) {
            if(debug.isEnabled(Level.Debug)) {
                debug.debug("delayed {} on vars {}", constraint, delay.vars());
            }
            for(ITermVar var : delay.vars()) {
                stuckOnVar.__insert(var, delayed);
            }
        } else if(!delay.criticalEdges().isEmpty()) {
            if(debug.isEnabled(Level.Debug)) {
                debug.debug("delayed {} on critical edges {}", constraint, delay.criticalEdges());
            }
            for(CriticalEdge edge : delay.criticalEdges()) {
                stuckOnEdge.__insert(edge, delayed);
            }
        } else {
            throw new IllegalArgumentException("delayed for no apparent reason");
        }
    }

    @Override public void activateFromVars(Iterable<? extends ITermVar> vars, IDebugContext debug) {
        for(ITermVar var : vars) {
            final Collection<Delayed> activated = CapsuleUtil.removeAll(stuckOnVar, var);
            for(Delayed delayed : activated) {
                if(delayed.activate()) {
                    final IConstraint constraint = delayed.constraint;
                    if(debug.isEnabled(Level.Debug)) {
                        debug.debug("activating {}", constraint);
                    }
                    add(constraint);
                }
            }
        }
    }

    @Override public void activateFromEdges(Iterable<? extends CriticalEdge> edges, IDebugContext debug) {
        for(CriticalEdge edge : edges) {
            final Collection<Delayed> activated = CapsuleUtil.removeAll(stuckOnEdge, edge);
            for(Delayed delayed : activated) {
                if(delayed.activate()) {
                    final IConstraint constraint = delayed.constraint;
                    if(debug.isEnabled(Level.Debug)) {
                        debug.debug("activating {}", constraint);
                    }
                    add(constraint);
                }
            }
        }
    }

    @Override public Map.Immutable<IConstraint, Delay> delayed() {
        final SetMultimap.Transient<IConstraint, ITermVar> varStuck = SetMultimap.Transient.of();
        stuckOnVar.entrySet().stream().filter(e -> !e.getValue().activated)
                .forEach(e -> varStuck.__insert(e.getValue().constraint, e.getKey()));

        final SetMultimap.Transient<IConstraint, CriticalEdge> edgeStuck = SetMultimap.Transient.of();
        stuckOnEdge.entrySet().stream().filter(e -> !e.getValue().activated)
                .forEach(e -> edgeStuck.__insert(e.getValue().constraint, e.getKey()));

        final Set<IConstraint> stuck = new HashSet<>();
        stuck.addAll(varStuck.keySet());
        stuck.addAll(edgeStuck.keySet());

        final Map.Transient<IConstraint, Delay> delayed = CapsuleUtil.transientMap();
        stuck.stream().forEach(c -> delayed.__put(c, new Delay(varStuck.get(c), edgeStuck.get(c))));
        return delayed.freeze();
    }

    @Override public Collection<IConstraint> active() {
        return Collections.unmodifiableCollection(active);
    }

    private static class Delayed {

        public final IConstraint constraint;

        private boolean activated = false;

        public Delayed(IConstraint constraint) {
            this.constraint = constraint;
        }

        public boolean activate() {
            if(activated) {
                return false;
            } else {
                activated = true;
                return true;
            }
        }

        @Override public String toString() {
            return activated ? "*" : constraint.toString();
        }

    }

}