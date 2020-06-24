package mb.statix.solver.concurrent2;

/**
 * Represents the user-implemented type checker for a specific unit.
 */
@FunctionalInterface
public interface ITypeChecker<S, L, D> {

    void run(IUnit<S, L, D> unit) throws InterruptedException;

}