package mb.nabl2.terms.unification;

import java.math.BigInteger;

import org.metaborg.util.functions.Action0;
import org.metaborg.util.functions.Action1;
import org.metaborg.util.functions.Function0;
import org.metaborg.util.functions.Function1;

public final class TermSize {

    public static final TermSize ZERO = new TermSize(BigInteger.ZERO);
    public static final TermSize ONE = new TermSize(BigInteger.ONE);
    public static final TermSize INF = new TermSize(true);

    private final boolean infinite;
    private final BigInteger size;

    private TermSize(BigInteger size) {
        this.size = size;
        this.infinite = false;
    }

    private TermSize(boolean infinite) {
        this.size = BigInteger.ZERO;
        this.infinite = infinite;
    }

    public TermSize add(TermSize other) {
        return (this.infinite || other.infinite) ? INF : new TermSize(this.size.add(other.size));
    }

    public void ifInfinite(Action0 a) {
        if(infinite) {
            a.apply();
        }
    }

    public void ifFinite(Action1<BigInteger> a) {
        if(!infinite) {
            a.apply(size);
        }
    }

    public <T> T match(Function1<BigInteger, T> onFinite, Function0<T> onInfinite) {
        return infinite ? onInfinite.apply() : onFinite.apply(size);
    }

    @Override public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (infinite ? 1231 : 1237);
        result = prime * result + ((size == null) ? 0 : size.hashCode());
        return result;
    }

    @Override public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if(obj == null)
            return false;
        if(getClass() != obj.getClass())
            return false;
        TermSize other = (TermSize) obj;
        if(infinite != other.infinite)
            return false;
        if(size == null) {
            if(other.size != null)
                return false;
        } else if(!size.equals(other.size))
            return false;
        return true;
    }

    @Override public String toString() {
        return infinite ? "inf" : size.toString();
    }

}