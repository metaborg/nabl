package mb.statix.taico.scopegraph.diff;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import static mb.nabl2.terms.build.TermBuild.B;

import mb.nabl2.terms.ITerm;
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
        ScopeGraphDiff ediff = effective.getDiffs().get("Global");
        assertTrue(ediff.getAddedData().contains(globalScope, label, data));
        
        //Check that the original remains and still has the scope but not the data
        assertTrue(effective.getDiffs().containsKey("A"));
        assertTrue(effective.getDiffs().get("A").getAddedScopes().contains(aScope));
        assertTrue(effective.getDiffs().get("A").getAddedData().isEmpty());
    }

}
