package mb.statix.solver.concurrent;

public interface ITypeChecker<S, L, D> {

    void run(S root) throws InterruptedException;

}