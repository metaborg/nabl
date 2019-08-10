package mb.statix.modular.incremental;

import static mb.statix.modular.module.ModuleCleanliness.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import mb.statix.modular.incremental.Flag;
import mb.statix.modular.incremental.Flaggable;
import mb.statix.modular.util.StablePriorityQueue;

public class FlaggableTest {

    private Flaggable flaggable;
    
    /**
     * @return
     *      a newly created flaggable
     */
    public Flaggable createFlaggable() {
        return new Flaggable() {
            private StablePriorityQueue<Flag> queue = new StablePriorityQueue<>();
            
            @Override
            public StablePriorityQueue<Flag> getFlags() {
                return queue;
            }
        };
    }
    
    @Before
    public void setUp() throws Exception {
        this.flaggable = createFlaggable();
    }

    /**
     * No flag present.
     */
    @Test
    public void testGetTopFlag1() {
        assertEquals(flaggable.getTopFlag(), Flag.CLEAN);
    }
    
    /**
     * One flag present.
     */
    @Test
    public void testGetTopFlag2() {
        Flag flag = new Flag(DIRTY, 1);
        flaggable.addFlag(flag);
        assertEquals(flaggable.getTopFlag(), flag);
    }
    
    /**
     * One flag present.
     */
    @Test
    public void testGetTopFlag3() {
        Flag flag1 = new Flag(UNSURE, 2);
        Flag flag2 = new Flag(UNSURE, 1);
        flaggable.addFlag(flag1);
        flaggable.addFlag(flag2);
        assertEquals(flaggable.getTopFlag(), flag2);
    }

    @Test
    public void testGetTopCleanliness() {
        Flag flag = new Flag(DIRTY, 1);
        flaggable.addFlag(flag);
        assertEquals(flaggable.getTopCleanliness(), flag.getCleanliness());
    }

    @Test
    public void testGetTopLevel() {
        Flag flag = new Flag(DIRTY, 1);
        flaggable.addFlag(flag);
        assertEquals(flaggable.getTopLevel(), flag.getLevel());
    }

    @Test
    public void testAddFlagIfNotSameCause() {
        Flag flag1 = new Flag(UNSURE, 1, "A");
        Flag flag2 = new Flag(UNSURECHILD, 1, "A");
        flaggable.addFlag(flag1);
        assertFalse(flaggable.addFlagIfNotSameCause(flag2));
        assertEquals(1, flaggable.getFlags().size());
    }

    @Test
    public void testSetFlag() {
        Flag flag1 = new Flag(DIRTY, 1);
        Flag flag2 = new Flag(DIRTY, 2);
        Flag flag3 = new Flag(DIRTY, 3);
        flaggable.addFlag(flag1);
        flaggable.addFlag(flag2);
        flaggable.setFlag(flag3);
        assertEquals(flag3, flaggable.getTopFlag());
        assertEquals(1, flaggable.getFlags().size());
    }

    /**
     * When it shouldn't have an effect.
     */
    @Test
    public void testSetFlagIfClean1() {
        Flag flag1 = new Flag(DIRTY, 1);
        Flag flag2 = new Flag(DIRTY, 2);
        flaggable.addFlag(flag1);
        flaggable.setFlagIfClean(flag2);
        assertEquals(flag1, flaggable.getTopFlag());
        assertEquals(1, flaggable.getFlags().size());
    }
    
    /**
     * When it should have an effect.
     */
    @Test
    public void testSetFlagIfClean2() {
        Flag flag1 = new Flag(DIRTY, 1);
        flaggable.setFlagIfClean(flag1);
        assertEquals(flag1, flaggable.getTopFlag());
        assertEquals(1, flaggable.getFlags().size());
    }

    @Test
    public void testRemoveTopFlag() {
        Flag flag1 = new Flag(DIRTY, 1);
        flaggable.addFlag(flag1);
        assertTrue(flaggable.removeTopFlag());
        assertEquals(0, flaggable.getFlags().size());
    }

    @Test
    public void testRemoveFlag() {
        Flag flag1 = new Flag(DIRTY, 1);
        Flag flag2 = new Flag(DIRTY, 2);
        flaggable.addFlag(flag1);
        flaggable.addFlag(flag2);
        
        assertTrue(flaggable.removeFlag(flag1));
        assertFalse(flaggable.removeFlag(flag1));
        assertEquals(flag2, flaggable.getTopFlag());
        assertEquals(1, flaggable.getFlags().size());
    }

}
