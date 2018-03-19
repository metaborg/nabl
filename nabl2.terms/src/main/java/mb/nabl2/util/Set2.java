package mb.nabl2.util;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

public final class Set2<T> {

    private final T t1;
    private final T t2;

    private Set2(T t1, T t2) {
        this.t1 = t1;
        this.t2 = t2;
    }

    public Set<T> elementSet() {
        return ImmutableSet.of(t1, t2);
    }

    public boolean contains(T t) {
        return t1.equals(t) || t2.equals(t);
    }

    @Override public int hashCode() {
        return t1.hashCode() + t2.hashCode();
    }

    @Override public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if(obj == null)
            return false;
        if(getClass() != obj.getClass())
            return false;
        @SuppressWarnings("unchecked") Set2<T> other = (Set2<T>) obj;
        if(t1.equals(other.t1)) {
            return t2.equals(other.t2);
        } else if(t1.equals(other.t2)) {
            return t2.equals(other.t1);
        }
        return false;
    }

    @Override public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("{ ");
        sb.append(t1);
        sb.append(", ");
        sb.append(t2);
        sb.append(" }");
        return sb.toString();
    }

    public static <T> Set2<T> of(T t1, T t2) {
        return new Set2<>(t1, t2);
    }

}