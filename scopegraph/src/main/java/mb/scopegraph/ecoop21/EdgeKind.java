package mb.scopegraph.ecoop21;

import java.io.Serializable;
import java.util.Optional;

import org.metaborg.util.functions.Action0;
import org.metaborg.util.functions.Action1;
import org.metaborg.util.functions.Function0;
import org.metaborg.util.functions.Function1;

import mb.scopegraph.oopsla20.reference.EdgeOrData;

/**
 * Implements the union of {@link EdgeOrData} and {@link EdgeOrEps}, and allows conversion between them.
 */
public abstract class EdgeKind<L> implements Serializable {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("rawtypes") private static final EdgeKind EPS = new Eps<>();

    @SuppressWarnings("rawtypes") private static final EdgeKind DATA = new Data<>();

    public abstract <T> T match(Function0<T> onData, Function0<T> onEps, Function1<L, T> onEdge);

    public abstract void accept(Action0 onData, Action0 onEps, Action1<L> onEdge);

    public Optional<EdgeOrData<L>> toEdgeOrData() {
        // @formatter:off
        return this.match(
            () -> Optional.of(EdgeOrData.data()),
            Optional::empty,
            lbl -> Optional.of(EdgeOrData.edge(lbl))
        );
        // @formatter:on
    }

    public Optional<EdgeOrEps<L>> toEdgeOrEps() {
        // @formatter:off
        return this.match(
            Optional::empty,
            () -> Optional.of(EdgeOrEps.eps()),
            lbl -> Optional.of(EdgeOrEps.edge(lbl))
        );
        // @formatter:on
    }

    @SuppressWarnings("unchecked") public static <L> EdgeKind<L> data() {
        return DATA;
    }

    @SuppressWarnings("unchecked") public static <L> EdgeKind<L> eps() {
        return EPS;
    }

    public static <L> EdgeKind<L> from(EdgeOrData<L> edge) {
        return edge.match(EdgeKind::data, EdgeKind::edge);
    }

    public static <L> EdgeKind<L> from(EdgeOrEps<L> edge) {
        return edge.match(EdgeKind::eps, EdgeKind::edge);
    }

    public static <L> EdgeKind<L> edge(L label) {
        return new Edge<>(label);
    }

    private static final class Data<L> extends EdgeKind<L> {

        private static final long serialVersionUID = 1L;

        private Data() {
        }

        @Override public <T> T match(Function0<T> onData, Function0<T> onEps, Function1<L, T> onEdge) {
            return onData.apply();
        }

        @Override public void accept(Action0 onData, Action0 onEps, Action1<L> onEdge) {
            onData.apply();
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

    private static final class Eps<L> extends EdgeKind<L> {

        private static final long serialVersionUID = 1L;

        @Override public <T> T match(Function0<T> onData, Function0<T> onEps, Function1<L, T> onEdge) {
            return onEps.apply();
        }

        @Override public void accept(Action0 onData, Action0 onEps, Action1<L> onEdge) {
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

    private static final class Edge<L> extends EdgeKind<L> {

        private static final long serialVersionUID = 1L;

        private L label;

        public Edge(L label) {
            this.label = label;
        }

        @Override public <T> T match(Function0<T> onData, Function0<T> onEps, Function1<L, T> onEdge) {
            return onEdge.apply(label);
        }

        @Override public void accept(Action0 onData, Action0 onEps, Action1<L> onEdge) {
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
