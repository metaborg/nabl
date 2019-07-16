package mb.statix.taico.util.test;

import static mb.nabl2.terms.build.TermBuild.B;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import mb.nabl2.regexp.impl.FiniteAlphabet;
import mb.nabl2.terms.ITerm;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.spec.Spec;
import mb.statix.taico.module.IModule;

public class TestUtil {

    /**
     * @param label
     * @return
     *      the new label string
     */
    public static ITerm label(String label) {
        return B.newString(label);
    }
    
    public static Spec createSpec() {
        return createSpec(mock(ITerm.class));
    }
    
    public static Spec createSpec(ITerm noRelationLabel) {
        return Spec.builder().noRelationLabel(noRelationLabel).labels(new FiniteAlphabet<>()).build();
    }
    
    /**
     * Convenient way to create a new child module with a state.
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
    public static IModule createChild(IModule parent, String name, Scope... scopes) {
        IModule module = parent.createChild(name, list(scopes), null);
        parent.addChild(module);
        return module;
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
    public static <T> List<T> list(T... items) {
        return Arrays.asList(items);
    }
    
    /**
     * Convenient way to create an empty list.
     * 
     * @return
     *      an empty list (immutable)
     */
    public static <T> List<T> empty() {
        return Collections.emptyList();
    }
}
