package mb.scopegraph.oopsla20.path;

public interface IStep<S, L> extends IScopePath<S, L> {

    L getLabel();

}