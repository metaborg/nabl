package mb.statix.arithmetic;

import java.io.Serializable;
import java.util.Objects;

import org.metaborg.util.functions.Predicate2;

public class ArithTest implements Serializable {

    private static final long serialVersionUID = 1L;

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

    @Override public boolean equals(Object o) {
        if(this == o)
            return true;
        if(o == null || getClass() != o.getClass())
            return false;
        ArithTest arithTest = (ArithTest) o;
        return isEquals == arithTest.isEquals && Objects.equals(op, arithTest.op) && Objects.equals(f, arithTest.f);
    }

    @Override public int hashCode() {
        return Objects.hash(op, f, isEquals);
    }
}
