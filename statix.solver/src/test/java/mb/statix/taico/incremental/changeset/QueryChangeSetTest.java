package mb.statix.taico.incremental.changeset;

import static mb.statix.taico.module.ModuleCleanliness.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import mb.nabl2.regexp.impl.FiniteAlphabet;
import mb.nabl2.terms.ITerm;
import mb.statix.constraints.CResolveQuery;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.spec.Spec;
import mb.statix.taico.incremental.Flag;
import mb.statix.taico.incremental.strategy.QueryIncrementalStrategy;
import mb.statix.taico.module.IModule;
import mb.statix.taico.module.Module;
import mb.statix.taico.module.ModuleCleanliness;
import mb.statix.taico.solver.SolverContext;

public class QueryChangeSetTest {
    protected SolverContext context;
    protected IModule global;
    protected Scope globalScope;

    @Before
    public void setUp() throws Exception {
        //Create a context
        Spec spec = Spec.builder().noRelationLabel(mock(ITerm.class)).labels(new FiniteAlphabet<>()).build();
        context = SolverContext.initialContext(new QueryIncrementalStrategy(), spec);
        
        //Create the global module and it's state
        global = new Module("global");
        globalScope = Scope.of(global.getId(), "s");
    }

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
    
    /**
     * Checks if the given flags are the actual flags of the given module. The given flags will be
     * sorted before comparing them to the actual flags.
     * 
     * @param module
     *      the module to check the flags of
     * @param expectedFlags
     *      the expected flags, don't have to be in the correct order.
     */
    protected static void checkFlags(IModule module, Flag... expectedFlags) {
        //Sort the given flags to the correct 
        List<Flag> list = new ArrayList<>(Arrays.asList(expectedFlags));
        Collections.sort(list);
        
        assertEquals("Expected flags " + expectedFlags + ", but was " + module.getFlags() + " (different amount)", list.size(), module.getFlags().size());
        int i = 0;
        for (Flag flag : module.getFlags()) {
            assertEquals("Expected flags " + expectedFlags + ", but was " + module.getFlags() + " (at " + i + ")", list.get(i++), flag);
        }
    }
    
    /**
     * Shorthand for creating a flag.
     */
    protected static Flag flag(ModuleCleanliness c, int level) {
        return new Flag(c, level);
    }
    
    /**
     * Shorthand for creating a flag.
     */
    protected static Flag flag(ModuleCleanliness c, int level, IModule module) {
        return new Flag(c, level, module.getId());
    }
    
    /**
     * Adds a dependency of "b depends on a" without specifying a query.
     * 
     * @param b
     *      the module with the dependency
     * @param a
     *      the module that is depended upon
     */
    protected static void addDependency(IModule b, IModule a) {
        a.addDependant(b.getId(), mock(CResolveQuery.class));
    }
    
    /**
     * Convenient way to create a new child module.
     * 
     * @param parent
     *      the parent
     * @param name
     *      the name of the module
     * @param scopes
     *      the scope that it can extend
     * 
     * @return
     *      the child module
     */
    protected static IModule createChild(IModule parent, String name, Scope... scopes) {
        return parent.createChild(name, list(scopes), null);
    }
    
    /**
     * Convenient way to create a new list.
     * 
     * @param items
     *      the items to add to the list
     * 
     * @return
     *      a list with the given items (fixed size)
     */
    @SafeVarargs
    protected static <T> List<T> list(T... items) {
        return Arrays.asList(items);
    }
    
    /**
     * Convenient way to create an empty list.
     * 
     * @return
     *      an empty list (immutable)
     */
    protected static <T> List<T> empty() {
        return Collections.emptyList();
    }

}
