package mb.statix.random;

import java.util.Objects;

public class SearchNode<O> {

    private final O output;
    private final SearchNode<?> parent;
    private final String desc;

    public SearchNode(O output, SearchNode<?> parent, String desc) {
        this.output = output;
        this.parent = parent;
        this.desc = desc;
    }

    public O output() {
        return output;
    }

    public SearchNode<?> parent() {
        return parent;
    }

    @Override public String toString() {
        return desc != null ? desc : Objects.toString(this);
    }

}