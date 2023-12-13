package mb.statix.generator.nodes;

import java.util.Objects;

import jakarta.annotation.Nullable;

import mb.statix.generator.SearchState;


public class SearchNode<O> implements SearchElement {

    private final int id;
    private final O output;
    @Nullable private final SearchNode<?> parent;
    private final String desc;

    public SearchNode(int id, O output, @Nullable SearchNode<?> parent, String desc) {
        this.id = id;
        this.output = output;
        this.parent = parent;
        this.desc = desc;
    }

    public int id() {
        return id;
    }

    public O output() {
        return output;
    }

    public <X extends SearchState> SearchNode<X> withOutput(X output) {
        return new SearchNode<>(id, output, parent, desc);
    }

    @Override public SearchNode<?> parent() {
        return parent;
    }

    @Override public String desc() {
        return desc;
    }

    @Override public String toString() {
        return desc != null ? desc : Objects.toString(this);
    }

}