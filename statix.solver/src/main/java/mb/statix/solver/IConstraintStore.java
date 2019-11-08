package mb.statix.solver;

import java.util.Map;
import java.util.Map.Entry;

import mb.nabl2.terms.ITermVar;
import mb.statix.scopegraph.reference.CriticalEdge;
import mb.statix.solver.log.IDebugContext;

public interface IConstraintStore {

    int activeSize();

    int delayedSize();

    void add(IConstraint constraint);

    default void addAll(Iterable<? extends IConstraint> constraints) {
        for(IConstraint constraint : constraints) {
            add(constraint);
        }
    }

    IConstraint remove();

    void delay(IConstraint constraint, Delay delay);

    default void delayAll(Iterable<? extends Entry<IConstraint, Delay>> delays) {
        delays.forEach(e -> delay(e.getKey(), e.getValue()));
    }

    Map<IConstraint, Delay> delayed();

    void activateFromVars(Iterable<? extends ITermVar> vars, IDebugContext debug);

    void activateFromEdges(Iterable<? extends CriticalEdge> edges, IDebugContext debug);

}