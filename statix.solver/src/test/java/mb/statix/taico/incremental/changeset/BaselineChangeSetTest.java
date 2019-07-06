package mb.statix.taico.incremental.changeset;

import static mb.statix.taico.module.ModuleCleanliness.*;
import static org.junit.Assert.*;

import org.junit.Test;

import mb.statix.taico.incremental.Flag;
import mb.statix.taico.module.IModule;

public class BaselineChangeSetTest extends IChangeSetTest {

    @Test
    public void testAddedOneFromEmpty() {
        BaselineChangeSet changeSet = new BaselineChangeSet(context, list("A"), empty(), empty());
        assertTrue(changeSet.added().contains("A"));
        assertEquals(global.getTopFlag(), flag(CLIRTY, 1));
    }
    
    /**
     * Global -> [A, B]
     * A is dirty.
     * 
     * --> Only A should be dirty, the rest should be clean.
     */
    @Test
    public void testDirty1() {
        //A and B are children of the root module, but are not passed any scopes.
        IModule a = createChild(global, "A");
        IModule b = createChild(global, "B");
        
        BaselineChangeSet changeSet = new BaselineChangeSet(context, empty(), list("A"), empty());
        
        //Root module is clean because it does not pass any scopes to its child
        assertTrue(changeSet.clean().contains(global));
        assertTrue(changeSet.dirty().contains(a));
        assertTrue(changeSet.clean().contains(b));
        
        assertEquals(global.getTopFlag(), Flag.CLEAN);
        assertEquals(a.getTopFlag(), flag(DIRTY, 1));
        assertEquals(b.getTopFlag(), Flag.CLEAN);
    }
    
    /**
     * Global -> [A, B]
     * A and B are passed a scope of global.
     * B depends on global.
     * A is dirty.
     * 
     * --> Global should be [(clirty, 1)]
     * --> A should be [(dirty, 1)]
     * --> B should be [(clirty, 1)]
     */
    @Test
    public void testDirty2() {
        IModule a = createChild(global, "A", globalScope);
        IModule b = createChild(global, "B", globalScope);
        addDependency(b, global);
        
        BaselineChangeSet changeSet = new BaselineChangeSet(context, empty(), list("A"), empty());
        checkFlags(global, flag(CLIRTY, 1));
        
        assertTrue(changeSet.dirty().contains(a));
        checkFlags(a, flag(DIRTY, 1));
        
        assertTrue(changeSet.clirty().contains(b));
        checkFlags(b, flag(CLIRTY, 1));
    }

}
