package mb.statix.random.util;

import java.util.Random;
import java.util.stream.IntStream;

public class RandomUtil {

    public static IntStream ints(int start, int end, Random rnd) {
        return end > start ? rnd.ints(start, end) : IntStream.empty();
    }

}