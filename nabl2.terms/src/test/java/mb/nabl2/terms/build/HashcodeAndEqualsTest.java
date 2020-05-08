package mb.nabl2.terms.build;

import static mb.nabl2.terms.build.TermBuild.B;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;

import org.junit.Test;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.ITerm;

public class HashcodeAndEqualsTest {

    @Test public void testSameInts() {
        ITerm t1 = B.newInt(1);
        ITerm t2 = B.newInt(1);
        assertEquals(t1.hashCode(), t2.hashCode());
        assertTrue(t1.equals(t2));
    }

    @Test public void testDifferentInts() {
        ITerm t1 = B.newInt(1);
        ITerm t2 = B.newInt(2);
        assertFalse(t1.equals(t2));
    }

    @Test public void testSameStrings() {
        ITerm t1 = B.newString("Hello!");
        ITerm t2 = B.newString("Hello!");
        assertEquals(t1.hashCode(), t2.hashCode());
        assertTrue(t1.equals(t2));
    }

    @Test public void testDifferentStrings() {
        ITerm t1 = B.newString("Hello!");
        ITerm t2 = B.newString("World!");
        assertFalse(t1.equals(t2));
    }

    @Test public void testSameApplNullaryCtors() {
        ITerm t1 = B.newAppl("Ctor");
        ITerm t2 = B.newAppl("Ctor");
        assertEquals(t1.hashCode(), t2.hashCode());
        assertTrue(t1.equals(t2));
    }

    @Test public void testSameApplUnaryCtors() {
        ITerm t1 = B.newAppl("Ctor", B.newInt(1));
        ITerm t2 = B.newAppl("Ctor", B.newInt(1));
        assertEquals(t1.hashCode(), t2.hashCode());
        assertTrue(t1.equals(t2));
    }

    @Test public void testDifferentApplNullaryCtors() {
        ITerm t1 = B.newAppl("Ctor1");
        ITerm t2 = B.newAppl("Ctor2");
        assertFalse(t1.equals(t2));
    }

    @Test public void testDifferentApplArity() {
        ITerm t1 = B.newAppl("Ctor1", B.newInt(1));
        ITerm t2 = B.newAppl("Ctor2", B.newInt(1), B.newString("Hello, world!"));
        assertFalse(t1.equals(t2));
    }

    @Test public void testSpecializedEqual() {
        ITerm t1 = SpecializedAppl.of("Hello, world!", 42);
        ITerm t2 = SpecializedAppl.of("Hello, world!", 42);
        assertEquals(t1.hashCode(), t2.hashCode());
        assertTrue(t1.equals(t2));
    }

    @Test public void testSpecializedAndGenericEqual() {
        ITerm t1 = SpecializedAppl.of("Hello, world!", 42);
        ITerm t2 = B.newAppl(ASpecializedAppl.OP, B.newString("Hello, world!"), B.newInt(42));
        assertEquals(t1.hashCode(), t2.hashCode());
        assertTrue(t1.equals(t2));
    }

    @Test public void testGenericAndSpecializedEqual() {
        ITerm t1 = B.newAppl(ASpecializedAppl.OP, B.newString("Hello, world!"), B.newInt(42));
        ITerm t2 = SpecializedAppl.of("Hello, world!", 42);
        assertEquals(t1.hashCode(), t2.hashCode());
        assertTrue(t1.equals(t2));
    }

    @Test public void testEqualsIgnoresAttachmentKeys() {
        ITerm t1 = B.newAppl("Ctor", ImmutableList.of(),
                ImmutableClassToInstanceMap.builder().put(String.class, "a").build());
        ITerm t2 = B.newAppl("Ctor", ImmutableList.of(),
                ImmutableClassToInstanceMap.builder().put(Integer.class, 3).build());
        assertEquals(t1.hashCode(), t2.hashCode());
        assertTrue(t1.equals(t2));
    }

    @Test public void testEqualsIgnoresAttachmentValues() {
        ITerm t1 = B.newAppl("Ctor", ImmutableList.of(),
                ImmutableClassToInstanceMap.builder().put(String.class, "a").build());
        ITerm t2 = B.newAppl("Ctor", ImmutableList.of(),
                ImmutableClassToInstanceMap.builder().put(String.class, "b").build());
        assertEquals(t1.hashCode(), t2.hashCode());
        assertTrue(t1.equals(t2));
    }

    @Test public void testGenericApplEqualAfterSerialization() throws Exception {
        ITerm t1 = B.newAppl(ASpecializedAppl.OP, B.newString("Hello, world!"), B.newInt(42));
        ITerm t2 = deserialize(serialize(t1));
        assertEquals(t1.hashCode(), t2.hashCode());
        assertTrue(t1.equals(t2));
    }

    @Test public void testSpecializedApplEqualAfterSerialization() throws Exception {
        ITerm t1 = SpecializedAppl.of("Hello, world!", 42);
        ITerm t2 = deserialize(serialize(t1));
        assertEquals(t1.hashCode(), t2.hashCode());
        assertTrue(t1.equals(t2));
    }

    @Test public void testSpecializedApplEqualsGenericAfterSerialization() throws Exception {
        ITerm t1 = deserialize(serialize(SpecializedAppl.of("Hello, world!", 42)));
        ITerm t2 = B.newAppl(ASpecializedAppl.OP, B.newString("Hello, world!"), B.newInt(42));
        assertEquals(t1.hashCode(), t2.hashCode());
        assertTrue(t1.equals(t2));
    }

    @Test public void testGenericApplEqualsSepcializedAfterSerialization() throws Exception {
        ITerm t1 = deserialize(serialize(B.newAppl(ASpecializedAppl.OP, B.newString("Hello, world!"), B.newInt(42))));
        ITerm t2 = SpecializedAppl.of("Hello, world!", 42);
        assertEquals(t1.hashCode(), t2.hashCode());
        assertTrue(t1.equals(t2));
    }

    @Test public void testSerializeGeneric() throws Exception {
        ITerm t = B.newAppl(ASpecializedAppl.OP, B.newString("Hello, world!"), B.newInt(42));
        byte[] b1 = serialize(t);
        byte[] b2 = serialize(deserialize(b1));
        assertTrue(Arrays.equals(b1, b2));
    }

    @Test public void testSerializeSpecialized() throws Exception {
        ITerm t = SpecializedAppl.of("Hello, world!", 42);
        byte[] b1 = serialize(t);
        byte[] b2 = serialize(deserialize(b1));
        assertTrue(Arrays.equals(b1, b2));
    }


    private static byte[] serialize(Object obj) throws IOException {
        try(final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                final ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream)) {
            objectOutputStream.writeObject(obj);
            objectOutputStream.flush();
            return outputStream.toByteArray();
        }
    }

    @SuppressWarnings("unchecked") private static <T> T deserialize(byte[] bytes)
            throws ClassNotFoundException, IOException {
        try(final ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
                final ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)) {
            return (T) objectInputStream.readObject();
        }
    }

}