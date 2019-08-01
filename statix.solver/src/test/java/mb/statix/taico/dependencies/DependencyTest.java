package mb.statix.taico.dependencies;

import static org.junit.Assert.*;
import static mb.statix.taico.util.test.TestUtil.*;
import static org.mockito.Mockito.mock;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import mb.statix.constraints.CResolveQuery;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.IConstraint;
import mb.statix.spec.Spec;
import mb.statix.taico.incremental.changeset.BaselineChangeSet;
import mb.statix.taico.incremental.changeset.IChangeSet;
import mb.statix.taico.incremental.strategy.IncrementalStrategy;
import mb.statix.taico.module.IModule;
import mb.statix.taico.module.Module;
import mb.statix.taico.solver.SolverContext;
import mb.statix.taico.solver.state.IMState;

public class DependencyTest {
    protected SolverContext oldContext;
    protected SolverContext context;
    protected IModule global;
    protected Scope globalScope;

    @Before
    public void setUp() throws Exception {
        //Create a context
        Spec spec = createSpec();
        oldContext = SolverContext.initialContext(IncrementalStrategy.of("combined"), spec);
        
        //Create the global module
        global = Module.topLevelModule("global");
        globalScope = global.getCurrentState().freshScope("s", null);
    }
    
    /**
     * @param changed
     *      the modules that have changed
     * 
     * @return
     *      an incremental context with the given changed modules
     */
    protected SolverContext incremental(Collection<String> changed) {
        IMState previousRootState = global.getCurrentState();
        IChangeSet changeSet = new BaselineChangeSet(oldContext, empty(), changed, empty());
        
        Map<String, IConstraint> initConstraints = new HashMap<>();
//        if (changed.contains(global.getName())) {
//            initConstraints.put(global.getName(), global.getInitialization());
//        }
        return SolverContext.incrementalContext(IncrementalStrategy.of("combined"), oldContext, previousRootState, changeSet, initConstraints, oldContext.getSpec());
    }
    
    @Test
    public void testDependencyTransfer() {
        //B depends on A
        IModule A = createChild(global, "A", globalScope);
        IModule B = createChild(global, "B", globalScope);
        
        NameDependencies doA = oldContext.getDependencies(A);
        doA.addDependant(B.getId(), mock(CResolveQuery.class));
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
        
        NameDependencies doA = oldContext.getDependencies(A);
        doA.addDependant(B.getId(), mock(CResolveQuery.class));
        assertTrue(doA.getModuleDependantIds().contains(B.getId()));
        
        //After transfer with changes to A, this should still hold
        context = incremental(list("A"));
        NameDependencies dnA = context.getDependencies(A);
        assertTrue(dnA.getModuleDependantIds().isEmpty());
        assertSame(doA, context.getOldDependencies(A.getId()));
    }
}
