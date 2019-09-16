package mb.statix.random.strategy;

import org.metaborg.util.functions.Function1;

public class Either2<T1, T2> {

    private final T1 left;
    private final T2 right;

    private Either2(T1 left, T2 right) {
        this.left = left;
        this.right = right;
    }

    public <R> R map(Function1<T1, R> mapLeft, Function1<T2, R> mapRight) {
        if(left != null) {
            return mapLeft.apply(left);
        } else {
            return mapRight.apply(right);
        }
    }

    public static <T1, T2> Either2<T1, T2> ofLeft(T1 left) {
        return new Either2<>(left, null);
    }

    public static <T1, T2> Either2<T1, T2> ofRight(T2 right) {
        return new Either2<>(null, right);
    }

}