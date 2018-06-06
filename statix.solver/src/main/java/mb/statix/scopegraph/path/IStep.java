package mb.statix.scopegraph.path;

public interface IStep<V, L> extends IScopePath<V, L> {

    L getLabel();

}