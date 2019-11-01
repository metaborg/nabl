package mb.statix.modular.scopegraph.diff;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.statix.modular.util.test.TestUtil.*;
import static org.junit.Assert.*;

import java.util.Set;

import org.junit.Test;

import mb.nabl2.terms.ITerm;
import mb.nabl2.util.collections.IRelation3;
import mb.statix.scopegraph.terms.Scope;

public class DiffTest {
    @Test
    public void testEffectiveDiff() {
        DiffResult result = new DiffResult();
        ScopeGraphDiff diff = ScopeGraphDiff.empty();
        
        //Create a scope in A
        Scope aScope = Scope.of("A", "s-a");
        diff.addedScopes.add(aScope);
        
        //The globalscope already existed
        Scope globalScope = Scope.of("Global", "s");
        ITerm label = B.newString("l");
        ITerm data = B.newString("A");
        
        //Add some data
        diff.addedData.put(globalScope, label, data);
        result.addDiff("A", diff);
        
        //The effective diff should now include an entry for the global module
        DiffResult effective = result.toEffectiveDiff();
        assertTrue(effective.getDiffs().containsKey("Global"));
        
        //The data should have been moved
        IScopeGraphDiff<Scope, ITerm, ITerm> ediff = effective.getDiffs().get("Global");
        assertTrue(ediff.getAddedData().contains(globalScope, label, data));
        
        //Check that the original remains and still has the scope but not the data
        assertTrue(effective.getDiffs().containsKey("A"));
        assertTrue(effective.getDiffs().get("A").getAddedScopes().contains(aScope));
        assertTrue(effective.getDiffs().get("A").getAddedData().isEmpty());
    }
    
    @Test
    public void testGetNewSets() {
        Set<String> nSet = Diff.getNew(set("A", "B"), set("B", "C"));
        assertEquals(set("C"), nSet);
    }
    
    @Test
    public void testGetNewRelations() {
        IRelation3<Integer, Integer, Integer> nRel = Diff.getNew(relation(0, 0, 0, 1, 1, 1), relation(1, 1, 1, 2, 2, 2));
        assertEquals(1, nRel.keySet().size());
        assertTrue(nRel.contains(2, 2, 2));
    }
}
