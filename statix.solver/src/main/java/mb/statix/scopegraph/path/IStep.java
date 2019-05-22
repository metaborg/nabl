package mb.statix.scopegraph.path;

public interface IStep<S, L> extends IScopePath<S, L> {

    L getLabel();

}