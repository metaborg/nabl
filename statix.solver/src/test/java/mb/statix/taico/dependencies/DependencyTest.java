package mb.statix.taico.dependencies;

import static mb.statix.taico.util.test.TestUtil.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import mb.statix.constraints.CFalse;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.IConstraint;
import mb.statix.spec.Spec;
import mb.statix.taico.incremental.changeset.BaselineChangeSet;
import mb.statix.taico.incremental.changeset.IChangeSet;
import mb.statix.taico.incremental.strategy.IncrementalStrategy;
import mb.statix.taico.module.IModule;
import mb.statix.taico.module.Module;
import mb.statix.taico.module.ModulePaths;
import mb.statix.taico.solver.Context;
import mb.statix.taico.solver.coordinator.ISolverCoordinator;
import mb.statix.taico.solver.state.IMState;

public class DependencyTest {
    protected Context oldContext;
    protected Context context;
    protected IModule global;
    protected Scope globalScope;

    @Before
    public void setUp() throws Exception {
        //Create a context
        Spec spec = createSpec();
        oldContext = Context.initialContext(IncrementalStrategy.of("combined"), spec);
        
        //Create the global module
        global = Module.topLevelModule("global");
        globalScope = global.getCurrentState().freshScope("s", null);
        
        //Set the coordinator
        oldContext.setCoordinator(mockCoordinator(global));
    }
    
    private ISolverCoordinator mockCoordinator(IModule root) {
        ISolverCoordinator coordinator = mock(ISolverCoordinator.class);
        when(coordinator.getRootModule()).thenReturn(root);
        return coordinator;
    }
    
    /**
     * @param changed
     *      the modules that have changed
     * 
     * @return
     *      an incremental context with the given changed modules
     */
    protected Context incremental(Collection<String> changed) {
        IMState previousRootState = global.getCurrentState();
        IChangeSet changeSet = new BaselineChangeSet(oldContext, empty(), changed, empty());
        
        Map<String, IConstraint> initConstraints = new HashMap<>();
        for (IModule module : oldContext.getModules()) {
            //Skip the root module
            if (!ModulePaths.containsPathSeparator(module.getId())) continue;
            
            initConstraints.put(module.getId(), new CFalse());
        }
        IncrementalStrategy strategy = IncrementalStrategy.of("combined");
        Context context = Context.incrementalContext(strategy, oldContext, previousRootState, changeSet, initConstraints, oldContext.getSpec());
        context.setCoordinator(mockCoordinator(global));
        
        //This step is normally done in the coordinator. It initializes the correct modules and removes or transfers dependencies
        strategy.createInitialModules(context, changeSet, context.getInitialConstraints());
        return context;
    }
    
    @Test
    public void testDependencyTransfer() {
        //B depends on A
        IModule A = createChild(global, "A", globalScope);
        IModule B = createChild(global, "B", globalScope);
        
        Dependencies doA = oldContext.getDependencies(A);
        Dependencies doB = oldContext.getDependencies(B);
        doB.addDependency(A);
        assertTrue(doA.getModuleDependantIds().contains(B.getId()));
        
        //After transfer with no changes, this should still hold
        context = incremental(empty());
        NameDependencies dnA = context.getDependencies(A);
        assertTrue(dnA.getModuleDependantIds().contains(B.getId()));
    }

    @Test
    public void testDependencyNonTransfer() {
        //B depends on A
        IModule A = createChild(global, "A", globalScope);
        IModule B = createChild(global, "B", globalScope);
        
        Dependencies doB = oldContext.getDependencies(B);
        doB.addDependency(A);
        
        Dependencies doA = oldContext.getDependencies(A);
        assertTrue(doA.getModuleDependantIds().contains(B.getId()));
        
        //After transfer with changes to A, this should still hold
        context = incremental(list("A"));
        NameDependencies dnA = context.getDependencies(A);
        assertTrue(dnA.getModuleDependantIds().isEmpty());
        assertSame(doA, context.getOldDependencies(A.getId()));
    }
}
