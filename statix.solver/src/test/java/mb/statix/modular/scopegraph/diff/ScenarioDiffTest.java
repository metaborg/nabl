package mb.statix.modular.scopegraph.diff;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.statix.modular.util.test.TestUtil.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.HashSet;
import java.util.Map.Entry;

import org.junit.Before;
import org.junit.Test;

import mb.nabl2.terms.ITerm;
import mb.statix.modular.incremental.changeset.IChangeSet;
import mb.statix.modular.incremental.strategy.NonIncrementalStrategy;
import mb.statix.modular.module.IModule;
import mb.statix.modular.module.Module;
import mb.statix.modular.scopegraph.IMInternalScopeGraph;
import mb.statix.modular.solver.Context;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.spec.Spec;

/**
 * Tests for diffs which contain full fledged scenarios
 */
public class ScenarioDiffTest {
    private IModule global;
    private ITerm edgeLabel;
    private ITerm dataLabel;
    private ITerm noDataLabel;
    private Scope globalScope;
    private Context oldContext;
    private Context newContext;
    
    @Before
    public void setup() {
        edgeLabel = B.newString("E");
        dataLabel = B.newString("D");
        noDataLabel = B.newString("$");
        
        Spec spec = createSpec(noDataLabel, edgeLabel, dataLabel);
        oldContext = Context.initialContext(new NonIncrementalStrategy(), spec);
        global = Module.topLevelModule("G");
        globalScope = global.getCurrentState().freshScope("global", null);
        
        //Create a new context as well, which also has the same global module
        IChangeSet changeSet = mock(IChangeSet.class);
        newContext = Context.testContext(new NonIncrementalStrategy(), spec, oldContext, changeSet);
        Module.topLevelModule("G");
        global.getCurrentState().freshScope("global", null);
        
        //We want to start each test in the old context.
        Context.setContext(oldContext);
    }
    
    @Test
    public void testDirectEffectiveEquivalence() {
        scenario1();
        
        DiffResult result = new DiffResult();
        Diff.diff(result, global.getId(), newContext, oldContext, true, false);
        DiffResult eResult = result.toEffectiveDiff();
        
        DiffResult eResult2 = new DiffResult();
        Diff.effectiveDiff(eResult2, new HashSet<>(), global.getId(), newContext, oldContext, true, false);
        result.print(System.out);
        System.out.println();
        eResult.print(System.out);
        System.out.println();
        eResult2.print(System.out);
        
        //Verify that both are the same
        compareDiffResults(eResult, eResult2);
    }

    private void compareDiffResults(DiffResult eResult, DiffResult eResult2) {
        for (Entry<String, IScopeGraphDiff<Scope, ITerm, ITerm>> entry : eResult.getDiffs().entrySet()) {
            final String module = entry.getKey();
            
            IScopeGraphDiff<Scope, ITerm, ITerm> diff1 = entry.getValue();
            IScopeGraphDiff<Scope, ITerm, ITerm> diff2 = eResult2.getDiffs().get(module);
            
            assertEquals("Added edges are different for " + module,
                    diff1.getAddedEdges()._getForwardMap(),
                    diff2.getAddedEdges()._getForwardMap());
            assertEquals("Removed edges are different for " + module,
                    diff1.getRemovedEdges()._getForwardMap(),
                    diff2.getRemovedEdges()._getForwardMap());
            assertEquals("Added data is different for " + module,
                    diff1.getAddedData()._getForwardMap(),
                    diff2.getAddedData()._getForwardMap());
            assertEquals("Removed data is different for " + module,
                    diff1.getRemovedData()._getForwardMap(),
                    diff2.getRemovedData()._getForwardMap());
            assertEquals("Added data names are different for " + module,
                    diff1.getAddedDataNames()._getForwardMap(),
                    diff2.getAddedDataNames()._getForwardMap());
            assertEquals("Removed data names are different for " + module,
                    diff1.getRemovedDataNames()._getForwardMap(),
                    diff2.getRemovedDataNames()._getForwardMap());
        }
    }

    private void scenario1() {
        IModule file, A, Am, B, C, Cm;
        {
            file = createChild(global, "file", globalScope);
            
            //Module A represents a Class with a single method m. m contains a variable x.
            A = createChild(file, "A", globalScope);
            IMInternalScopeGraph<Scope, ITerm, ITerm> sgA = oldContext.getScopeGraph(A);
            Scope sA = sgA.createScopeWithIdentity("classScope");
            sgA.addEdge(sA, edgeLabel, globalScope);
            sgA.addDatum(globalScope, dataLabel, createData("Class", "A", sA));
            
            Am = createChild(A, "m", sA);
            IMInternalScopeGraph<Scope, ITerm, ITerm> sgAm = oldContext.getScopeGraph(Am);
            Scope sAm = sgAm.createScopeWithIdentity("methodScope");
            sgAm.addEdge(sAm, edgeLabel, sA);
            sgAm.addDatum(sA, dataLabel, occurence("Method", "m"));
            sgAm.addDatum(sAm, dataLabel, occurence("Var", "x"));
            
            //Module B represents a Class with no methods.
            B = createChild(global, "B", globalScope);
            IMInternalScopeGraph<Scope, ITerm, ITerm> sgB = oldContext.getScopeGraph(B);
            Scope sB = sgB.createScopeWithIdentity("classScope");
            sgB.addEdge(sB, edgeLabel, globalScope);
            sgB.addDatum(globalScope, dataLabel, createData("Class", "B", sB));
        }
        
        //After the incremental analysis, A%m has been removed and A%n has been added.
        Context.setContext(newContext);
        {
            file = createChild(global, "file", globalScope);
            
            //Module A represents a Class with no methods.
            C = createChild(file, "C", globalScope);
            IMInternalScopeGraph<Scope, ITerm, ITerm> sgC = newContext.getScopeGraph(C);
            Scope sC = sgC.createScopeWithIdentity("classScope");
            sgC.addEdge(sC, edgeLabel, globalScope);
            sgC.addDatum(globalScope, dataLabel, createData("Class", "C", sC));
            
            Cm = createChild(C, "m", sC);
            IMInternalScopeGraph<Scope, ITerm, ITerm> sgCm = newContext.getScopeGraph(Cm);
            Scope sCm = sgCm.createScopeWithIdentity("methodScope");
            sgCm.addEdge(sCm, edgeLabel, sC);
            sgCm.addDatum(sC, dataLabel, occurence("Method", "m"));
            sgCm.addDatum(sCm, dataLabel, occurence("Var", "x"));
            
            //Module B represents a Class with no methods.
            B = createChild(global, "B", globalScope);
            IMInternalScopeGraph<Scope, ITerm, ITerm> sgB = newContext.getScopeGraph(B);
            Scope sB = sgB.createScopeWithIdentity("classScope");
            sgB.addEdge(sB, edgeLabel, globalScope);
            sgB.addDatum(globalScope, dataLabel, createData("Class", "B", sB));
        }
    }
}
