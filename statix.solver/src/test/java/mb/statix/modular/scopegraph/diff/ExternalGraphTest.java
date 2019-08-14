package mb.statix.modular.scopegraph.diff;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.statix.modular.util.test.TestUtil.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import mb.nabl2.terms.ITerm;
import mb.statix.modular.incremental.strategy.NonIncrementalStrategy;
import mb.statix.modular.module.IModule;
import mb.statix.modular.module.Module;
import mb.statix.modular.scopegraph.ModuleScopeGraph;
import mb.statix.modular.solver.Context;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.spec.Spec;

public class ExternalGraphTest {
    private IModule global;
    private ITerm edgeLabel;
    private ITerm dataLabel;
    private ITerm noDataLabel;
    private Scope globalScope;
    
    @Before
    public void setup() {
        edgeLabel = B.newString("E");
        dataLabel = B.newString("D");
        noDataLabel = B.newString("$");
        
        Spec spec = createSpec(noDataLabel, edgeLabel, dataLabel);
        Context.initialContext(new NonIncrementalStrategy(), spec);
        
        global = Module.topLevelModule("G");
        globalScope = global.getCurrentState().freshScope("global", null);
    }
    
    @Test
    public void testExternalGraphNotReachable() {
        IModule A = createChild(global, "A", globalScope);
        ModuleScopeGraph msg = (ModuleScopeGraph) A.getCurrentState().scopeGraph();
        Scope AScope = msg.createScopeWithIdentity("s");
        Scope mScope = msg.createScopeWithIdentity("m");
        
        msg.addEdge(AScope, edgeLabel, globalScope);
        msg.addEdge(mScope, edgeLabel, AScope);
        
        ModuleScopeGraph extGraph = msg.externalGraph();
        assertTrue(extGraph.getScopes().isEmpty());
        assertTrue(extGraph.getOwnEdges().isEmpty());
        assertTrue(extGraph.getOwnData().isEmpty());
    }
    
    @Test
    public void testExternalGraphSemiReachable() {
        IModule A = createChild(global, "A", globalScope);
        ModuleScopeGraph msg = (ModuleScopeGraph) A.getCurrentState().scopeGraph();
        Scope AScope = msg.createScopeWithIdentity("s");
        Scope mScope = msg.createScopeWithIdentity("m");
        
        msg.addEdge(AScope, edgeLabel, globalScope);
        msg.addEdge(mScope, edgeLabel, AScope);
        msg.addDatum(mScope, dataLabel, occurence("Var", "x"));
        msg.addDatum(globalScope, dataLabel, createData("Class", "A", AScope));
        
        //mScope should not appear in the result, since it is not reachable
        ModuleScopeGraph extGraph = msg.externalGraph();
        assertEquals(extGraph.getScopes(), set(AScope));
        assertTrue(extGraph.getOwnEdges().contains(AScope, edgeLabel, globalScope));
        assertTrue(extGraph.getOwnData().contains(globalScope, dataLabel));
    }
}
