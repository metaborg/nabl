package mb.statix.scopegraph.path;

public interface IStep<S, L, O> extends IScopePath<S, L, O> {

    L getLabel();

}