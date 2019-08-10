package mb.statix.modular.util.test;

import static mb.nabl2.terms.build.TermBuild.B;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.metaborg.util.iterators.Iterables2;

import mb.nabl2.regexp.impl.FiniteAlphabet;
import mb.nabl2.terms.ITerm;
import mb.nabl2.util.ImmutableTuple3;
import mb.nabl2.util.Tuple3;
import mb.nabl2.util.collections.HashTrieRelation3;
import mb.nabl2.util.collections.IRelation3;
import mb.statix.modular.module.IModule;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.spec.Spec;

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
        return Spec.builder()
                .noRelationLabel(noRelationLabel)
                .labels(new FiniteAlphabet<>())
                .build();
    }
    
    public static Spec createSpec(ITerm noRelationLabel, ITerm edgeLabel, ITerm dataLabel) {
        return Spec.builder()
                .noRelationLabel(noRelationLabel)
                .labels(new FiniteAlphabet<>(Iterables2.from(noRelationLabel, edgeLabel, dataLabel)))
                .addEdgeLabels(edgeLabel)
                .addRelationLabels(dataLabel)
                .build();
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
        IModule module = parent.createChild(name, list(scopes), null, false);
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
    
    /**
     * Convenient way to create a new set.
     * 
     * @param items
     *      the items to add to the set
     * 
     * @return
     *      a set with the given items
     */
    @SafeVarargs
    public static <T> Set<T> set(T... items) {
        return new HashSet<>(list(items));
    }
    
    /**
     * Convenient way to create an empty set.
     * 
     * @return
     *      an empty set (immutable)
     */
    public static <T> Set<T> emptySet() {
        return Collections.emptySet();
    }
    
    /**
     * @param objects
     *      the items for in the relation
     * 
     * @return
     *      a relation with the given items
     */
    @SuppressWarnings("unchecked")
    public static <T, U, V> IRelation3.Transient<T, U, V> relation(Object... objects) {
        IRelation3.Transient<T, U, V> rel = HashTrieRelation3.Transient.of();
        for (int i = 0; i < objects.length; i += 3) {
            rel.put((T) objects[i], (U) objects[i + 1], (V) objects[i + 2]);
        }
        return rel;
    }
    
    /**
     * @param tuples
     *      the tuples for in the relation
     * 
     * @return
     *      a relation with the given items
     */
    @SuppressWarnings("unchecked")
    public static <T, U, V> IRelation3.Transient<T, U, V> relation(Tuple3<T, U, V>... tuples) {
        IRelation3.Transient<T, U, V> rel = HashTrieRelation3.Transient.of();
        for (Tuple3<T, U, V> tuple : tuples) {
            rel.put(tuple._1(), tuple._2(), tuple._3());
        }
        return rel;
    }
    
    /**
     * @param t
     *      t
     * @param u
     *      u
     * @param v
     *      v
     * 
     * @return
     *      a tuple
     */
    public static <T, U, V> Tuple3<T, U, V> tuple(T t, U u, V v) {
        return ImmutableTuple3.of(t, u, v);
    }
}
