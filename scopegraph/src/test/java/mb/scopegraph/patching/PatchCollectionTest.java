package mb.scopegraph.patching;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PatchCollectionTest {

    private final String s1 = "s1";
    private final String s2 = "s2";
    private final String s3 = "s3";

    @Test public void testEmpty() {
        final PatchCollection.Immutable<String> empty = PatchCollection.Immutable.of();

        assertEquals(s1, empty.patch(s1));
        assertEmpty(empty);
        assertIdentity(empty);
    }

    @Test public void testSingleIdentity() throws InvalidPatchCompositionException {
        final PatchCollection.Immutable<String> id = PatchCollection.Immutable.<String>of().put(s1, s1);

        assertEquals(s1, id.patch(s1));
        assertNotEmpty(id);
        assertIdentity(id);
    }

    @Test public void testSinglePatch() throws InvalidPatchCompositionException {
        final PatchCollection.Immutable<String> id = PatchCollection.Immutable.<String>of().put(s2, s1);

        assertEquals(s2, id.patch(s1));
        assertEquals(s2, id.patch(s2));
        assertEquals(s3, id.patch(s3));
        assertNotEmpty(id);
        assertNonIdentity(id);
    }

    @Test public void testIdentityAndPatch() throws InvalidPatchCompositionException {
        final PatchCollection.Immutable<String> id = PatchCollection.Immutable.<String>of().put(s1, s1).put(s3, s2);

        assertEquals(s1, id.patch(s1));
        assertEquals(s3, id.patch(s2));
        assertEquals(s3, id.patch(s3)); // Also produces logger warning.

        assertNotEmpty(id);
        assertNonIdentity(id);
    }

    @Test public void testSwappingPatch() throws InvalidPatchCompositionException {
        final PatchCollection.Immutable<String> id = PatchCollection.Immutable.<String>of().put(s1, s2).put(s2, s1);

        assertEquals(s2, id.patch(s1));
        assertEquals(s1, id.patch(s2));
        assertEquals(s3, id.patch(s3));

        assertNotEmpty(id);
        assertNonIdentity(id);
    }

    // Invalid additions

    @Test(expected = InvalidPatchCompositionException.class) public void testDoubleSrc() throws InvalidPatchCompositionException {
        PatchCollection.Immutable.<String>of().put(s2, s1).put(s3, s1);
    }

    @Test(expected = InvalidPatchCompositionException.class) public void testDoubleTgt() throws InvalidPatchCompositionException {
        PatchCollection.Immutable.<String>of().put(s3, s1).put(s3, s2);
    }

    @Test(expected = InvalidPatchCompositionException.class) public void testSrcID() throws InvalidPatchCompositionException {
        PatchCollection.Immutable.<String>of().put(s2, s2).put(s3, s2);
    }

    @Test(expected = InvalidPatchCompositionException.class) public void testTgtID() throws InvalidPatchCompositionException {
        PatchCollection.Immutable.<String>of().put(s2, s2).put(s2, s3);
    }

    @Test(expected = InvalidPatchCompositionException.class) public void testIDSrc() throws InvalidPatchCompositionException {
        PatchCollection.Immutable.<String>of().put(s3, s2).put(s2, s2);
    }

    @Test(expected = InvalidPatchCompositionException.class) public void testIDTgt() throws InvalidPatchCompositionException {
        PatchCollection.Immutable.<String>of().put(s2, s3).put(s2, s2);
    }

    // Valid compositions

    @Test public void testCompose_IDandID() throws InvalidPatchCompositionException {
        final PatchCollection.Immutable<String> p1 = PatchCollection.Immutable.<String>of().put(s1, s1);
        final PatchCollection.Immutable<String> p2 = PatchCollection.Immutable.<String>of().put(s2, s2);

        final PatchCollection.Immutable<String> compose = p1.putAll(p2);

        assertEquals(0, compose.patches().size());
        assertEquals(2, compose.identityPatches().size());
    }

    @Test public void testCompose_MapAndID() throws InvalidPatchCompositionException {
        final PatchCollection.Immutable<String> p1 = PatchCollection.Immutable.<String>of().put(s1, s1);
        final PatchCollection.Immutable<String> p2 = PatchCollection.Immutable.<String>of().put(s3, s2);

        final PatchCollection.Immutable<String> compose = p1.putAll(p2);

        assertEquals(1, compose.patches().size());
        assertEquals(1, compose.identityPatches().size());

        assertEquals(s1, compose.patch(s1));
        assertEquals(s3, compose.patch(s2));
    }

    @Test public void testCompose_IDAndMap() throws InvalidPatchCompositionException {
        final PatchCollection.Immutable<String> p1 = PatchCollection.Immutable.<String>of().put(s3, s2);
        final PatchCollection.Immutable<String> p2 = PatchCollection.Immutable.<String>of().put(s1, s1);

        final PatchCollection.Immutable<String> compose = p1.putAll(p2);

        assertEquals(1, compose.patches().size());
        assertEquals(1, compose.identityPatches().size());

        assertEquals(s3, compose.patch(s2));
        assertEquals(s1, compose.patch(s1));
    }

    @Test public void testCompose_MapAndMap() throws InvalidPatchCompositionException {
        final PatchCollection.Immutable<String> p1 = PatchCollection.Immutable.<String>of().put(s2, s1);
        final PatchCollection.Immutable<String> p2 = PatchCollection.Immutable.<String>of().put(s3, s2);

        final PatchCollection.Immutable<String> compose = p1.putAll(p2);

        assertEquals(2, compose.patches().size());
        assertEquals(0, compose.identityPatches().size());

        assertEquals(s2, compose.patch(s1));
        assertEquals(s3, compose.patch(s2));
    }

    @Test public void testCompose_EqualID() throws InvalidPatchCompositionException {
        final PatchCollection.Immutable<String> p1 = PatchCollection.Immutable.<String>of().put(s1, s1);
        final PatchCollection.Immutable<String> p2 = PatchCollection.Immutable.<String>of().put(s1, s1);

        final PatchCollection.Immutable<String> compose = p1.putAll(p2);

        assertEquals(0, compose.patches().size());
        assertEquals(1, compose.identityPatches().size());

        assertEquals(s1, compose.patch(s1));
    }

    @Test public void testCompose_EqualMap() throws InvalidPatchCompositionException {
        final PatchCollection.Immutable<String> p1 = PatchCollection.Immutable.<String>of().put(s2, s1);
        final PatchCollection.Immutable<String> p2 = PatchCollection.Immutable.<String>of().put(s2, s1);

        final PatchCollection.Immutable<String> compose = p1.putAll(p2);

        assertEquals(1, compose.patches().size());
        assertEquals(0, compose.identityPatches().size());

        assertEquals(s2, compose.patch(s1));
    }

    // Invalid compositions

    @Test(expected = IllegalArgumentException.class) public void testCompose_DoubleSrc() throws InvalidPatchCompositionException {
        final PatchCollection.Immutable<String> p1 = PatchCollection.Immutable.<String>of().put(s3, s1);
        final PatchCollection.Immutable<String> p2 = PatchCollection.Immutable.<String>of().put(s2, s1);

        p1.putAll(p2);
    }

    @Test(expected = IllegalArgumentException.class) public void testCompose_DoubleTgt() throws InvalidPatchCompositionException {
        final PatchCollection.Immutable<String> p1 = PatchCollection.Immutable.<String>of().put(s1, s2);
        final PatchCollection.Immutable<String> p2 = PatchCollection.Immutable.<String>of().put(s1, s3);

        p1.putAll(p2);
    }

    @Test(expected = InvalidPatchCompositionException.class) public void testCompose_IDSrc() throws InvalidPatchCompositionException {
        final PatchCollection.Immutable<String> p1 = PatchCollection.Immutable.<String>of().put(s1, s1);
        final PatchCollection.Immutable<String> p2 = PatchCollection.Immutable.<String>of().put(s2, s1);

        final PatchCollection.Immutable<String> pc = p1.putAll(p2);
        pc.assertConsistent();
    }

    @Test(expected = InvalidPatchCompositionException.class) public void testCompose_IDTgt() throws InvalidPatchCompositionException {
        final PatchCollection.Immutable<String> p1 = PatchCollection.Immutable.<String>of().put(s1, s1);
        final PatchCollection.Immutable<String> p2 = PatchCollection.Immutable.<String>of().put(s1, s2);

        final PatchCollection.Immutable<String> pc = p1.putAll(p2);
        pc.assertConsistent();
    }

    @Test(expected = InvalidPatchCompositionException.class) public void testCompose_SrcID() throws InvalidPatchCompositionException {
        final PatchCollection.Immutable<String> p1 = PatchCollection.Immutable.<String>of().put(s2, s1);
        final PatchCollection.Immutable<String> p2 = PatchCollection.Immutable.<String>of().put(s1, s1);

        final PatchCollection.Immutable<String> pc = p1.putAll(p2);
        pc.assertConsistent();
    }

    @Test(expected = InvalidPatchCompositionException.class) public void testCompose_TgtID() throws InvalidPatchCompositionException {
        final PatchCollection.Immutable<String> p1 = PatchCollection.Immutable.<String>of().put(s1, s2);
        final PatchCollection.Immutable<String> p2 = PatchCollection.Immutable.<String>of().put(s1, s1);

        final PatchCollection.Immutable<String> pc = p1.putAll(p2);
        pc.assertConsistent();
    }

    // Helper assertions

    public void assertEmpty(IPatchCollection<?> patches) {
        assertTrue("Expected empty patch collection.", patches.isEmpty());
    }

    public void assertNotEmpty(IPatchCollection<?> patches) {
        assertFalse("Expected non-empty patch collection.", patches.isEmpty());
    }

    public void assertIdentity(IPatchCollection<?> patches) {
        assertTrue("Expected identity patch set.", patches.isIdentity());
    }

    public void assertNonIdentity(IPatchCollection<?> patches) {
        assertFalse("Expected non-identity patch set.", patches.isIdentity());
    }

}
