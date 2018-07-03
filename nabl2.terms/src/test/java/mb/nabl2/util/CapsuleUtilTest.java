package mb.nabl2.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Map.Entry;

import org.junit.Test;

import io.usethesource.capsule.Map;

public class CapsuleUtilTest {

    // capsule util tests
    
    @Test public void testTransientReplaceAll() {
        Map.Transient<String, String> map = Map.Transient.of("x", "Hello", "y", "World");
        CapsuleUtil.replace(map, (k, v) -> v.toUpperCase());
        assertEquals("HELLO", map.get("x"));
    }

    @Test public void testTransientReplaceSome() {
        Map.Transient<String, String> map = Map.Transient.of("x", "Hello", "y", "World");
        CapsuleUtil.replace(map, (k, v) -> k == "x" ? v.toUpperCase() : null);
        assertEquals("HELLO", map.get("x"));
        assertFalse(map.containsKey("y"));
    }

    @Test public void testImmutableReplaceAll() {
        Map.Immutable<String, String> map = Map.Immutable.of("x", "Hello", "y", "World");
        map = CapsuleUtil.replace(map, (k, v) -> v.toUpperCase());
        assertEquals("HELLO", map.get("x"));
    }

    @Test public void testImmutableReplaceSome() {
        Map.Immutable<String, String> map = Map.Immutable.of("x", "Hello", "y", "World");
        map = CapsuleUtil.replace(map, (k, v) -> k == "x" ? v.toUpperCase() : null);
        assertEquals("HELLO", map.get("x"));
        assertFalse(map.containsKey("y"));
    }

    // capsule assumptions tests

    @Test(expected = UnsupportedOperationException.class) public void testSetValue() {
        Map.Transient<String, String> map = Map.Transient.of("x", "Hello", "y", "World");
        map.entrySet().stream().forEach(e -> e.setValue(e.getValue().toUpperCase()));
        assertEquals("HELLO", map.get("x"));
    }

    @Test(expected = UnsupportedOperationException.class) public void testReplace() {
        Map.Transient<String, String> map = Map.Transient.of("x", "Hello", "y", "World");
        map.replaceAll((k, v) -> v.toUpperCase());
        assertEquals("HELLO", map.get("x"));
    }

    @Test public void testPutInEntryLoop() {
        Map.Transient<String, String> map = Map.Transient.of("x", "Hello", "y", "World");
        for(Entry<String, String> entry : map.entrySet()) {
            map.__put(entry.getKey(), entry.getValue().toUpperCase());
        }
        assertEquals("HELLO", map.get("x"));
    }

    @Test public void testPutExtraInEntryLoop() {
        Map.Transient<String, String> map = Map.Transient.of("x", "Hello", "y", "World");
        for(Entry<String, String> entry : map.entrySet()) {
            map.__put(entry.getKey(), entry.getValue().toUpperCase());
            map.__put(entry.getKey() + "'", entry.getValue().toLowerCase());
        }
        assertEquals("HELLO", map.get("x"));
        assertEquals("hello", map.get("x'"));
    }

    @Test public void testPutOrDeleteInEntryLoop() {
        Map.Transient<String, String> map = Map.Transient.of("x", "Hello", "y", "World");
        boolean put = true;
        for(Entry<String, String> entry : map.entrySet()) {
            if(put) {
                map.__put(entry.getKey(), entry.getValue().toUpperCase());
            } else {
                map.__remove(entry.getKey());
            }
            put = !put;
        }
        assertEquals(1, map.size());
    }

}