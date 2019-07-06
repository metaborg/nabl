package mb.statix.taico.incremental.changeset;

import static mb.statix.taico.module.ModuleCleanliness.*;
import static org.junit.Assert.*;

import org.junit.Test;

import mb.statix.taico.incremental.Flag;
import mb.statix.taico.module.IModule;

public class QueryChangeSetTest extends IChangeSetTest {
    @Test
    public void testAddedOneFromEmpty() {
        QueryChangeSet changeSet = new QueryChangeSet(context, list("A"), empty(), empty());
        //A = createChild(global, "A", Scope.of("global", 
        assertTrue(changeSet.added().contains("A"));
        assertTrue(changeSet.hasNewChild().contains(global));
        assertTrue(changeSet.hasNewChildIds().contains(global.getId()));
        assertEquals(global.getTopFlag(), Flag.NEWCHILD);
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
        
        QueryChangeSet changeSet = new QueryChangeSet(context, empty(), list("A"), empty());
        
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
     * B depends on A.
     * A is dirty.
     * 
     * --> Global should be [(dirtyChild, 1, A), (clirty, 2, B)]
     * --> A should be [(dirty, 1)]
     * --> B should be [(clirty, 1, A)]
     */
    @Test
    public void testDirty2() {
        IModule a = createChild(global, "A", globalScope);
        IModule b = createChild(global, "B", globalScope);
        addDependency(b, global);
        addDependency(b, a);
        
        QueryChangeSet changeSet = new QueryChangeSet(context, empty(), list("A"), empty());
        assertTrue(changeSet.hasDirtyChild().contains(global));
        assertTrue(changeSet.hasClirtyChild().contains(global));
        checkFlags(global, flag(DIRTYCHILD, 1, a), flag(CLIRTYCHILD, 2, b));
        
        assertTrue(changeSet.dirty().contains(a));
        checkFlags(a, flag(DIRTY, 1));
        
        assertTrue(changeSet.clirty().contains(b));
        checkFlags(b, flag(CLIRTY, 1, a), flag(CLIRTY, 2, global));
    }
}
