package mb.statix.solver.store;

import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.metaborg.util.log.Level;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Multimap;

import mb.nabl2.terms.ITermVar;
import mb.statix.solver.CriticalEdge;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IConstraintStore;
import mb.statix.solver.log.IDebugContext;

public class BaseConstraintStore implements IConstraintStore {

    final IDebugContext debug;
    private final Deque<IConstraint> active;
    private final Multimap<ITermVar, Delayed> stuckOnVar;
    private final Multimap<CriticalEdge, Delayed> stuckOnEdge;

    public BaseConstraintStore(IDebugContext debug) {
        this.debug = debug;
        this.active = new ConcurrentLinkedDeque<>();
        this.stuckOnVar = HashMultimap.create();
        this.stuckOnEdge = HashMultimap.create();
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
                stuckOnVar.put(var, delayed);
            }
        } else if(!delay.criticalEdges().isEmpty()) {
            if(debug.isEnabled(Level.Debug)) {
                debug.debug("delayed {} on critical edges {}", constraint, delay.criticalEdges());
            }
            for(CriticalEdge edge : delay.criticalEdges()) {
                stuckOnEdge.put(edge, delayed);
            }
        } else {
            throw new IllegalArgumentException("delayed for no apparent reason");
        }
    }

    @Override public void activateFromVars(Iterable<? extends ITermVar> vars, IDebugContext debug) {
        for(ITermVar var : vars) {
            final Collection<Delayed> activated = stuckOnVar.removeAll(var);
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
            final Collection<Delayed> activated = stuckOnEdge.removeAll(edge);
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

    @Override public Map<IConstraint, Delay> delayed() {
        final Multimap<IConstraint, ITermVar> varStuck = HashMultimap.create();
        stuckOnVar.entries().stream().filter(e -> !e.getValue().activated)
                .forEach(e -> varStuck.put(e.getValue().constraint, e.getKey()));

        final Multimap<IConstraint, CriticalEdge> edgeStuck = HashMultimap.create();
        stuckOnEdge.entries().stream().filter(e -> !e.getValue().activated)
                .forEach(e -> edgeStuck.put(e.getValue().constraint, e.getKey()));

        final Set<IConstraint> stuck = new HashSet<>();
        stuck.addAll(varStuck.keys());
        stuck.addAll(edgeStuck.keys());

        final Builder<IConstraint, Delay> delayed = ImmutableMap.builder();
        stuck.stream().forEach(c -> delayed.put(c, new Delay(varStuck.get(c), edgeStuck.get(c))));
        return delayed.build();
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