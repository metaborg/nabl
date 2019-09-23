package mb.statix.random;

import java.util.Objects;

public class SearchNode<O> {

    private final int id;
    private final O output;
    private final SearchNode<?> parent;
    private final String desc;

    public SearchNode(int id, O output, SearchNode<?> parent, String desc) {
        this.id = id;
        this.output = output;
        this.parent = parent;
        this.desc = desc;
    }

    public O output() {
        return output;
    }

    public <X> SearchNode<X> withOutput(X output) {
        return new SearchNode<>(id, output, parent, desc);
    }

    public SearchNode<?> parent() {
        return parent;
    }

    public String desc() {
        return desc;
    }

    @Override public String toString() {
        return String.format("[%03d] %s", id, (desc != null ? desc : Objects.toString(this)));
    }

}