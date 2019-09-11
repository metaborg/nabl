package mb.statix.arithmetic;

import org.metaborg.util.functions.Predicate2;

public class ArithTest {

    private final String op;
    private final Predicate2<Integer, Integer> f;
    private final boolean isEquals;

    public ArithTest(String op, Predicate2<Integer, Integer> f, boolean isEquals) {
        this.op = op;
        this.f = f;
        this.isEquals = isEquals;
    }

    public boolean isEquals() {
        return isEquals;
    }

    public boolean test(int i1, int i2) {
        return f.test(i1, i2);
    }

    @Override public String toString() {
        return op;
    }

}