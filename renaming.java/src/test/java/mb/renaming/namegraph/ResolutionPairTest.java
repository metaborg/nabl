package mb.renaming.namegraph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import mb.renaming.namegraph.NameIndex;
import mb.renaming.namegraph.ResolutionPair;

public class ResolutionPairTest {

	@Test
	public void testEqual() {
		ResolutionPair pair1 = new ResolutionPair("foo", 2, 1);
		ResolutionPair pair2 = new ResolutionPair("foo", 2, 1);
		
		assertEquals(pair1, pair2);
	}
	
	@Test
	public void testNotEqualRef() {
		ResolutionPair pair1 = new ResolutionPair("foo", 2, 1);
		ResolutionPair pair2 = new ResolutionPair("foo", 3, 1);
		
		assertNotEquals(pair1, pair2);
	}
	
	@Test
	public void testNotEqualDec() {
		ResolutionPair pair1 = new ResolutionPair("foo", 2, 1);		
		ResolutionPair pair2 = new ResolutionPair("foo", 2 ,3);
		
		assertNotEquals(pair1, pair2);
	}
	
	@Test
	public void testNotEqualType() {
		ResolutionPair pair = new ResolutionPair("foo", 2, 1);
		
		String otherType = "foo";
		assertNotEquals(pair, otherType);
	}
	
	@Test
	public void testInvalidRef() {
		 assertThrows(IllegalArgumentException.class, () -> {
				NameIndex index = new NameIndex(1, "foo");
				new ResolutionPair(null, index);
			  });
		
	}
	
	@Test
	public void testInvalidDec() {
		 assertThrows(IllegalArgumentException.class, () -> {
				NameIndex index = new NameIndex(1, "foo");
				new ResolutionPair(index, null);
			  });
		
	}
	
	
	@Test
	public void testConstructorOne() {
		NameIndex ref = new NameIndex(2, "foo");
		NameIndex dec = new NameIndex(1, "foo");
		ResolutionPair pair = new ResolutionPair(ref, dec);
		
		assertEquals(ref, pair.getReference());
		assertEquals(dec, pair.getDeclaration());
	}
	
	@Test
	public void testConstructorTwo() {
		NameIndex ref = new NameIndex(2, "foo");
		NameIndex dec = new NameIndex(1, "foo");
		ResolutionPair pair = new ResolutionPair("foo", 2, 1);
		
		assertEquals(ref, pair.getReference());
		assertEquals(dec, pair.getDeclaration());
	}
	
	@Test
	public void testHashCode() {
		ResolutionPair pair1 = new ResolutionPair("foo", 2, 1);
		ResolutionPair pair2 = new ResolutionPair("foo", 2, 1);
		
		assertEquals(pair1.hashCode(), pair2.hashCode());
		
	}
	
	@Test
	public void testToString() {
		NameIndex ref = new NameIndex(2, "foo");
		NameIndex dec = new NameIndex(1, "foo");
		ResolutionPair pair = new ResolutionPair(ref, dec);
		
		assertEquals(pair.toString(), "ResolutionPair(NameIndex(foo, 2), NameIndex(foo, 1))");
	}

}
