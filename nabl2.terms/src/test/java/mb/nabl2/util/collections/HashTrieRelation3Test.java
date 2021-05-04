package mb.nabl2.util.collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.metaborg.util.collection.HashTrieRelation3;
import org.metaborg.util.collection.IRelation3;

public class HashTrieRelation3Test {

    @Test public void testContainsInserted() {
        IRelation3.Transient<String, Boolean, Integer> rel = HashTrieRelation3.Transient.of();

        rel.put("one", true, 1);

        assertTrue(rel.contains("one"));
        assertTrue(rel.contains("one", true));
        assertTrue(rel.contains("one", true, 1));

        assertFalse(rel.contains("two"));
        assertFalse(rel.contains("one", false));
        assertFalse(rel.contains("two", true));
        assertFalse(rel.contains("one", true, 2));
    }

    @Test public void testRemoveKey() {
        IRelation3.Transient<String, Boolean, Integer> rel = HashTrieRelation3.Transient.of();

        rel.put("one", true, 1);
        rel.put("one", false, 1);
        rel.put("two", true, 2);

        rel.remove("one");

        assertFalse(rel.contains("one"));
        assertFalse(rel.contains("one", true));
        assertFalse(rel.contains("one", false));
        assertFalse(rel.contains("one", true, 1));
        assertFalse(rel.contains("one", false, 1));

        assertTrue(rel.contains("two"));
        assertTrue(rel.contains("two", true));
        assertTrue(rel.contains("two", true, 2));
    }

    @Test public void testRemoveKeyLabel() {
        IRelation3.Transient<String, Boolean, Integer> rel = HashTrieRelation3.Transient.of();

        rel.put("one", true, 1);
        rel.put("one", false, 1);

        rel.remove("one", false);

        assertTrue(rel.contains("one"));
        assertTrue(rel.contains("one", true));
        assertFalse(rel.contains("one", false));
        assertTrue(rel.contains("one", true, 1));
        assertFalse(rel.contains("one", false, 1));
    }

    @Test public void testRemoveEntry() {
        IRelation3.Transient<String, Boolean, Integer> rel = HashTrieRelation3.Transient.of();

        rel.put("one", true, 1);
        rel.put("one", true, 2);

        rel.remove("one", true, 2);

        assertTrue(rel.contains("one"));
        assertTrue(rel.contains("one", true));
        assertTrue(rel.contains("one", true, 1));
        assertFalse(rel.contains("one", true, 2));
    }

    @Test public void testRemoveFromInverse() {
        IRelation3.Transient<String, Boolean, Integer> rel = HashTrieRelation3.Transient.of();

        rel.put("one", true, 1);
        rel.put("one", false, 2);
        rel.put("two", true, 1);

        rel.inverse().remove(1);

        assertTrue(rel.contains("one"));
        assertFalse(rel.contains("one", true));
        assertTrue(rel.contains("one", false));
        assertFalse(rel.contains("one", true, 1));
        assertFalse(rel.contains("one", false, 1));

        assertFalse(rel.contains("two"));
        assertFalse(rel.contains("two", true));
        assertFalse(rel.contains("two", true, 1));
    }

}