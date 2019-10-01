package mb.statix.random.util;

import java.util.Random;

public class RandomGenerator implements org.apache.commons.math3.random.RandomGenerator {

    private final Random rnd;

    public RandomGenerator(Random rnd) {
        this.rnd = rnd;
    }

    @Override public void setSeed(int seed) {
        throw new UnsupportedOperationException();
    }

    @Override public void setSeed(int[] seed) {
        throw new UnsupportedOperationException();
    }

    @Override public void setSeed(long seed) {
        throw new UnsupportedOperationException();
    }

    @Override public void nextBytes(byte[] bytes) {
        rnd.nextBytes(bytes);
    }

    @Override public int nextInt() {
        return rnd.nextInt();
    }

    @Override public int nextInt(int n) {
        return rnd.nextInt(n);
    }

    @Override public long nextLong() {
        return rnd.nextLong();
    }

    @Override public boolean nextBoolean() {
        return rnd.nextBoolean();
    }

    @Override public float nextFloat() {
        return rnd.nextFloat();
    }

    @Override public double nextDouble() {
        return rnd.nextDouble();
    }

    @Override public double nextGaussian() {
        return rnd.nextGaussian();
    }

}