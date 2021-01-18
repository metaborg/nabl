package mb.renaming.namegraph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import mb.renaming.namegraph.NameIndex;

public class NameIndexTest {

	@Test
	public void testEqual() {
		NameIndex index1 = new NameIndex(1, "foo");
		NameIndex index2 = new NameIndex(1, "foo");
		assertEquals(index1, index2);
	}
	
	@Test
	public void testNotEqualNumIndex() {
		NameIndex index1 = new NameIndex(1, "foo");
		NameIndex index2 = new NameIndex(2, "foo");
		assertNotEquals(index1, index2);
	}
	
	@Test
	public void testNotEqualPath() {
		NameIndex index1 = new NameIndex(1, "foo");
		NameIndex index2 = new NameIndex(1, "bar");
		assertNotEquals(index1, index2);
	}
	
	@Test
	public void testNotEqualType() {
		NameIndex index = new NameIndex(1, "foo");
		String otherType = "foo";
		assertNotEquals(index, otherType);
	}
	
	@Test
	public void testInvalidNumIndex() {
		 assertThrows(IllegalArgumentException.class, () -> {
			 new NameIndex(-1, "foo");
			  });
		
	}
	
	@Test
	public void testInvalidPath() {
		 assertThrows(IllegalArgumentException.class, () -> {
			 new NameIndex(1, null);
			  });
		
	}
	
	@Test
	public void testHashCode() {
		NameIndex index1 = new NameIndex(1, "foo");
		NameIndex index2 = new NameIndex(1, "foo");
		assertEquals(index1.hashCode(), index2.hashCode());
	}
	
	@Test
	public void testToString() {
		NameIndex index = new NameIndex(1, "foo");
		assertEquals(index.toString(), "NameIndex(foo, 1)");
	}
}
