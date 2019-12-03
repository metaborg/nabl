package mb.nabl2.terms.stratego;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;

import org.junit.Test;

import mb.nabl2.terms.ITerm;

public class TermIndexTest {

    @Test public void testEqualAfterSerialization1() throws Exception {
        ITerm t1 = ImmutableTermIndex.of("Hello, world!", 42);
        ITerm t2 = deserialize(serialize(t1));
        assertEquals(t1.hashCode(), t2.hashCode());
        assertTrue(t1.equals(t2));
    }

    @Test public void testEqualAfterSerialization2() throws Exception {
        ITerm t1 = ImmutableTermIndex.of("Hello, world!", 42);
        ITerm t2 = deserialize(serialize(ImmutableTermIndex.of("Hello, world!", 42)));
        assertEquals(t1.hashCode(), t2.hashCode());
        assertTrue(t1.equals(t2));
    }

    @Test public void testSerializeSpecialized() throws Exception {
        ITerm t = ImmutableTermIndex.of("Hello, world!", 42);
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