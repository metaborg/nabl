package mb.statix.solver;

import java.util.Map;

import mb.nabl2.terms.ITermVar;
import mb.statix.scopegraph.reference.CriticalEdge;
import mb.statix.solver.log.IDebugContext;

public interface IConstraintStore {

    int activeSize();

    int delayedSize();

    void addAll(Iterable<? extends IConstraint> constraints);

    void activateStray();

    void activateFromVars(Iterable<? extends ITermVar> vars, IDebugContext debug);

    void activateFromEdges(Iterable<? extends CriticalEdge> edges, IDebugContext debug);

    Iterable<Entry> active(IDebugContext debug);

    Map<IConstraint, Delay> delayed();

    interface Entry {

        IConstraint constraint();

        void delay(Delay d);

        void remove();

    }

}