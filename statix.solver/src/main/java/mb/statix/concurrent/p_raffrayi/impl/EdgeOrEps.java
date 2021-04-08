package mb.statix.concurrent.p_raffrayi.impl;

import java.io.Serializable;

import org.metaborg.util.functions.Action0;
import org.metaborg.util.functions.Action1;
import org.metaborg.util.functions.Function0;
import org.metaborg.util.functions.Function1;

public abstract class EdgeOrEps<L> implements Serializable {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("rawtypes") private static final EdgeOrEps EPS = new Eps<>();

    public abstract <T> T match(Function0<T> onEps, Function1<L, T> onEdge);

    public abstract void accept(Action0 onEps, Action1<L> onEdge);

    @SuppressWarnings("unchecked") public static <L> EdgeOrEps<L> eps() {
        return EPS;
    }

    public static <L> EdgeOrEps<L> edge(L l) {
        return new Edge<>(l);
    }

    private static final class Eps<L> extends EdgeOrEps<L> {

        private static final long serialVersionUID = 1L;

        @Override public <T> T match(Function0<T> onEps, Function1<L, T> onEdge) {
            return onEps.apply();
        }

        @Override public void accept(Action0 onEps, Action1<L> onEdge) {
            onEps.apply();
        }

        @Override public int hashCode() {
            return 13;
        }

        @Override public boolean equals(Object obj) {
            if(obj == null)
                return false;
            if(this == obj)
                return true;
            if(getClass() != obj.getClass())
                return false;
            return true;
        }

        @Override public String toString() {
            return "<ε>";
        }

    }

    private static class Edge<L> extends EdgeOrEps<L> {

        private static final long serialVersionUID = 1L;

        private L label;

        public Edge(L label) {
            this.label = label;
        }

        @Override public <T> T match(Function0<T> onEps, Function1<L, T> onEdge) {
            return onEdge.apply(label);
        }

        @Override public void accept(Action0 onEps, Action1<L> onEdge) {
            onEdge.apply(label);
        }

        private volatile int hashCode;

        @Override public int hashCode() {
            int result = hashCode;
            if(result == 0) {
                result = label.hashCode();
                hashCode = result;
            }
            return result;
        }

        @Override public boolean equals(Object obj) {
            if(obj == null)
                return false;
            if(this == obj)
                return true;
            if(getClass() != obj.getClass())
                return false;
            @SuppressWarnings("unchecked") Edge<L> other = (Edge<L>) obj;
            return label.equals(other.label);
        }

        @Override public String toString() {
            return "<edge>" + label;
        }
    }

}
