package statix.cli;

import java.util.List;
import java.util.Random;

public class TestRandomness {
    private long seed;
    private Random random;
    
    public TestRandomness() {
        seed = System.currentTimeMillis();
        random = new Random(seed);
    }
    
    public TestRandomness(long seed) {
        this.seed = seed;
        random = new Random(seed);
    }
    
    public Random getRandom() {
        return random;
    }
    
    public long getSeed() {
        return seed;
    }
    
    /**
     * Picks an item at random.
     * 
     * @param items
     *      the items to pick from
     * 
     * @return
     *      the randomly selected item
     * 
     * @throws IllegalArgumentException
     *      If the given array is empty.
     */
    public <T> T pick(T[] items) {
        return items[random.nextInt(items.length)];
    }
    
    /**
     * Picks an item at random.
     * 
     * @param items
     *      the items to pick from
     * 
     * @return
     *      the randomly selected item
     * 
     * @throws IllegalArgumentException
     *      If the given list is empty.
     */
    public <T> T pick(List<T> items) {
        return items.get(random.nextInt(items.size()));
    }
}
