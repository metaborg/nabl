package mb.statix.scopegraph.diff;

public class Edge<S, L> {

    public final S source;
    public final L label;
    public final S target;

    public Edge(S source, L label, S target) {
        this.source = source;
        this.label = label;
        this.target = target;
    }

    @Override public String toString() {
        return source + " -" + label + "-> " + target;
    }

}