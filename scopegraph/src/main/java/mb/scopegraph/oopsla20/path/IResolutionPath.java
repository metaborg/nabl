package mb.scopegraph.oopsla20.path;

public interface IResolutionPath<S, L, D> extends IPath<S, L> {

    IScopePath<S, L> getPath();

    D getDatum();

}