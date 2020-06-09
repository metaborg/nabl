package mb.statix.scopegraph.reference;

import java.io.Serializable;
import java.util.Objects;

import org.metaborg.util.functions.Function1;

public abstract class EdgeOrData<L> implements Serializable {

    private static final long serialVersionUID = 1L;

    abstract public <R> R matchInResolution(OnData<R> onData, OnEdge<L, R> onEdge)
            throws ResolutionException, InterruptedException;

    public abstract <R> R match(Function1<Access, R> onData, Function1<L, R> onEdge);

    public static <L> EdgeOrData<L> data(Access access) {
        return new Data<>(access);
    }

    public static <L> EdgeOrData<L> edge(L l) {
        return new Edge<>(l);
    }

    private static class Data<L> extends EdgeOrData<L> {

        private static final long serialVersionUID = 1L;

        private final Access access;

        private Data(Access access) {
            this.access = access;
        }

        @Override public <R> R matchInResolution(OnData<R> onData, OnEdge<L, R> onEdge)
                throws ResolutionException, InterruptedException {
            return onData.apply(access);
        }

        @Override public <R> R match(Function1<Access, R> onData, Function1<L, R> onEdge) {
            return onData.apply(access);
        }

        @Override public int hashCode() {
            return Objects.hash(access);
        }

        @Override public boolean equals(Object obj) {
            if(obj == null)
                return false;
            if(this == obj)
                return true;
            if(getClass() != obj.getClass())
                return false;
            @SuppressWarnings("unchecked") Data<L> other = (Data<L>) obj;
            return this.access.equals(other.access);
        }

        @Override public String toString() {
            return "<data>" + access;
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

        @Override public <R> R match(Function1<Access, R> onData, Function1<L, R> onEdge) {
            return onEdge.apply(label);
        }

        @Override public int hashCode() {
            return Objects.hash(label);
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

        R apply(Access access) throws ResolutionException, InterruptedException;

    }

    @FunctionalInterface
    public interface OnEdge<L, R> {

        R apply(L l) throws ResolutionException, InterruptedException;

    }

}