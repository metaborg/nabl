package mb.nabl2.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Map.Entry;

import org.junit.Test;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;

public class CapsuleUtilTest {

    // capsule util tests

    @Test public void testReplaceAll() {
        Map.Transient<String, String> map = Map.Transient.of("x", "Hello", "y", "World");
        CapsuleUtil.updateValues(map, (k, v) -> v.toUpperCase());
        assertEquals("HELLO", map.get("x"));
    }

    @Test public void testReplaceOrRemoveSome() {
        Map.Transient<String, String> map = Map.Transient.of("x", "Hello", "y", "World");
        CapsuleUtil.updateValuesOrRemove(map, (k, v) -> k == "x" ? v.toUpperCase() : null);
        assertEquals("HELLO", map.get("x"));
        assertFalse(map.containsKey("y"));
    }

    // capsule assumptions tests

    @Test(expected = UnsupportedOperationException.class) public void testEntrySetValueFails() {
        Map.Transient<String, String> map = Map.Transient.of("x", "Hello", "y", "World");
        map.entrySet().stream().forEach(e -> e.setValue(e.getValue().toUpperCase()));
        assertEquals("HELLO", map.get("x"));
    }

    @Test(expected = UnsupportedOperationException.class) public void testMapReplaceAllFails() {
        Map.Transient<String, String> map = Map.Transient.of("x", "Hello", "y", "World");
        map.replaceAll((k, v) -> v.toUpperCase());
        assertEquals("HELLO", map.get("x"));
    }

    @Test public void testPutCurrentInEntrySetLoop() {
        Map.Transient<String, String> map = Map.Transient.of("x", "Hello", "y", "World");
        for(Entry<String, String> entry : map.entrySet()) {
            map.__put(entry.getKey(), entry.getValue().toUpperCase());
        }
        assertEquals("HELLO", map.get("x"));
    }

    @Test public void testPutExtraInEntrySetLoop() {
        Map.Transient<String, String> map = Map.Transient.of("x", "Hello", "y", "World");
        for(Entry<String, String> entry : map.entrySet()) {
            assertFalse(entry.getKey().matches(".*'")); // new entries do not become part of the iteration
            map.__put(entry.getKey(), entry.getValue().toUpperCase());
            map.__put(entry.getKey() + "'", entry.getValue().toLowerCase());
        }
        assertEquals("HELLO", map.get("x"));
        assertEquals("hello", map.get("x'"));
    }

    @Test public void testPutOrDeleteInEntrySetLoop() {
        Map.Transient<String, String> map = Map.Transient.of("x", "Hello", "y", "World", "z", "!");
        for(Entry<String, String> entry : map.entrySet()) {
            if(entry.getKey().equals("y")) {
                map.__remove(entry.getKey());
            } else {
                map.__put(entry.getKey(), entry.getValue().toUpperCase());
            }
        }
        assertEquals(2, map.size());
    }

    @Test public void testAddExtraInSetLoop() {
        final Set.Transient<String> set = Set.Transient.of("x", "y", "z");
        for(String e : set) {
            assertFalse(e.matches(".*'")); // new entries do not become part of the iteration
            set.__insert(e + "'");
        }
        assertEquals(6, set.size());
    }

    @Test public void testRemoveCurrentInSetLoop() {
        final Set.Transient<String> set = Set.Transient.of("x", "y", "z");
        for(String e : set) {
            if(e.equals("y")) {
                set.__remove(e);
            }
        }
        assertEquals(2, set.size());
    }

}