package mb.statix.modular.util;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import mb.statix.modular.util.StablePriorityQueue;

/**
 * Test class for {@link StablePriorityQueue}.
 */
public class StablePriorityQueueTest {
    protected StablePriorityQueue<Dummy> queue;

    /**
     * Dummy object for testing orderings.
     */
    protected static class Dummy implements Comparable<Dummy> {
        protected final int order;
        
        public Dummy(int order) {
            this.order = order;
        }
        
        @Override
        public int compareTo(Dummy o) {
            return Integer.compare(order, o.order);
        }
        
        @Override
        public int hashCode() {
            return order;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof Dummy)) return false;
            
            return order == ((Dummy) obj).order;
        }
        
        @Override
        public String toString() {
            return String.valueOf(order);
        }
    }
    
    public Dummy dummy(int number) {
        return new Dummy(number);
    }
    
    @Before
    public void setUp() throws Exception {
        queue = new StablePriorityQueue<>();
    }

    @Test
    public void testEqualElementsRetainPositioning() {
        Dummy one = dummy(1);
        Dummy two = dummy(1);
        
        queue.add(one);
        queue.add(two);
        assertSame(one, queue.element());
    }

    @Test
    public void testEqualElementsRetainPositioning2() {
        Dummy one = dummy(1);
        Dummy two = dummy(1);
        Dummy more = dummy(2);
        
        queue.add(one);
        queue.add(two);
        queue.add(more);
        assertSame(one, queue.element());
    }
    
    @Test
    public void testPriorityQueue1() {
        Dummy three = dummy(3);
        Dummy two = dummy(2);
        Dummy one = dummy(1);
        
        queue.add(three);
        queue.add(two);
        queue.add(one);
        assertSame(one, queue.element());
    }
    
    @Test
    public void testPriorityQueue2() {
        Dummy three = dummy(3);
        Dummy two = dummy(2);
        Dummy one = dummy(1);
        
        queue.add(two);
        queue.add(one);
        queue.add(three);
        assertSame(one, queue.element());
    }
}
