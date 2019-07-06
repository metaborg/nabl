package mb.statix.taico.solver.coordinator;

import mb.statix.taico.solver.ModuleSolver;

public class EntailsCoordinator extends SolverCoordinator {
    @Override
    public void addSolver(ModuleSolver solver) {
        throw new IllegalStateException("Cannot add additional solvers to entails coordinator! (Do not hit module boundaries in queries!)");
    }
}
