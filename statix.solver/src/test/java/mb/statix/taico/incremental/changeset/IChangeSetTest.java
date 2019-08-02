package mb.statix.taico.incremental.changeset;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;

import mb.nabl2.regexp.impl.FiniteAlphabet;
import mb.nabl2.terms.ITerm;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.spec.Spec;
import mb.statix.taico.incremental.Flag;
import mb.statix.taico.incremental.strategy.QueryIncrementalStrategy;
import mb.statix.taico.module.IModule;
import mb.statix.taico.module.Module;
import mb.statix.taico.module.ModuleCleanliness;
import mb.statix.taico.solver.Context;
import mb.statix.taico.util.test.TestUtil;

public class IChangeSetTest {
    protected Context context;
    protected IModule global;
    protected Scope globalScope;

    @Before
    public void setUp() throws Exception {
        //Create a context
        Spec spec = Spec.builder().noRelationLabel(mock(ITerm.class)).labels(new FiniteAlphabet<>()).build();
        context = Context.initialContext(new QueryIncrementalStrategy(), spec);
        
        //Create the global module
        global = Module.topLevelModule("global");
        globalScope = Scope.of(global.getId(), "s");
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
        
        String expected = "Expected flags " + list + ", but was " + module.getFlags();
        assertEquals(expected + " (different amount)", list.size(), module.getFlags().size());
        int i = 0;
        for (Flag flag : module.getFlags()) {
            assertEquals(expected + " (at " + i + ")", list.get(i++), flag);
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
        b.dependencies().addDependency(a);
    }
    
    /**
     * Convenient way to create a new child module (without a state).
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
        return TestUtil.createChild(parent, name, scopes);
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
        return TestUtil.list(items);
    }
    
    /**
     * Convenient way to create an empty list.
     * 
     * @return
     *      an empty list (immutable)
     */
    protected static <T> List<T> empty() {
        return TestUtil.empty();
    }

}
