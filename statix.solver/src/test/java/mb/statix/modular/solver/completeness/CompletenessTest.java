package mb.statix.modular.solver.completeness;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.statix.modular.util.test.TestUtil.*;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import mb.nabl2.terms.ITerm;
import mb.statix.constraints.CFalse;
import mb.statix.constraints.CTellEdge;
import mb.statix.modular.incremental.changeset.IChangeSet;
import mb.statix.modular.incremental.strategy.NameIncrementalStrategy;
import mb.statix.modular.module.IModule;
import mb.statix.modular.module.Module;
import mb.statix.modular.solver.Context;
import mb.statix.modular.solver.ModuleSolver;
import mb.statix.modular.solver.coordinator.ISolverCoordinator;
import mb.statix.modular.solver.state.IMState;
import mb.statix.modular.util.TDebug;
import mb.statix.scopegraph.reference.CriticalEdge;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.spec.Spec;

public class CompletenessTest {
    protected ITerm noRelationLabel;
    protected ITerm edgeLabel;
    protected ITerm dataLabel;
    protected Spec spec;
    protected Context context;
    protected IModule global;
    protected Scope globalScope;
    
    @Before
    public void setUp() throws Exception {
        noRelationLabel = B.newString("NO");
        edgeLabel = B.newString("EDGE");
        dataLabel = B.newString("DATA");
        spec = createSpec(noRelationLabel, edgeLabel, dataLabel);
        context = Context.initialContext(new NameIncrementalStrategy(), spec);
        
        //Init module
        global = Module.topLevelModule("global");
        globalScope = global.getCurrentState().freshScope("globalScope", null);
        context.setCoordinator(mockCoordinator(global));
        ModuleSolver.topLevelSolver(global.getCurrentState(), null, TDebug.DEV_NULL);
    }
    
    private ISolverCoordinator mockCoordinator(IModule root) {
        ISolverCoordinator coordinator = mock(ISolverCoordinator.class);
        when(coordinator.getRootModule()).thenReturn(root);
        return coordinator;
    }

    /**
     * Tests if the completeness is restored appropriately whenever an incremental run is started.
     */
    @Test
    public void testRestoredOnIncremental() {
        IModule A = createChild(global, "A", globalScope);
        IMState aState = A.getCurrentState();
        context.getSolver(global).childSolver(aState, null, null);
        
        ModuleSolver solver = aState.solver();
        
        Scope aScope = aState.freshScope("s", null);
        
        CTellEdge constraint = new CTellEdge(aScope, edgeLabel, globalScope);
        
        //Critical edge s -EDGE-> globalScope
        solver.getCompleteness().add(constraint, aState.unifier());
        solver.getStore().delay(constraint, Delay.ofCriticalEdge(CriticalEdge.of(aScope, edgeLabel)), aState);
        
        context.addResult(A, solver.finishSolver());
        
        IChangeSet changeSet = mock(IChangeSet.class);
        Map<String, IConstraint> initConstraints = new HashMap<>();
        initConstraints.put(A.getId(), new CFalse());
        Context nContext = Context.incrementalContext(
                new NameIncrementalStrategy(),
                context,
                global.getCurrentState(),
                changeSet,
                initConstraints,
                spec);
        nContext.setCoordinator(mockCoordinator(global));
        
        nContext.getStrategy().createInitialModules(nContext, changeSet, initConstraints);
        
        IMState anState = A.getCurrentState();
        assertFalse(anState.solver().getCompleteness().isComplete(aScope, edgeLabel, anState.unifier()));
    }

}
