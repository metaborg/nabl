package mb.statix.scopegraph.reference;

import java.io.Serializable;

import org.metaborg.util.functions.Function0;
import org.metaborg.util.functions.Function1;

public abstract class EdgeOrData<L> implements Serializable {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("rawtypes") private static final EdgeOrData DATA = new Data<>();

    abstract public <R> R matchInResolution(OnData<R> onData, OnEdge<L, R> onEdge)
            throws ResolutionException, InterruptedException;

    public abstract <R> R match(Function0<R> onData, Function1<L, R> onEdge);

    @SuppressWarnings("unchecked") public static <L> EdgeOrData<L> data() {
        return DATA;
    }

    public static <L> EdgeOrData<L> edge(L l) {
        return new Edge<>(l);
    }

    private static class Data<L> extends EdgeOrData<L> {

        private static final long serialVersionUID = 1L;

        private Data() {
        }

        @Override public <R> R matchInResolution(OnData<R> onData, OnEdge<L, R> onEdge)
                throws ResolutionException, InterruptedException {
            return onData.apply();
        }

        @Override public <R> R match(Function0<R> onData, Function1<L, R> onEdge) {
            return onData.apply();
        }

        @Override public int hashCode() {
            return 7;
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
            return "<data>";
        }

    }

    private static class Edge<L> extends EdgeOrData<L> {

        private static final long serialVersionUID = 1L;

        private final L label;

        private Edge(L label) {
            this.label = label;
        }

        @Override public <R> R matchInResolution(OnData<R> onData, OnEdge<L, R> onEdge)
                throws ResolutionException, InterruptedException {
            return onEdge.apply(label);
        }

        @Override public <R> R match(Function0<R> onData, Function1<L, R> onEdge) {
            return onEdge.apply(label);
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

    @FunctionalInterface
    public interface OnData<R> {

        R apply() throws ResolutionException, InterruptedException;

    }

    @FunctionalInterface
    public interface OnEdge<L, R> {

        R apply(L l) throws ResolutionException, InterruptedException;

    }

}